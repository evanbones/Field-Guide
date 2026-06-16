# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.8.3] - 2026-06-15

### Fixed

- Fixed issues with item/entity model resolving.
- Item entries no longer show any drops by default.

## [1.8.2] - 2026-06-07

### Fixed

- Fixed Axolotl variants.

## [1.8.1] - 2026-06-07

### Fixed

- Fixed Mooshroom variants.

## [1.8.0] - 2026-06-06

### Fixed

- Fixed `alignment_icon` configuration not working.
- Added an `autopopulate` strategy for paintings.
- Added Panda variants.
- Fixed Fox and Parrot variants in 1.20.
- Backport various fixes from 1.21.

## [1.7.5] - 2026-05-05

### Fixed

- Simplified scan verification to fix issues with servers rejecting scans.

## [1.7.3] - 2026-05-02

### Fixed

- Fixed issues with mob loot on dedicated servers.

## [1.7.2] - 2026-04-29

### Fixed

- Hopefully fix startup crash with certain mods.

## [1.7.1] - 2026-04-29

### Fixed

- Fixed item blacklist tag.

## [1.7.0] - 2026-04-29

### Added

- Added additional variants for mobs from Environmental, Neapolitan, Quark, and Caverns and Chasms (@mc_polaris)
- Added support for biome tags and item tags in biome and loot modifications respectively.
- Composites now support a `"replace": true` field for overwriting.
- Added a config option to disable variants as a whole.

### Fixed

- Fixed variant issues with Goety.
- Fixed Field Guide biomes ignoring Immersive Overlays text files.

## [1.6.9] - 2026-04-09

### Fixed

- Fixed Curios scanning on dedicated servers.

## [1.6.8] - 2026-04-02

### Changed

- Added additional API capabilities for eventual Spawn variant integration.

## [1.6.7] - 2026-04-02

### Fixed

- Hotfix for invisible Biomes o' Plenty trees on 1.20.

## [1.6.6] - 2026-04-02

### Added

- Added config option to disable the red out of range overlay.

## [1.6.5] - 2026-04-01

### Added

- Added support for modifying the locked entry descriptions (see the Resource Packs page in the docs for information on
  this).

### Changed

- Improved the default locked entry descriptions to be smarter.

### Fixed

- Fixed alternate unlock strategies (eat, obtain) causing entities to still be scannable.
- Fixed searching by mod id not working.
- Fixed some exported items and entities sharing a description.

## [1.6.4] - 2026-03-31

### Changed

- Improved Item Descriptions integration to automatically use tag descriptions and polymer item descriptions, if
  applicable.

### Fixed

- Fixed "obtain" triggers not applying to food.
- Fixed Tide lava fish rendering sideways.

## [1.6.3] - 2026-03-31

### Added

- Added config option to restrict opening the Field Guide to players with the item in their inventory.

### Fixed

- Fixed scans rejecting when spyglasses are in the Curios slot.
- Fixed Scholar integration causing scrollbars to not show up in long descriptions.
- Fixed max Journal page length being one line too short.

## [1.6.2] - 2026-03-30

### Fixed

- Fixed duplicate rabbit variants.
- Fixed Atmospheric and Environmental variants.

## [1.6.1] - 2026-03-30

### Added

- Added `"hidden"` field for category definitions (primarily used to hide the previously unhidable `info` category).

### Fixed

- Fixed format of `/fieldguide export` to match expected format for lang entries.

## [1.6.0] - 2026-03-29

### Added

- Added integration with Scholar for text editing.
- Added a Journal tab for keeping general notes on your discoveries.
- Added better Biomes o' Plenty integration.

### Changed

- Changed default description of Chorus Plant.
- When exporting data, you'll now be able to click on the link to open the folder in your system's file browser.
- Adjusted default renders for Dark Oak Trees and Chorus Plants.

### Fixed

- Fixed killing any entities causing them to be unlocked.

## [1.5.15] - 2026-03-29

### Added

- Added a config option to disable the scanning ding sound.

### Fixed

- Fixed export format for entries with variants.

