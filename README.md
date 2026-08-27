# Ender Bags Continue

A collection of giant sacks. This mod is forked from Ignition: EnderBags,
which stopped updating after Minecraft 1.18.2.

This project targets Minecraft 1.21.1 with NeoForge.

## Features

- A portable Ender Bag with 104 storage slots.
- Open the bag by holding it in the main hand and right-clicking.
- Craft a bag from seven matching wool blocks to obtain the corresponding color; mixed
  wool colors do not form a valid recipe.
- Re-dye bags into any of Minecraft's 16 dye colors without losing their contents.
- Empty bags behave like normal items and may be destroyed.
- Filled bags do not despawn and cannot be destroyed by ordinary damage. They are still
  destroyed after falling into the void.
- Ender Bags may be stored in ordinary containers, but an Ender Bag cannot contain another
  Ender Bag.
- Ender Bags always have a maximum stack size of one.
- Bag contents are authoritative on the server and committed to the held item after every
  slot mutation. Invalid or conflicting legacy data is preserved instead of silently cleaned.
- Existing bag contents and colors from the Minecraft 1.20.1 version are migrated
  to Minecraft 1.21 data components when the bag is first used.

## Development

Run `gradlew.bat build` on Windows or `./gradlew build` on Linux and macOS.

ClientSort and Cloth Config are downloaded as optional development dependencies and loaded
by local run configurations. They are not required by the published Ender Bags mod.

## License

Ender Bags Continue is licensed under the MIT License. See [LICENSE](LICENSE).
