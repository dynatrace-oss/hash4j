# Claude Code Instructions for hash4j

The instructions for AI coding agents working on this repository are
maintained in a single source of truth: [`AGENTS.md`](./AGENTS.md).

Please read and follow `AGENTS.md` for:

- Project overview and tech stack
- Project structure and key packages
- Common Gradle commands (build, test, format, benchmarks, API check, javadoc)
- Coding guidelines (Spotless, `-Werror`, ErrorProne, 100% JaCoCo coverage,
  license headers, reference-implementation cross-checks, git submodules)

Keeping a single file avoids drift between agent-specific instruction files.
If you need to update guidance for Claude Code, update `AGENTS.md` so all
agents (Claude Code, GitHub Copilot, Cursor, etc.) stay in sync.