## [1.5.14] - 2026-03-29

### Changed

- Redirects are now gracefully ignored if the target mod is missing.

### Fixed

- Fixed redirects not working properly.
- Fixed `/fieldguide export` for JSONs not working properly on Fabric 1.21.

## [1.5.13] - 2026-03-29

### Added

- Added support for The Dawn Era (Legacy).

## [1.5.12] - 2026-03-28

### Added

- Added default configurations and variant support for No Man's Land.

### Fixed

- Fixed No Man's Land Deer rendering.
- Fixed overlapping tooltip on the Seasons icons.

## [1.5.11] - 2026-03-28

### Added

- Added variant support for additional vanilla variants, Vanilla Backport, Atmospheric, and Environmental (@proxillus).

### Fixed

- Fixed variants cycling while the selector is open, sometimes leading to a crash.
- Fixed biome removals for variants not working.

## [1.5.10] - 2026-03-27

### Changed

- Variants now support unique descriptions, loot, and biomes.
    - See the wiki for more information on this.

### Fixed

- Fixed variant attributes not updating when cycling variants.

## [1.5.9] - 2026-03-27

### Added

- Added support for Alex's Mobs variants.

### Changed

- Tweaked default configuration for Alex's Mobs.
- Reduced volume of discovery sound.

## [1.5.8] - 2026-03-27

### Fixed

- Fixed Cobblemon-related server crash.

## [1.5.7] - 2026-03-27

### Fixed

- Fixed issues with some modded bosses not being unlockable.

## [1.5.6] - 2026-03-27

### Added

- Added Ecliptic Seasons and Fabric Seasons integration.

### Changed

- Variants in the overview will now show their selected Exposure photograph, if they have one.

### Fixed

- Fixed entity hitboxes (F3+B) rendering in the Field Guide.

## [1.5.5] - 2026-03-26

### Fixed

- Fixed issues with Immediately Fast on 1.21.1.

## [1.5.4] - 2026-03-26

### Changed

- Changed description of the "Aha!" description to not specifically mention spyglasses.
- Renamed "content" tab in Cloth Config screen to "commands" for clarity.

### Fixed

- Removed trees from block autopopulates.
- Fixed invisible Environmental trees on 1.20.1 this time.

## [1.5.3] - 2026-03-26

### Fixed

- Fixed Environmental NBT (@Proxillus).
- Fixed recipe error in logs.
- Removed unused config option on 1.21.1.

## [1.5.2] - 2026-03-25

### Fixed

- Hotfix for Chicken, Cod, and Salmon not being scannable.

## [1.5.1] - 2026-03-25

### Fixed

- Fixed Field Guide recipe not working on 1.21.

## [1.5.0] - 2026-03-25

### Added

- Added Pokémon category for when Cobblemon is installed.
- Added support for variants.
- Added ore icon to the default icon pack.
- Added KubeJS integration.
    - Documentation is available on the Field Guide wiki.
- Added an option to enable a physical Field Guide item.
- Pages can now be copied from the Field Guide to give to your friends in Multiplayer, or stored.
    - Pages contain all the information about an entry, including custom names, descriptions, Exposure photographs, and
      unlocked variants.
- Added a display for seasons integration.
    - Only Serene Seasons is supported for now, integration with other seasons mods is planned for the near future.
- Added new Field Guide advancement tab and related advancements.
- Added increased functionality for entry unlocking criteria.
    - This includes killing, obtaining, eating, scanning, and prerequisite entries.
- Added support for virtual entries.
    - These are used to define entries without any related item, block, or entity, like for tutorial entries as an
      example.
- Added descriptions for trees (@proxillus).
- Added Environmental compatibility (@proxillus).
- Added Blooming Nature compatibility (@thoughtRock05).
- Added fallback icon for when a biome is missing a texture in Immersive Overlays.

### Changed

- Improved icon cache generation speeds.
- Outdated Exposure versions will now intentionally trigger a crash. Please update Exposure!
- Improved server join speeds.
- Moved vanilla spyglass-related advancements to the Field Guide advancement tab.
- Pressing B when hovering over an item will now open its entry in the Field Guide (if it exists).
- Pressing E while in the Field Guide will now return you to the inventory screen.
- Improved loot/biome querying speeds.

