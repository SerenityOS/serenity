# Visual Studio Code Project Configuration

Visual Studio Code does not work optimally for Serenity development, and there's a bunch of configuring and fiddling around you'll have to do.

The WSL Remote extension allows you to use VSCode in Windows while using the normal WSL workflow. This works surprisingly well, but for code comprehension speed you should put the Serenity directory on your WSL root partition.

## Code comprehension

Both C++ comprehension tools listed below report fake errors.

### clangd

The official clangd extension can be used for C++ comprehension. It is recommended in general, as it is most likely to work on all platforms. You'll have to use the following .clangd:

```yaml
CompileFlags:
  Add: [-D__serenity__, -I/path/to/serenity/Toolchain/Local/x86_64/x86_64-pc-serenity/include/c++/12.2.0, -I/path/to/serenity/Toolchain/Local/x86_64/x86_64-pc-serenity/include/c++/12.2.0/x86_64-pc-serenity]
  CompilationDatabase: Build/x86_64
```

You only need the `Add:` line if you're using GCC. Run cmake at least once for this to work. There's some configuring to do for the include paths: First, replace `/path/to/serenity` with the actual path to the Serenity source folder. Then, you should replace the toolchain subdirectory and target triple architecture, here `x86_64`, with the one you're compiling most often. And finally, the compiler version (`12.2.0`) might need to be updated in the future. Just look in the `c++` parent folder and see what's currently there.

A known issue with clangd in general is that it likes to crash. You can usually just restart it via the command palette. If that doesn't help, close currently open C++ files and/or switch branches before restarting, which helps sometimes.

### Microsoft C/C++ tools

These extensions can be used as-is, but you need to point them to the custom Serenity compilers. Use the following cpp-preferences to circumvent some errors:

<details>
<summary>.vscode/c_cpp_properties.json</summary>

```json
{
    "configurations": [
        {
            "name": "userland-i386-gcc",
            "includePath": [
                "${workspaceFolder}",
                "${workspaceFolder}/Build/x86_64/",
                "${workspaceFolder}/Build/x86_64/Userland",
                "${workspaceFolder}/Build/x86_64/Userland/Applications",
                "${workspaceFolder}/Build/x86_64/Userland/Libraries",
                "${workspaceFolder}/Build/x86_64/Userland/Services",
                "${workspaceFolder}/Build/x86_64/Root/usr/include/**",
                "${workspaceFolder}/Userland",
                "${workspaceFolder}/Userland/Libraries",
                "${workspaceFolder}/Userland/Libraries/LibC",
                "${workspaceFolder}/Userland/Services",
                "${workspaceFolder}/Toolchain/Local/x86_64/x86_64-pc-serenity/include/c++/**"
            ],
            "defines": [
                "DEBUG",
                "__serenity__"
            ],
            "compilerPath": "${workspaceFolder}/Toolchain/Local/x86_64/bin/x86_64-pc-serenity-g++",
            "cStandard": "c17",
            "cppStandard": "c++20",
            "intelliSenseMode": "linux-gcc-x86",
            "compileCommands": "Build/x86_64/compile_commands.json",
            "compilerArgs": [
                "-Wall",
                "-Wextra",
                "-Werror"
            ],
            "browse": {
                "path": [
                    "${workspaceFolder}",
                    "${workspaceFolder}/Build/x86_64/",
                    "${workspaceFolder}/Build/x86_64/Userland",
                    "${workspaceFolder}/Build/x86_64/Userland/Applications",
                    "${workspaceFolder}/Build/x86_64/Userland/Libraries",
                    "${workspaceFolder}/Build/x86_64/Userland/Services",
                    "${workspaceFolder}/Build/x86_64/Root/usr/include/**",
                    "${workspaceFolder}/Userland",
                    "${workspaceFolder}/Userland/Libraries",
                    "${workspaceFolder}/Userland/Libraries/LibC",
                    "${workspaceFolder}/Userland/Services",
                    "${workspaceFolder}/Toolchain/Local/x86_64/x86_64-pc-serenity/include/c++/**"
                ],
                "limitSymbolsToIncludedHeaders": true,
                "databaseFilename": "${workspaceFolder}/Build/x86_64/"
            }
        }
    ],
    "version": 4
}
```
</details>

Most nonsensical errors from the extension also involve not finding methods, types etc.

### DSL syntax highlighting

There's a syntax highlighter extension for both IPC and GML called "SerenityOS DSL Syntax Highlight", available [here](https://marketplace.visualstudio.com/items?itemName=kleinesfilmroellchen.serenity-dsl-syntaxhighlight) or [here](https://open-vsx.org/extension/kleinesfilmroellchen/serenity-dsl-syntaxhighlight).

## Formatting

clang-format is included with the Microsoft tools (see above); there's also a separate extension which works well. The settings below include a key that makes it use the proper style. Alternatively, you can use the clang-format extension itself, which should work out of the box.

## Settings

These belong in the `.vscode/settings.json` of Serenity.

