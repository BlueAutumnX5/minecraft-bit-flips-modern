# Minecraft Radiation Bit-Flip Mod

[![Download Mod](https://img.shields.io/badge/Download-Mod%20.jar-DBB13B?style=for-the-badge&logo=java&logoColor=black)](https://github.com/BlueAutumnX5/minecraft-bit-flips-modern/raw/main/radiation-bit-flip-3.0.0+1.21.11.jar)

[Interactive Table of Minecraft](https://atomicfrontiercode.github.io/minecraft/)

# === minecraft bit flipper === 
- Implements single event upsets for blocks near player. 
- Press K in game to select mode (off / full / solids only) and rate (low / medium / high / replay). The replay mode is roughly based on the actual bit flip rate during the run.
- For more information check out [https://www.youtube.com/watch?v=Kd5-1IXqiDQ](https://www.youtube.com/watch?v=Kd5-1IXqiDQ)
- Use the interactive site [https://atomicfrontiercode.github.io/minecraft/](https://atomicfrontiercode.github.io/minecraft/) to plan bit flips.
- Runs on Minecraft 1.21.11 with Fabric. The original 1.12.2 Forge build is still in radiation-bit-flip-2.0.0.jar.
- Block ids are the 1.12 ones (via the game's own flattening table), so the interactive table still applies. Blocks added after 1.12 (deepslate, copper, etc.) have no id and are never flipped.

# === more details === 
- The measured flip rates in the actual experiment were:
  - Low: 2.36689 × 10⁻⁶ flips/bit/s (warm up, takes about 10s)
  - Medium: 2.24665 × 10⁻⁴ flips/bit/s upsets per bit per second (steady state, dafault for the mod)
  - High: 1.67743 × 10⁻² flips/bit/s (chip failing, takes about 90s to get here)
  - Radiation damadge is entirely random both in type (i.e. 0->1 is as likely as 1->0) and position (no bits within a byte, or bytes within the chip are any more or less likely to flip). I was kinda hoping it would be more interesting and I'd be able to publish a paper on it, alas no.

- Flips one of the eight ID bits of nearby blocks. Metadata (e.g., block orientation or variant) is retained. Invalid target IDs or incompatible metadata leave the original block unchanged.
- Each player affects a 4 × 4 × 4 group of 16 × 16 × 16 blocks (i.e., 64 sub-chunks), limited to the world height. 
- The full mode can turn air into blocks (which is really annoying); solids-only mode attempts changes only on blocks whose material is solid.
- In theory this works for everyone on your server, but as I have no friends I've been unable to verify.

# === installation ===
1. You'll need Minecraft Java Edition and the Fabric installer (https://fabricmc.net/use/installer/).
2. Run the Fabric installer, pick Minecraft 1.21.11 and install the client (the default).
3. Download Fabric API for 1.21.11 (https://modrinth.com/mod/fabric-api) and put it in the mods folder.
4. Put radiation-bit-flip-3.0.0+1.21.11.jar in the mods folder too.
5. Open the launcher, select the "fabric-loader-1.21.11" installation and play. Press "K" to turn on and select the mode.
- The mods folder is in .minecraft (on Windows: %appdata%\.minecraft\mods).
- For a server, install the same jar and Fabric API on the server. Players also need the mod to open the menu.
- With Mod Menu installed, the menu can also be opened from the mod list.

# == the source code ==
- Located in RadiationBitFlipMod folder
- Written in java, should be easy enough to modify if you want to.
- Use Java 21. On Windows, run gradlew.bat build. On macOS or Linux, run ./gradlew build. The distributable JAR appears in build/libs/radiation-bit-flip-3.0.0+1.21.11.jar. The gradle/wrapper files are included so a separate Gradle installation is not required.
