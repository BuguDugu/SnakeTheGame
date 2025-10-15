# SnakeTheGame 🐍🎮

A colorful, fast, and simple Snake game built with Java + LibGDX (LWJGL3). Pick a level, eat apples, grow longer, and beat your high score! 🏆

Highlights ✨
- 🧭 Main menu with Play, High Scores, Exit
- 🚀 Level select with 5 speeds (from chill to insane)
- 🏅 In-memory Top 10 high scores
- 🌿 Fresh menu visuals: styled buttons and a meadow background with a cute cartoon snake

Controls ⌨️
- Menus: UP/DOWN to navigate, ENTER to select, ESC to go back
- In game: Arrow keys or WASD to move
- Game Over: ENTER — back to Menu (score saved), R — restart same level

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

Troubleshooting 🧰
- Ensure internet access for Maven dependencies and Launch4j plugin (com.akathist:launch4j-maven-plugin)
- If the EXE step fails in IDE, try Reimport Maven or run from terminal
- The EXE uses the shaded JAR built in the same package phase

Project layout 🗂️
- snake-the-game/ (parent POM)
  - core/ — game logic (LibGDX core)
  - launcher/ — desktop launcher (LWJGL3)

Requirements 📦
- JDK 17+ (the project targets 25, but 17/21 work fine to run)
- Maven with access to Maven Central

Notes 📝
- Rendering is asset-free; visuals are generated programmatically via ShapeRenderer and Scene2D.
- Want to persist high scores? For now they are in-memory; a DB/file store can be added later.