#!/usr/bin/env python3
"""커밋하지 않은 변경을 지우는 git 명령을 막는다. (CLAUDE.md의 "리뷰에서 배운 것", 작업)

Claude Code의 PreToolUse hook이다. Bash 도구의 명령을 stdin의 JSON으로 받는다.
작업 트리에 커밋하지 않은 변경이 있을 때만 막는다. 깨끗하면 아무것도 하지 않는다.

막는 명령:
  git checkout -- <path>, git checkout <있는 경로>, git checkout -f
  git restore (--staged만 있는 것은 뺀다)
  git stash (list, show, pop, apply, drop, branch는 뺀다)
  git reset --hard, git switch -f / --discard-changes
  git clean -f (추적하지 않는 파일이 있을 때)

한계: 명령을 글자로 읽는다. 변수, 별칭, 스크립트의 안의 git은 보지 못한다.
"""
import json
import os
import shlex
import subprocess
import sys

ADVICE = ("커밋하지 않은 변경이 있다. 이 명령은 그 변경을 지운다. "
          "파일을 되돌리려면 먼저 복사해 두고 복사한 것으로 되돌린다 (cp). "
          "변경을 남기려면 먼저 커밋한다.")


def git(args, cwd):
    return subprocess.run(["git"] + args, cwd=cwd, capture_output=True, text=True).stdout


def segments(command):
    """명령을 &&, ||, ;, |, 줄바꿈으로 나눈다. 따옴표의 안은 나누지 않는다."""
    try:
        lexer = shlex.shlex(command, posix=True, punctuation_chars=";&|\n")
        lexer.whitespace = " \t"
        lexer.whitespace_split = True
        tokens = list(lexer)
    except ValueError:
        tokens = command.split()
    part = []
    for token in tokens:
        if token and set(token) <= set(";&|\n"):
            if part:
                yield part
            part = []
        else:
            part.append(token)
    if part:
        yield part


def git_args(part):
    """git의 하위 명령과 그 인자. git이 아니면 None. `git -C dir`, `-c k=v`를 건너뛴다."""
    while part and "=" in part[0] and not part[0].startswith("-"):
        part = part[1:]  # VAR=value git ...
    if not part or os.path.basename(part[0]) != "git":
        return None
    rest = part[1:]
    while rest and rest[0].startswith("-"):
        rest = rest[2:] if rest[0] in ("-C", "-c", "--git-dir", "--work-tree") else rest[1:]
    return rest or None


def discards(args, cwd, untracked):
    sub, rest = args[0], args[1:]
    flags = [a for a in rest if a.startswith("-")]
    if sub == "checkout":
        if "--" in rest or "-f" in flags or "--force" in flags or "." in rest:
            return True
        return any(os.path.exists(os.path.join(cwd, a)) for a in rest if not a.startswith("-"))
    if sub == "restore":
        return not ("--staged" in flags and "--worktree" not in flags and "-W" not in flags)
    if sub == "stash":
        return not rest or rest[0] not in ("list", "show", "pop", "apply", "drop", "branch")
    if sub == "reset":
        return "--hard" in flags
    if sub == "switch":
        return "-f" in flags or "--force" in flags or "--discard-changes" in flags
    if sub == "clean":
        return untracked and any(f.startswith("-") and "f" in f and not f.startswith("--") or f == "--force"
                                 for f in flags) and "-n" not in flags and "--dry-run" not in flags
    return False


def main():
    try:
        payload = json.load(sys.stdin)
    except ValueError:
        return
    command = (payload.get("tool_input") or {}).get("command") or ""
    if "git" not in command:
        return
    cwd = payload.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
    status = git(["status", "--porcelain"], cwd).splitlines()
    tracked = [line for line in status if not line.startswith("??")]
    untracked = [line for line in status if line.startswith("??")]
    if not tracked and not untracked:
        return
    for part in segments(command):
        args = git_args(part)
        if not args:
            continue
        if args[0] != "clean" and not tracked:
            continue
        if discards(args, cwd, bool(untracked)):
            print(json.dumps({"hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": "막았다: `git %s`. %s (변경된 파일 %d개)"
                                            % (" ".join(args), ADVICE, len(tracked) + len(untracked)),
            }}, ensure_ascii=False))
            return


if __name__ == "__main__":
    main()
