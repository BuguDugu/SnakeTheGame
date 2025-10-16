# SnakeTheGame 🐍🎮

A colorful, fast, and simple Snake game built with Java + LibGDX (LWJGL3). Pick a level, eat apples, grow longer, and beat your high score! 🏆

Highlights ✨
- 🧭 Main menu with Play, Settings, High Scores, Exit
- 🚀 Level select with 5 speeds (from chill to insane)
- 🏅 In-memory Top 10 high scores
- 🌿 Fresh menu visuals: styled buttons and a meadow background with a cute cartoon snake

Controls ⌨️
- In game: Arrow keys or WASD to move, SPACE to speed up (only on Levels 1–2)
- Menus: Enter/Click to activate, ESC to go back (or exit from main menu)

Quick start ⚡
1) Build all modules:
   mvn -f snake-the-game/pom.xml -q package
2) Run the desktop launcher (dev):
   mvn -f snake-the-game/launcher/pom.xml exec:java

Fat JAR 🧪
- Build a runnable fat-jar for desktop:
  mvn -f snake-the-game/launcher/pom.xml -q package
  Result: snake-the-game/launcher/target/snake-the-game-launcher-1.0-SNAPSHOT.jar
  Run it with:
  java -jar snake-the-game/launcher/target/snake-the-game-launcher-1.0-SNAPSHOT.jar

Windows EXE 🪟
- The launcher module wraps the shaded JAR into an EXE using Launch4j:
  mvn -f snake-the-game/launcher/pom.xml -q package
  Output: snake-the-game/launcher/target/SnakeTheGame.exe

Settings & persistence ⚙️
- Video settings (resolution and fullscreen) can be adjusted in the Settings screen.
- Your last applied choice is saved in a local preferences file and used on the next launch.
- High scores are kept in memory only (cleared on restart). Persistence can be added later if needed.

Troubleshooting 🧰
- Ensure internet access for Maven dependencies and the Launch4j plugin (com.akathist:launch4j-maven-plugin).
- If the EXE step fails in the IDE, try Reimport Maven or run from a terminal.
- The EXE uses the shaded JAR built in the same package phase.

Project layout 🗂️
- snake-the-game/ (parent POM)
  - core/ — game logic (LibGDX core)
  - launcher/ — desktop launcher (LWJGL3)

Requirements 📦
- JDK 17+ (project builds and runs fine on 17/21; newer LTS OK)
- Maven with access to Maven Central

Notes 📝
- Rendering is largely asset-free; visuals are generated programmatically via ShapeRenderer and Scene2D.
- High scores are in-memory only for now.

License 📄
- See LICENSE file in the repository root.