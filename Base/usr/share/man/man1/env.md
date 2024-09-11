## Name

env - Execute a command with a modified environment

## Synopsis

```sh
$ env [--ignore-environment] [--split-string S] [--unset name] [env/command...]
```

## Options

-   `-i`, `--ignore-environment`: Start with an empty environment
-   `-S S`, `--split-string S`: Process and split S into separate arguments; used to pass multiple arguments on shebang lines
-   `-u name`, `--unset name`: Remove variable from the environment

## Arguments

-   `env/command`: Environment and commands
