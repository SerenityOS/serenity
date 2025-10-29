# Helix Configuration

Helix comes with support for `clangd` and `clang-format` out of the box! Refer to [ClangdConfiguration](ClangdConfiguration.md) for how to configure clangd.

To configure clangd command-line arguments, create a `.helix/languages.toml` file in the project root:

```toml
[language-server.serenity]
command = "clangd"
# clangd arguments, refer to ClangdConfiguration.md for what may be needed.
args = []

[[language]]
name = "cpp"
language-servers = ["serenity"]
```