### Fixed

- Fixed certain commands causing client crashes.
- Fixed selected Exposure photographs for entries not persisting.
- Fixed issue with Iron's Spells and Spellbooks spell forging.
- Fixed issues with Fresh Animations and other EMF resource packs.

## [1.4.1] - 2026-03-14

### Changed

- Temporarily disabled LootJS integration due to lag.
    - To recreate this functionality, just create loot removal/addition datapacks for Field Guide (or better, create
      vanilla loot table datapacks instead of using LootJS).

### Fixed

- Fixed lag when joining servers and running /reload.

## [1.4.0] - 2026-03-13

### Added

- Added many more modded bosses to the default bosses category.
- Added French localization (@TheCreateMaster).
- Added many more default blacklist entries.
- Added new config options for naked eye scanning and scan distance.
- Added optional keybind to initiate scanning when bound.

### Changed

- Switched to a tag-based blacklist, which now supports blacklisting on a per-category basis.

### Fixed

- Fixed Autumnity large pumpkin render.

## [1.3.0] - 2026-03-12

### Changed

- Servers now enforce progress and validate discoveries for improved security (@pau101).

### Fixed

- Fixed Exposure discoveries not giving rewards (@pau101).
- Fixed crash on dedicated servers when taking photos with Exposure.
- Fixed LootJS changes not being reflected in the Field Guide.
- Fixed datapack loot modifications not being reflected in the Field Guide.
- Fixed `x_offset` not doing anything in resource packs.
- Fixed Autumnity red and yellow maple trees having swapped names.
- Fixed fish rendering sideways in the Field Guide (Thanks, Ninni!)

## [1.2.1] - 2026-03-08

### Fixed

- Fixed the 1.20.1 versions being slightly out of date.

## [1.2.0] - 2026-03-08

### Added

- Added Exposure integration.
    - Entities/blocks in photos will automatically be unlocked in the Field Guide (configurable).
    - Photographs can be added to any entry displays in place of their regular renders.
- Added Reliable Remover integration.
    - Blacklisted items will now be removed from entries and loot displays.
- Added default configurations for the following mods' plants:
    - Farmer's Delight
    - Atmospheric
    - Autumnity
    - Supplementaries
    - Windswept
- Added config options for changing silhouette colors.
- Added support for biome tags in biome modifiers.
- Added Simplified and Traditional Chinese localizations (@balitube).

### Changed

- Increased widget render size.
- Removed default fish texture overrides.
- The display for days in the Field Guide now starts from Day 1.
- Moved more text to translation keys.

### Fixed

- Fixed mob rendering with Mixed Litter (No Man's Land).
- Fixed spawn biomes not showing on dedicated servers.
- Fixed crash with Shades, Monster Booklet, and possibly other mods.
- Fixed issue where pressing B would sometimes erroneously open the guide.
- Improved render scaling.

## [1.1.3] - 2026-03-02

### Fixed

- Fixed crash with Lithostitched.

## [1.1.2] - 2026-03-01

### Fixed

- Improve networking performance for 1.21.1.
- Fixed crashes with scanning multipart entities.

## [1.1.1] - 2026-03-01

### Fixed

- Fixed bosses tag not populating properly.
- Fixed possible networking-related NPE.

## [1.1.0] - 2026-03-01

### Added

- Added tag for Spyglass items.
- `tag` strategies now support tags for blocks.

### Fixed

- Fixed issues with category syncing with Lithium.
- Fixed resource pack reloading failing in-game.

## [1.0.3] - 2026-03-01

### Fixed

- Fixed huge mushrooms not showing up in the Field Guide.

## [1.0.2] - 2026-03-01

### Fixed

- Fixed category syncing in heavily modded environments.

## [1.0.1] - 2026-03-01

### Fixed

- Fixed too large payloads in heavily modded environments.

## [1.0.0] - 2026-03-01

- Initial release.