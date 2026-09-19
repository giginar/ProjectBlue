# Repository language

- Use English for all repository content: file and directory names, documentation,
  code identifiers, comments, UI text, script messages, and generated package instructions.
- Write commit messages and pull request titles/descriptions in English.
- Keep documentation and script references in sync when renaming files.
- Conversation with the user may use the user's preferred language.

# Project Blue Development Rules

- The project uses Java and libGDX.
- Preserve the existing module and build structure.
- Do not introduce Kotlin unless explicitly requested.
- Do not replace working architecture without explaining the reason.
- Do not copy assets, code, UI, characters, levels, audio, or distinctive designs from Sky Force or another game.
- Use only original placeholders or assets with verified commercial-use licenses.
- Record every asset in ASSET_LICENSES.md.
- Keep Android-specific dependencies outside the core module.
- Use the existing data-driven level system.
- Do not create a separate GameScreen for every level.
- Preserve backward compatibility of saved profiles.
- Add tests for new pure game logic.
- Run relevant tests after every implementation.
- Verify the desktop build.
- Verify the Android debug build when the Android SDK is available.
- Do not add real AdMob identifiers, signing passwords, keystores, or secrets to Git.
- Do not make unrelated refactors.
- Do not commit changes unless explicitly requested.
- Before editing, inspect the existing implementation and summarize the intended changes.
- After editing, report changed files, tests, build results, and remaining limitations.
