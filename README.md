# AnarchySpawn Plugin
**AnarchySpawn** is a Minecraft Folia 1.21.8 plugin that handles random spawning of players. When players first join or respawn without a bed/anchor, they will be randomly teleported to a safe location within a configurable radius from world coordinates 0,0.

## Features
- Random spawn locations within configurable radius.
- Safe spawn location detection.
- First-join random spawn.
- Random respawn when no bed or respawn anchor is set.
- Dimension restrictions: `/spawn` command cannot be used in the Nether or End.
- Configurable cooldown for the `/spawn` command to prevent abuse.

## Installation
1. Download the latest release [here](https://github.com/DRATHARR/anarchyspawn-folia/releases/tag/Folia).
2. Place the `.jar` file in your Minecraft server's `plugins` folder.
3. Restart your server.

## Configuration
In `config.yml`, you can configure:
```yaml
# Maximum distance from 0,0 that players can spawn (in blocks)
spawn-radius: 300

# Maximum number of attempts to find a safe spawn location
max-spawn-attempts: 75

# Cooldown in seconds for using the /spawn command
spawn-cooldown: 5

# List of blocks that are considered unsafe to spawn on or next to
unsafe-blocks:
  - LAVA
  - WATER
  - CACTUS
  - FIRE
  - MAGMA_BLOCK
  - SWEET_BERRY_BUSH
  - POWDER_SNOW
  - PALE_OAK_LEAVES
  - BIRCH_LEAVES
  - OAK_LEAVES
  - DARK_OAK_LEAVES
  - STONE
  - COPPER_ORE
  - WAXED_OXIDIZED_COPPER
  - SPRUCE_LEAVES
  - CLAY
  - OAK_PLANKS
  - DEEPSLATE
  - RED_MUSHROOM_BLOCK
  - GRAVEL
  - DIORITE
  - GRANITE
  - MOSS_BLOCK
  - OWN_MUSHROOM_BLOCK
  - TUFF
  - AMETHYST_BLOCK
  - CALCITE
  - SMOOTH_BASALT
  - SCULK
  - GRAY_WOOL
  - DEEPSLATE_BRICKS
  - POLISHED_DEEPSLATE
  - WAXED_COPPER_GRATE
  - MOSSY_COBBLESTONE
  - COBBLESTONE
  - SCULK_SENSOR
  - ANDESITE
  - DRIPSTONE_BLOCK
  - WAXED_COPPER_BLOCK
  - POINTED_DRIPSTONE
  - SCULK_VEIN
  - COAL_ORE
  - BROWN_MUSHROOM_BLOCK
  - DIRT
  - TUFF_BRICKS
  - DEEPSLATE_REDSTONE_ORE
  - CRACKED_DEEPSLATE_BRICKS
  - DARK_OAK_PLANKS
  - CHISELED_DEEPSLATE
  - DEEPSLATE_IRON_ORE
  - DEEPSLATE_TILE_STAIRS
  - REDSTONE_ORE
  - DEEPSLATE_LAPIS_ORE
  - WAXED_OXIDIZED_CUT_COPPER
  - DEEPSLATE_TILES
  - NOTE_BLOCK
  - CRACKED_DEEPSLATE_TILES
  - WHITE_WOOL
  - BIRCH_PLANKS
  - IRON_ORE
  - COBBLED_DEEPSLATE
  - DEEPSLATE_DIAMOND_ORE
  - OAK_PLANKS
  - OAK_STAIRS
  - BIRCH_STAIRS
  - DEEPSLATE_COPPER_ORE
  - DEEPSLATE_GOLD_ORE
  - LAPIS_ORE
  - GOLD_ORE
  - REINFORCED_DEEPSLATE
  - WAXED_OXIDIZED_COPPER_GRATE
  - DEEPSLATE_BRICKS_SLAB
 ```

 ## Commands
- `/spawn`: Teleports the player to a random safe location (requires `anarchyspawn.spawn` permission) - Has a configurable cooldown
- `/anarchyspawn reload`: Reloads the plugin configuration (requires `anarchyspawn.reload` permission)

## Permissions
- `anarchyspawn.spawn`: Allows use of the spawn command (default: true)
- `anarchyspawn.reload`: Allows reloading the plugin configuration (default: op)

## Support first autor
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/K3K715TC1R)
