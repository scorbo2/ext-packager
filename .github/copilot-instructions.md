# GitHub Copilot Instructions for ext-packager

## Repository Overview

**ext-packager** is a Java Swing desktop application that helps developers package, sign, and distribute extensions for Java applications built with the swing-extras framework. It generates JSON manifests for extension discovery and updates, signs JAR files with digital signatures, and uploads extension packages to web servers.

- **Language**: Java 17
- **Version**: 1.1 is in production, 1.2 in development
- **Build System**: Apache Maven 3.9.11
- **Project Type**: Desktop GUI application (Swing)
- **Size**: ~4,700 lines of Java code in 23 source files
- **Main Dependencies**: swing-extras 2.5.0, commons-net 3.9.0, commons-io 2.19.0, FlatLaf UI themes
- **Test Framework**: JUnit 5 (Jupiter) with Mockito

## Build and Validation Commands

### Prerequisites
- **Java 17** (OpenJDK or Oracle JDK) - REQUIRED for both compilation and runtime
- **Maven 3.x** - REQUIRED for building

### Build Commands (Execute in Order)

#### 1. Clean Build Artifacts
```bash
mvn clean
```
Always run this before a fresh build to avoid stale artifacts.

#### 2. Compile Source Code
```bash
mvn compile
```
Expected time: 5-10 seconds (first run: 10-15 seconds with dependency downloads).
Compiles 23 Java source files to `target/classes`.

#### 3. Package Application
```bash
mvn package -DskipTests
```
Expected time: 8-12 seconds.
Creates `target/ext-packager-1.2-SNAPSHOT.jar` and copies dependencies to `target/lib/`.

#### 4. Full Build (One Command)
```bash
mvn clean package -DskipTests
```
This is the recommended command for a complete build from scratch.

### Installation Package Generation

If the `make-installer` script is installed at `~/bin/make-installer`, Maven will automatically generate an installer package during the `package` phase. Configuration is in `installer.props`.

## Project Structure

### Root Directory Files
```
.editorconfig         # Code formatting rules (4 spaces, LF line endings, max 120 chars)
.gitignore           # Standard Java/Maven ignores
pom.xml              # Maven project configuration
installer.props      # Configuration for make-installer script
README.md            # User-facing documentation
screenshot.jpg       # Application screenshot
```

### Source Directory Layout
```
src/
├── main/
│   ├── java/ca/corbett/packager/
│   │   ├── Main.java              # Application entry point
│   │   ├── Version.java           # Version constants and config paths
│   │   ├── AppConfig.java         # User preferences and settings
│   │   ├── io/                    # FTP and file system upload utilities
│   │   ├── project/               # Project data model and management
│   │   └── ui/                    # Swing UI components and dialogs
│   └── resources/ca/corbett/extpackager/
│       ├── ReleaseNotes.txt       # Version history
│       ├── logging.properties     # Java logging configuration
│       └── images/                # Application icons and logos
└── test/
    └── java/ca/corbett/packager/project/
        └── ProjectManagerTest.java  # JUnit tests
```

### Build Output
```
target/
├── ext-packager-1.1.jar    # Main application JAR
├── lib/                     # All dependency JARs (copied by maven-dependency-plugin)
├── classes/                 # Compiled .class files
└── surefire-reports/        # Test execution reports
```

## Code Style and Conventions

### EditorConfig Settings (Enforced)
- **Indentation**: 4 spaces (NO TABS)
- **Line endings**: LF (Unix-style)
- **Max line length**: 120 characters
- **Charset**: UTF-8
- **No final newline** at end of files

### Java Code Conventions
- **Brace style**: End-of-line (K&R style)
- **Braces**: Always use braces for `if`, `for`, `while` (enforced by editorconfig)
- **Catch/else/finally**: On new line after closing brace
- **Imports**: Use single-class imports (no wildcard imports unless 999+ classes)
- **Comments**: Minimal inline comments; prefer Javadoc for public APIs
- **Package structure**: `ca.corbett.packager` is the base package

### Important Code Notes
- Main entry point: `ca.corbett.packager.Main.main()`
- The application is designed as a GUI tool; CLI mode is not currently implemented
- TODOs in code (4 occurrences) reference hardcoded filenames like "public.key"
- The UI uses singleton pattern for `MainWindow` and `ProjectManager`

## Dependencies and Versioning

### Runtime Dependencies (from pom.xml)
```xml
swing-extras (2.6.0)        # Custom Swing components and extension framework
commons-net (3.9.0)         # FTP upload functionality
commons-io (2.19.0)         # File I/O utilities
```

### Test Dependencies
```xml
junit-jupiter (5.12.1)      # JUnit 5 testing framework
mockito-core (5.14.2)       # Mocking framework
```

### UI Themes (Transitive Dependencies)
- FlatLaf 3.6 (modern flat UI theme)
- JTattoo 1.6.13 (additional theme options)

## Common Pitfalls and Workarounds

### 1. Test Failures in Headless Environments
**Symptom**: `java.awt.HeadlessException` when running tests
**Solution**: Always use `mvn package -DskipTests` for builds in CI or remote environments

### 2. Application Won't Start from JAR
**Symptom**: Missing dependencies when running `java -jar target/ext-packager-1.1.jar`
**Solution**: Ensure `target/lib/` directory is present with all dependency JARs. The manifest uses `lib/` as classpath prefix.

## Making Code Changes

### Before Making Changes
1. Run `mvn clean` to start fresh
2. Review relevant source files in `src/main/java/ca/corbett/packager/`
3. Check the `.editorconfig` for formatting rules

### After Making Changes
1. Compile: `mvn compile` (to check for compilation errors)
2. Package: `mvn package -DskipTests` (to verify the build succeeds)
3. If modifying tests: Run `mvn test` locally
4. Verify your code follows editorconfig style (4-space indent, 120-char line limit)

### Testing Strategy
- The single test class `ProjectManagerTest` validates project file path resolution
- Tests require mocking or refactoring to run in headless mode
- When adding new features, consider whether they can be unit tested without GUI

## Version and Release Information

- **Current Version**: 1.1 (released 2025-12-01), 1.2 (in development)
- **Version Location**: `src/main/java/ca/corbett/packager/Version.java`
- **Release Notes**: `src/main/resources/ca/corbett/extpackager/ReleaseNotes.txt`
- **Project URL**: https://github.com/scorbo2/ext-packager
- **License**: MIT License

## Additional Context

### No CI/CD Pipeline
This repository does not have GitHub Actions workflows or other CI/CD automation. All builds are manual.

### No Static Analysis
There are no CheckStyle, SpotBugs, PMD, or other linting tools configured. Code quality relies on editorconfig and manual review.

### Application Settings
The application stores user settings in:
- Linux/Mac: `~/.ExtPackager/ExtPackager.prefs`
- Projects default location: `~/.ExtPackager/projects/`

## Trust These Instructions

These instructions are comprehensive and validated by testing actual builds and exploring the codebase. Follow them precisely to minimize trial-and-error. If you encounter issues not covered here, the problem is likely environmental (Java version, missing dependencies) or requires updating these instructions.