```json
{
    // Excluding the generated directories keeps your file view clean and speeds up search.
    "files.exclude": {
        "**/.git": true,
        "Toolchain/Local/**": true,
        "Toolchain/Tarballs/**": true,
        "Toolchain/Build/**": true,
        "Build/**": true,
        "build/**": true,
    },
    "search.exclude": {
        "**/.git": true,
        "Toolchain/Local/**": true,
        "Toolchain/Tarballs/**": true,
        "Toolchain/Build/**": true,
        "Build/**": true,
        "build/**": true,
    },
    // Force clang-format to respect Serenity's .clang-format style file. This is not necessary if you're not using the Microsoft C++ extension.
    "C_Cpp.clang_format_style": "file",
    // Tab settings
    "editor.tabSize": 4,
    "editor.useTabStops": false,
    // format trailing new lines
    "files.trimFinalNewlines": true,
    "files.insertFinalNewline": true,
    // git commit message length
    "git.inputValidationLength": 72,
    "git.inputValidationSubjectLength": 72
}
```

## Customization

### Custom Tasks

You can create custom tasks (`.vscode/tasks.json`) to quickly compile Serenity.
The following three example tasks should suffice in most situations, and allow you to specify the build system to use, as well as give you error highlighting.

Note: The Assertion und KUBSan Problem matchers will only run after you have closed qemu.

<details>
<summary>tasks.json</summary>

```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "build lagom",
            "type": "shell",
            "problemMatcher": [
                {
                    "base": "$gcc",
                    "fileLocation": [
                        "relative",
                        "${workspaceFolder}/Build/lagom"
                    ]
                }
            ],
            "command": [
                "bash"
            ],
            "args": [
                "-c",
                "\"Meta/serenity.sh build lagom\""
            ],
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "group": "build",
                "panel": "shared",
                "showReuseMessage": true,
                "clear": true
            }
        },
        {
            "label": "build",
            "type": "shell",
            "command": "bash",
            "args": [
                "-c",
                "Meta/serenity.sh build ${input:arch} ${input:compiler}"
            ],
            "problemMatcher": [
                {
                    "base": "$gcc",
                    "fileLocation": [
                        "relative",
                        // FIXME: Clang uses ${input:arch}clang
                        "${workspaceFolder}/Build/${input:arch}"
                    ]
                },
                {
                    "source": "gcc",
                    "fileLocation": [
                        "relative",
                        // FIXME: Clang uses ${input:arch}clang
                        "${workspaceFolder}/Build/${input:arch}"
                    ],
                    "pattern": [
                        {
                            "regexp": "^([^\\s]*\\.S):(\\d*): (.*)$",
                            "file": 1,
                            "location": 2,
                            "message": 3
                        }
                    ]
                }
            ],
            "group": {
                "kind": "build",
                "isDefault": true
            }
        },
        {
            "label": "launch",
            "type": "shell",
            "command": "bash",
            "args": [
                "-c",
                "Meta/serenity.sh run ${input:arch} ${input:compiler}"
            ],
            "options": {
                "env": {
                    // Put your custom run configuration here, e.g. SERENITY_RAM_SIZE
                }
            },
            "problemMatcher": [
                {
                    "base": "$gcc",
                    "fileLocation": [
                        "relative",
                        // FIXME: Clang uses ${input:arch}clang
                        "${workspaceFolder}/Build/${input:arch}"
                    ]
                },
                {
                    "source": "gcc",
                    "fileLocation": [
                        "relative",
                        // FIXME: Clang uses ${input:arch}clang
                        "${workspaceFolder}/Build/${input:arch}"
                    ],
                    "pattern": [
                        {
                            "regexp": "^([^\\s]*\\.S):(\\d*): (.*)$",
                            "file": 1,
                            "location": 2,
                            "message": 3
                        }
                    ]
                },
                {
                    "source": "KUBSan",
                    "owner": "cpp",
                    "fileLocation": [
                        "relative",
                        "${workspaceFolder}"
                    ],
                    "pattern": [
                        {
                            "regexp": "KUBSAN: (.*)",
                            "message": 0
                        },
                        {
                            "regexp": "KUBSAN: at ../(.*), line (\\d*), column: (\\d*)",
                            "file": 1,
                            "line": 2,
                            "column": 3
                        }
                    ]
                },
                {
                    "source": "Assertion Failed",
                    "owner": "cpp",
                    "pattern": [
                        {
                            "regexp": "ASSERTION FAILED: (.*)$",
                            "message": 1
                        },
                        {
                            "regexp": "^((?:.*)\\.(h|cpp|c|S)):(\\d*)$",
                            "file": 1,
                            "location": 3
                        }
                    ],
                    "fileLocation": [
                        "relative",
                        // FIXME: Clang uses ${input:arch}clang
                        "${workspaceFolder}/Build/${input:arch}"
                    ]
                }
            ]
        }
    ],
    "inputs": [
        {
            "id": "compiler",
            "description": "Compiler to use",
            "type": "pickString",
            "default": "GNU",
            "options": [
                "GNU",
                "Clang"
            ]
        },
        {
            "id": "arch",
            "description": "Architecture to compile for",
            "type": "pickString",
            "default": "x86_64",
            "options": [
                "i686",
                "x86_64",
                "aarch64"
            ]
        }
    ]
}
```

</details>

### License snippet

The following snippet may be useful if you want to quickly generate a license header, put it in `.vscode/serenity.code-snippets`:
```json
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
        "description": "License header"
    }
}
```
