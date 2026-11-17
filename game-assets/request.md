Use your imagegen skill (the built-in image_gen tool, model gpt-image-2) to create 4 game art assets for a small top-down 2D RPG, then copy the final files into the current working directory with EXACTLY these filenames. Generate one image per asset. Do not write any code other than what is needed to copy files or to remove a chroma-key background. Do not create any other files.

Shared art direction for all 4 images: cohesive 16-bit JRPG pixel art (SNES era), crisp pixels, warm friendly palette, soft outlines, medieval fantasy village theme, consistent 3/4 top-down view. Absolutely NO text, letters, numbers, UI, watermarks, or borders in any image.

1) village_map.png  — landscape, about 1536x1024.
A complete top-down village map seen from above (3/4 RPG view), filling the whole image edge to edge. Layout:
- CENTER: a round cobblestone plaza with a small stone fountain.
- TOP CENTER: a big stone castle gatehouse with a closed wooden gate, a wall running left and right from it.
- LEFT (middle-left): a blacksmith forge workshop with a chimney and an anvil outside.
- RIGHT (middle-right): a library building with a blue roof and a book sign shape (no letters).
- BOTTOM RIGHT: a tall clock tower.
- BOTTOM LEFT: a shabby dark wooden shack in a shadowy corner with crates and barrels.
- BOTTOM CENTER: a small post office with a red roof and a mailbox outside.
- Wide dirt paths connect the plaza to every building's front door. Grass everywhere else, a few trees and flowers near the edges only. Leave the paths and plaza clearly open and empty.
- NO people, NO animals, NO characters anywhere on the map.

2) hero_sheet.png — transparent background, about 1024x1024 or wider.
One young adventurer hero (brown hair, green tunic, small backpack), drawn as 8 separate full-body sprites in a tidy 4 columns x 2 rows grid with LARGE empty transparent gaps between every sprite (sprites must not touch or overlap each other).
- Columns, left to right: facing DOWN (toward viewer), facing UP (back to viewer), facing LEFT, facing RIGHT.
- Row 1: walking pose with the left foot forward. Row 2: walking pose with the right foot forward.
- All 8 sprites the same size and scale, same character, feet at the same baseline within each row. No ground shadow, no background.

3) npc_sheet.png — transparent background, wide landscape (about 1536x1024).
6 different villager characters standing in ONE single horizontal row, each facing the viewer (front view), full body, same scale as a JRPG NPC, with LARGE empty transparent gaps between them (they must not touch). Left to right:
  a) a burly blacksmith with a leather apron holding a wax seal stamp,
  b) an elderly librarian woman with round glasses holding a big book,
  c) a castle gate guard in armor holding a spear,
  d) a suspicious rogue in a dark purple hooded cloak with a sly grin,
  e) a cheerful courier with a blue cap and a mail satchel,
  f) an old clock keeper with a white beard holding a pocket watch.
No ground shadows, no background.

4) items_sheet.png — transparent background, landscape.
4 separate item icons in ONE horizontal row with large transparent gaps between them. Left to right: a shiny gold coin, a rolled parchment scroll closed with a red wax seal, an ornate golden key, a small blue-gray padlock. No background.

Transparency: ask the tool for a genuinely transparent background for images 2, 3 and 4. If the result does not have a real alpha channel, regenerate that image on a flat pure green (#00FF00) background and use the imagegen skill's scripts/remove_chroma_key.py to produce a transparent PNG.

When done, the current directory must contain exactly: village_map.png, hero_sheet.png, npc_sheet.png, items_sheet.png. Finish by printing, for each file: its pixel size and whether it has an alpha channel.
