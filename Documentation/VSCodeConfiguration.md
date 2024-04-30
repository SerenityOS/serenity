# Visual Studio Code Project Configuration

Visual Studio Code requires some configuration files, and a tailored ``settings.json`` file to understand serenity.

The WSL Remote extension allows you to use VS Code in Windows while using the normal WSL workflow. This works well, but for code comprehension speed you should put the Serenity directory on your WSL root partition.

The recommended extensions for VS Code include:

- [clangd](https://marketplace.visualstudio.com/items?itemName=llvm-vs-code-extensions.vscode-clangd)
- [GitLens](https://marketplace.visualstudio.com/items?itemName=eamodio.gitlens)

## Code comprehension

Clangd has the best support for cross-compiling workflows, especially if configured as noted below. The Microsoft C/C++ tools can work, but require a lot more configuration and may not understand the sysroot in use.

### clangd

The official clangd extension can be used for C++ comprehension. It is recommended in general, as it is most likely to work on all platforms.

clangd uses ``compile_commands.json`` files to understand the project. CMake will generate these in either Build/x86_64, Build/x86_64clang, and Build/lagom.
Depending on which configuration you use most, set the CompilationDatabase configuration item in the below ``.clangd`` file accordingly. It goes at the root of your checkout (``serenity/.clangd``):

```yaml
CompileFlags:
  Add: [-D__serenity__]
  CompilationDatabase: Build/x86_64
  
Diagnostics:
  UnusedIncludes: None
  MissingIncludes: None
```

The UnusedIncludes and MissingIncludes flags are used to disable the [Include Cleaner](https://clangd.llvm.org/design/include-cleaner) feature of newer clangd releases.
It can be re-enabled if you don't mind the noisy inlay hints and problems in the problem view.

Run ``./Meta/serenity.sh run`` at least once to generate the ``compile_commands.json`` file.

In addition to the ``.clangd`` file, the ``settings.json`` file below has a required ``clangd.arguments`` entry for ``--query-driver`` that allows clangd to find the cross-compiler's built-in include paths.

#### Known issues

- Some distribution clangd packages still have issues identifying paths to the serenity cross-compilers' builtin include paths after supplying the ``--query-driver`` option from ``settings.json``. This has been seen on at least Debian. If the inlay hints suggest that ``<new>`` cannot be found, first triple check your configuration matches the ``.clangd`` file from this section, verify that you've run the OS via ``Meta/serenity.sh run``, and quadruple check your ``clangd.arguments`` section in the project-local ``settings.json`` file. If all of the above are correct, building ``clangd`` from the serenity clang toolchain is known to work. See [AdvancedBuildInstructions](AdvancedBuildInstructions.md#serenity-aware-clang-tools) for steps on how to build it from source. After building from source, be sure to set ``clangd.path`` in your ``settings.json`` to ``${workspaceFolder}/Toolchain/Local/clang/bin/clangd``.

- clangd has a tendency to crash when stressing bleeding edge compiler features. You can usually just restart it via the command palette. If that doesn't help, close currently open C++ files and/or switch branches before restarting, which helps sometimes.

### DSL syntax highlighting

There's a syntax highlighter extension for SerenityOS DSLs called "SerenityOS DSL Syntax Highlight", available [here](https://marketplace.visualstudio.com/items?itemName=kleinesfilmroellchen.serenity-dsl-syntaxhighlight) or [here](https://open-vsx.org/extension/kleinesfilmroellchen/serenity-dsl-syntaxhighlight).
The extension provides syntax highlighting for LibIPC's IPC files, LibGUI's GUI Markup Language (GML), [Web IDL](https://webidl.spec.whatwg.org/), and LibJS's
serialization format (no extension) as output by js with the -d option.

### Microsoft C/C++ tools

This extension can be used as-is, but you need to point it to the custom Serenity compilers. Note that enabling the extension in the same workspace as the
clangd and clang-format extensions will cause conflicts. If you choose to use Microsoft C/C++ Tools rather than clangd and clang-format, use the
following ``c_cpp_properties.json`` to circumvent some errors. Even with the configuration in place, the extension will likely still report errors related to types and methods not being found.

<details>
<summary>.vscode/c_cpp_properties.json</summary>

```json
{
    "configurations": [
        {
            "name": "userland-x86_64-gcc",
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
            "cppStandard": "c++23",
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

## Formatting

clangd provides code formatting out of the box using the ``clang-format`` engine. ``clang-format`` support is also included with the Microsoft C/C++ tools (see above). The settings below include a key that makes the Microsoft extension use the proper style.

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
    "git.inputValidationSubjectLength": 72,
    // Tell clangd to ask the cross-compilers for their builtin include paths
    "clangd.arguments": [
        "--query-driver=${workspaceFolder}/Toolchain/Local/**/*",
        "--header-insertion=never" // See https://github.com/clangd/clangd/issues/1247
    ]
}
```

## Customization

### Custom Tasks

You can create custom tasks (`.vscode/tasks.json`) to quickly compile Serenity.
The following three example tasks should suffice in most situations, and allow you to specify the build system to use, as well as give you error highlighting.

Note: The Assertion und KUBSan Problem matchers will only run after you have closed qemu.

<details>
<summary>.vscode/tasks.json</summary>

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
