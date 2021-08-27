# Visual Studio Code Project Configuration

Visual Studio Code does not work optimally for Serenity development, and there's a bunch of configuring and fiddling around you'll have to do.

The WSL Remote extension allows you to use VSCode in Windows while using the normal WSL workflow. This works surprisingly well, but for code comprehension speed you should put the Serenity directory on your WSL root partition.

## Note on CMake

The CMake Tools plugin for VSCode does not work with projects that don't accept a CMAKE_BUILD_TYPE. See also [this CMake Tools issue](https://github.com/microsoft/vscode-cmake-tools/issues/1639); an appropriate feature is planned for 1.9.0. For now, it is best to disable all CMake extensions when working on Serenity.

## Code comprehension

Both C++ comprehension tools listed below report fake errors.

### clangd

The official clangd extension can be used for C++ comprehension. You'll have to use the following .clangd:

```yaml
CompileFlags:
  CompilationDatabase: Build/i686
```

Run cmake at least once for this to work. clangd has difficulty finding specific methods and types, especially with inheritance trees. Also, include errors are wrong in 90% of cases.

### Microsoft C/C++ tools

These extensions can be used as-is, but you need to point them to the custom Serenity compilers. Use the following cpp-preferences to circumvent some errors:

<details>
<summary>.vscode/c_cpp_propperties.json</summary>

```json
{
    "name": "Userspace",
    "includePath": [
        "${workspaceFolder}",
        "${workspaceFolder}/Build/i686/",
        "${workspaceFolder}/Build/i686/Userland",
        "${workspaceFolder}/Build/i686/Userland/Applications",
        "${workspaceFolder}/Build/i686/Userland/Libraries",
        "${workspaceFolder}/Build/i686/Userland/Services",
        "${workspaceFolder}/Build/i686/Root/usr/include/**",
        "${workspaceFolder}/Userland",
        "${workspaceFolder}/Userland/Libraries",
        "${workspaceFolder}/Userland/Libraries/LibC",
        "${workspaceFolder}/Userland/Libraries/LibM",
        "${workspaceFolder}/Userland/Libraries/LibPthread",
        "${workspaceFolder}/Userland/Services",
        "${workspaceFolder}/Toolchain/Local/i686/i686-pc-serenity/include/c++/**"
    ],
    "defines": [
        "DEBUG",
        "__serenity__",
    ],
    "compilerPath": "${workspaceFolder}/Toolchain/Local/i686/bin/i686-pc-serenity-g++",
    "cStandard": "c17",
    "cppStandard": "c++20",
    "intelliSenseMode": "linux-gcc-x86",
    "browse": {
        "path": [
            "${workspaceFolder}",
            "${workspaceFolder}/Build/i686/",
            "${workspaceFolder}/Build/i686/Userland",
            "${workspaceFolder}/Build/i686/Userland/Applications",
            "${workspaceFolder}/Build/i686/Userland/Libraries",
            "${workspaceFolder}/Build/i686/Userland/Services",
            "${workspaceFolder}/Build/i686/Root/usr/include/**",
            "${workspaceFolder}/Userland",
            "${workspaceFolder}/Userland/Libraries",
            "${workspaceFolder}/Userland/Libraries/LibC",
            "${workspaceFolder}/Userland/Libraries/LibM",
            "${workspaceFolder}/Userland/Libraries/LibPthread",
            "${workspaceFolder}/Userland/Services",
            "${workspaceFolder}/Toolchain/Local/i686/i686-pc-serenity/include/c++/**"
        ],
        "limitSymbolsToIncludedHeaders": true
    },
    "compileCommands": "Build/i686/compile_commands.json",
    "compilerArgs": [
        "-wall",
        "-wextra",
        "-werror"
    ]
}
```
</details>

Most nonsentical errors from the extension also involve not finding methods, types etc.

### DSL syntax highlighting

There's a syntax highlighter extension for both IPC and GML called "SerenityOS DSL Syntax Highlight", available [here](https://marketplace.visualstudio.com/items?itemName=kleinesfilmroellchen.serenity-dsl-syntaxhighlight) or [here](https://open-vsx.org/extension/kleinesfilmroellchen/serenity-dsl-syntaxhighlight).

## Formatting

clang-format is included with the Microsoft tools (see above). The settings below include a key that makes it use the proper style. Alternatively, you can use the clang-format extension itself, which should work out of the box.

## Settings

These belong in the `.vscode/settings.json` of Serenity.

```json
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
// Force clang-format to respect Serenity's .clang-format style file.
"C_Cpp.clang_format_style": "file",
// Tab settings
"editor.tabSize": 4,
"editor.useTabStops": false,
```

## Customization

### Custom Tasks

You can create custom tasks (`.vscode/tasks.json`) to quickly compile Serenity. The following are two example tasks, a "just build" task and a "build and run" task for i686 that also give you error highlighting:

```json
{
    "tasks": [
        {
            "label": "build",
            "type": "shell",
            "problemMatcher": [
                {
                    "base": "$gcc",
                    "fileLocation": [
                        "relative",
                        "${workspaceFolder}/Build/i686"
                    ]
                },
                {
                    "source": "gcc",
                    "fileLocation": [
                        "relative",
                        "${workspaceFolder}/Build/i686"
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
            "command": [
                "bash"
            ],
            "args": [
                "-c",
                "\"Meta/serenity.sh image\""
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
            "label": "launch",
            "type": "shell",
            "linux": {
                "command": "bash",
                "args": [
                    "-c",
                    "\"Meta/serenity.sh run\""
                ],
                "options": {
                    "env": {
                        // Put your custom run configuration here, e.g. SERENITY_RAM_SIZE
                    }
                }
            },
            "problemMatcher": [
                {
                    "source": "Assertion Failed",
                    "fileLocation": [
                        "relative",
                        "${workspaceFolder}/Build/i686"
                    ],
                    "pattern": [
                        {
                            "regexp": "ASSERTION FAILED: (.*)$",
                            "message": 1
                        },
                        {
                            "regexp": "^((?:.*)\\.(h|cpp|c|S)):(\\d*)$",
                            "file":1,
                            "location":3
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
                    "base": "$gcc",
                    "fileLocation": [
                        "relative",
                        "${workspaceFolder}/Build/i686"
                    ]
                },
                {
                    "source": "gcc",
                    "fileLocation": [
                        "relative",
                        "${workspaceFolder}/Build/i686"
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
            "presentation": {
                "echo": true,
                "reveal": "always",
                "group": "run",
                "focus": false,
                "panel": "shared",
                "showReuseMessage": true,
                "clear": true
            }
        },
    ]
}
```

This can easily be adopted into x86_64 by appending the architecture to the serenity.sh commands.

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
        "description": "Licence header"
    }
}
```
