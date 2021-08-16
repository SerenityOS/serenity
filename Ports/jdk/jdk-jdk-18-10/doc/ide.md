% IDE support in the JDK

## Introduction

When you are familiar with building and testing the JDK, you may want to
configure an IDE to work with the source code. The instructions differ a bit
depending on whether you are interested in working with the native (C/C++) or
the Java code.

### IDE support for native code

There are a few ways to generate IDE configuration for the native sources,
depending on which IDE to use.

#### Visual Studio Code

The make system can generate a [Visual Studio Code](https://code.visualstudio.com)
workspace that has C/C++ source indexing configured correctly, as well as
launcher targets for tests and the Java launcher. After configuring, a workspace
for the configuration can be generated using:

```shell
make vscode-project
```

This creates a file called `jdk.code-workspace` in the build output folder. The
full location will be printed after the workspace has been generated. To use it,
choose `File -> Open Workspace...` in Visual Studio Code.

##### Alternative indexers

The main `vscode-project` target configures the default C++ support in Visual
Studio Code. There are also other source indexers that can be installed, that
may provide additional features. It's currently possible to generate
configuration for two such indexers, [clangd](https://clang.llvm.org/extra/clangd/)
and [rtags](https://github.com/Andersbakken/rtags). These can be configured by
appending the name of the indexer to the make target, such as:

```shell
make vscode-project-clangd
```

Additional instructions for configuring the given indexer will be displayed
after the workspace has been generated.

#### Visual Studio

This section is a work in progress.

```shell
make ide-project
```

#### Compilation Database

The make system can generate generic native code indexing support in the form of
a [Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html)
that can be used by many different IDEs and source code indexers.

```shell
make compile-commands
```

It's also possible to generate the Compilation Database for the HotSpot source
code only, which is a bit faster as it includes less information.

```shell
make compile-commands-hotspot
```

### IDE support for Java code

This section is a work in progress.