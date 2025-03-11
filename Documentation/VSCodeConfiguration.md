# Visual Studio Code Project Configuration

Visual Studio Code requires some configuration files, and a tailored `settings.json` file to understand serenity.

The WSL Remote extension allows you to use VS Code in Windows while using the normal WSL workflow. This works well, but for code comprehension speed you should put the Serenity directory on your WSL root partition.

The recommended extensions for VS Code include:

-   [clangd](https://marketplace.visualstudio.com/items?itemName=llvm-vs-code-extensions.vscode-clangd)
-   [GitLens](https://marketplace.visualstudio.com/items?itemName=eamodio.gitlens)
-   [Jakt](https://github.com/SerenityOS/jakt/tree/main/editors/vscode) (See [Jakt](#jakt) for more information)

## Code comprehension

Clangd has the best support for cross-compiling workflows, especially if configured as noted below. The Microsoft C/C++ tools can work, but require a lot more configuration and may not understand the sysroot in use.

See [ClangdConfiguration](ClangdConfiguration.md) for information on how to configure clangd.

### DSL syntax highlighting

There's a syntax highlighter extension for SerenityOS DSLs called "SerenityOS DSL Syntax Highlight", available [here](https://marketplace.visualstudio.com/items?itemName=kleinesfilmroellchen.serenity-dsl-syntaxhighlight) or [here](https://open-vsx.org/extension/kleinesfilmroellchen/serenity-dsl-syntaxhighlight).
The extension provides syntax highlighting for LibIPC's IPC files, LibGUI's GUI Markup Language (GML), [Web IDL](https://webidl.spec.whatwg.org/), and LibJS's
serialization format (no extension) as output by js with the -d option.

### Microsoft C/C++ tools

This extension can be used as-is, but you need to point it to the custom Serenity compilers. Note that enabling the extension in the same workspace as the
clangd and clang-format extensions will cause conflicts. If you choose to use Microsoft C/C++ Tools rather than clangd and clang-format, use the
following `c_cpp_properties.json` to circumvent some errors. Even with the configuration in place, the extension will likely still report errors related to types and methods not being found.

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
            "defines": ["DEBUG", "__serenity__"],
            "compilerPath": "${workspaceFolder}/Toolchain/Local/x86_64/bin/x86_64-pc-serenity-g++",
            "cStandard": "c17",
            "cppStandard": "c++23",
            "intelliSenseMode": "linux-gcc-x86",
            "compileCommands": "Build/x86_64/compile_commands.json",
            "compilerArgs": ["-Wall", "-Wextra", "-Werror"],
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

clangd provides code formatting out of the box using the `clang-format` engine. `clang-format` support is also included with the Microsoft C/C++ tools (see above). The settings below include a key that makes the Microsoft extension use the proper style.

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
        "build/**": true
    },
    "search.exclude": {
        "**/.git": true,
        "Toolchain/Local/**": true,
        "Toolchain/Tarballs/**": true,
        "Toolchain/Build/**": true,
        "Build/**": true,
        "build/**": true
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
    // See ClangdConfiguration.md for arguments that may be needed.
    "clangd.arguments": [],
    // Set if needed.
    "clangd.path": "..."
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
                    "fileLocation": ["relative", "${workspaceFolder}/Build/lagom"]
                }
            ],
            "command": ["bash"],
            "args": ["-c", "\"Meta/serenity.sh build lagom\""],
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
            "args": ["-c", "Meta/serenity.sh build ${input:arch} ${input:compiler}"],
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
            "args": ["-c", "Meta/serenity.sh run ${input:arch} ${input:compiler}"],
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
                    "fileLocation": ["relative", "${workspaceFolder}"],
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
            "options": ["GNU", "Clang"]
        },
        {
            "id": "arch",
            "description": "Architecture to compile for",
            "type": "pickString",
            "default": "x86_64",
            "options": ["x86_64", "aarch64"]
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

## Jakt

To use Jakt, build and install the jakt extension from the [Jakt repository](https://github.com/SerenityOS/jakt/tree/main/editors/vscode).
A few configuration options are required to have `import extern` statements work correctly, which should go into your `settings.json`:

```json
{
    // If you have installed jakt globally, you can omit this setting (though keep in mind that the compiler build *should* match the one in your serenity checkout)
    "jaktLanguageServer.compiler.executablePath": "Toolchain/Local/jakt/bin/jakt",

    "jaktLanguageServer.extraCompilerImportPaths": [
        ".",
        "Userland/Libraries",
        "Userland/Libraries/LibCrypt",
        "Userland/Libraries/LibSystem",
        "Userland/Services",
        "Userland",
        "Build/x86_64",
        "Build/x86_64/Userland/Services",
        "Build/x86_64/Userland/Libraries",
        "Build/x86_64/Userland"
    ]
}
```

Note that the build directories are architecture-specific, so you may need to adjust them if the generated headers are different per architecture.
