# Neovim project configuration

Those steps assume you have LSP with clangd setup

First you need to create a `.clangd` file in the root of the serenity repository.
This file tells clangd which compiler flags should it use when "looking" at the code.
It should look like the one below, please note that you need to replace `/path/to/serenity` with your serenity working directory. Please also check if the compiler version is correct as it might change in the future.
```yaml
CompileFlags:
  Add: [-D__serenity__, -I/path/to/serenity/Toolchain/Local/x86_64/x86_64-pc-serenity/include/c++/12.2.0, -I/path/to/serenity/Toolchain/Local/x86_64/x86_64-pc-serenity/include/c++/12.2.0/x86_64-pc-serenity]
  CompilationDatabase: Build/x86_64
```

The second step is to generate the `compile_commands.json` file, to do that just run `Meta/serenity.sh rebuild` you will need to run this command every time new source files are added. 

The third and final step is to link the `compile_commands.json` file to the repository root.
```bash
ln -s /path/to/serenity/Build/x86_64/compile_commands.json /path/to/serenity/compile_commands.json
```

That's it now when you run Neovim it should pick up the `compile_commands.json` file automagically and stuff like linting and code completion should work :^)
