# Git Commits
- There is NO dedicated `git_commit` tool. Do not search for one.
- When asked to commit changes, immediately call `run_shell_command` with:
  `git add -A && git commit -m "<type>: <concise summary>"`
- Use Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`).
- Do not run `git status` or `git diff` beforehand unless the commit fails.

# Android Project Rules
- Language: Kotlin (use Coroutines/Flow for async work).
- UI: Jetpack Compose (follow Material 3 guidelines).
- Keep responses concise and modify only the files directly related to the request.


