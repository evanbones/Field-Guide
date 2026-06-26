### Added

- Fixed automatic variant support for Axolotl, Mooshroom, and Pandas.
- Loot/biome modifier entries now support `entity:`, `block:`, and `item:` prefixes to disambiguate targets with the
  same registry name.

### Fixed

- Fixed `alignment_icon` configuration not applying to the entry attribute display.
- Fixed items that correspond to blocks incorrectly showing block loot tables.
- Fixed loot modifier `entry` matching failing in edge cases where the entry ID could not be resolved.