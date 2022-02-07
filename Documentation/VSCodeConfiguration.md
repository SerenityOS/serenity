Visual Studio Code Project Configuration
===

Visual Studio Code does not work optimally for Serenity development, though we can convince it to be more understanding of our needs. This document attempts to outline a minimal configuration that is satisfying enough that you don't feel compelled to use another editor, unless you want to. `:^)`


Prerequisites
---
The path of least resistance is to build using Serenity's clang-toolchain which provides `clang-tidy`, followed by manually building `clangd`:

```sh
./Meta/serenity.sh rebuild-world i686 Clang &&
cmake Toolchain/Build/clang/llvm -DCLANG_ENABLE_CLANGD=ON &&
ninja -C Toolchain/Build/clang/llvm &&
ninja -C Toolchain/Build/clang/llvm install
```

If you would prefer to use the GCC toolchain, use the following `.clangd` configuration and change all future references to `i686clang` with `i686`, or the TARGET of your choosing:
```yaml
---
CompileFlags:
   Add:
      [
         -D__serenity__,
         -isystem../../Toolchain/Local/i686/i686-pc-serenity/include/c++/11.2.0,
         -isystem../../Toolchain/Local/i686/i686-pc-serenity/include/c++/11.2.0/bits,
         -isystem../../Toolchain/Local/i686/i686-pc-serenity/include/c++/11.2.0/i686-pc-serenity,
      ]
   CompilationDatabase: Build/i686
```


Extensions
---
Required extensions:
* llvm-vs-code-extensions.vscode-clangd
* notskm.clang-tidy

Recommended extensions:
* kleinesfilmroellchen.serenity-dsl-syntaxhighlight ([marketplace.visualstudio.com](https://marketplace.visualstudio.com/items?itemName=kleinesfilmroellchen.serenity-dsl-syntaxhighlight) | [open-vsx.org](https://open-vsx.org/extension/kleinesfilmroellchen/serenity-dsl-syntaxhighlight))
* twxs.cmake
* wayou.vscode-todo-highlight

### .clangd
```yaml
---
CompileFlags:
  CompilationDatabase: Build/i686clang
```

### .vscode/settings.json
```jsonc
{
  // Pass additional arguments to clangd
  "clangd.arguments": [
    // Inform clangd of where our TARGET `compile_commands.json` lives
    "--compile-commands-dir=${workspaceFolder}/Build/i686clang"
  ],
  // Use the clangd binary built by clang-toolchain
  "clangd.path": "${workspaceFolder}/Toolchain/Local/clang/bin/clangd",
  // Inform clang-tidy of where our TARGET `compile_commands.json` lives
  "clang-tidy.buildPath": "${workspaceFolder}/Build/i686clang",
  // Use the clang-tidy binary built by clang-toolchain
  "clang-tidy.executable": "${workspaceFolder}/Toolchain/Local/clang/bin/clang-tidy",
  // File paths to exclude from the explorer, which are also implicitly excluded from search
  "files.exclude": {
    "**/.git": true,
    "Build/**": true,
    "Toolchain/Build/**": true,
    "Toolchain/Local/**": true,
    "Toolchain/Tarballs/**": true,
  },
  // Set our tab size to 4 columns
  "editor.tabSize": 4,
  // Disable the use of tab stops
  "editor.useTabStops": false,
  // Disable word wrap because Serenity does not define a maximum column width
  "editor.wordWrap": "off",
}
```

Code Snippets
---

### License Header
The following snippet may be useful if you want to quickly generate a license header:
```jsonc
// .vscode/license.code-snippets
{
    "License": {
        "scope": "cpp,c",
        "prefix": "license",
        "body": [
            "/*",
            " * Copyright (c) $CURRENT_YEAR, ${1:Your Name} <${2:YourName@Email.com}>.",
            " *",
            " * SPDX-License-Identifier: BSD-2-Clause",
            " */"
        ],
        "description": "Generate license header"
    }
}
```
