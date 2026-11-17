# game-assets

`game-token-village.html`(토큰 마을)이 쓰는 그림이다.

| 파일 | 내용 |
|---|---|
| `village_map.webp` | 마을의 지도. 1536 x 1024 |
| `hero_sheet.webp` | 주인공. 4방향 x 걷는 자세 2개 |
| `npc_sheet.webp` | 주민 6명 |
| `items_sheet.webp` | 금화, 두루마리, 열쇠, 자물쇠 |
| `request.md` | Codex에게 보낸 요청의 글 (영어) |

- 그림은 Codex CLI가 만들었다. Codex의 이미지 생성 스킬(`imagegen`, 모델 gpt-image-2)을 썼다.
- 투명한 배경은 초록 배경으로 만든 뒤에 Codex가 지웠다.
- 원래의 PNG를 WebP로 줄였다 (`cwebp`). 지도는 품질 84이고, 나머지는 무손실이다.
- 게임의 HTML은 이 그림들을 파일의 안에 넣고 있다. 게임을 여는 데 이 폴더가 필요하지 않다. 이 폴더는 그림을 다시 쓰거나 게임을 다시 조립할 때를 위한 것이다.
