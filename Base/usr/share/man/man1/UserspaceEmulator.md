## Name

UserspaceEmulator

## Synopsis

```sh
$ UserspaceEmulator [--report-to-debug] [--pause] [--profile] [--profile-interval num_instructions] [--profile-file path] [--roi] <command...>
```

## Options:

* `--help`: Display help message and exit
* `--version`: Print version
* `--report-to-debug`: Write reports to the debug log
* `-p`, `--pause`: Pause on startup
* `--profile`: Generate a ProfileViewer-compatible profile
* `-i num_instructions`, `--profile-interval num_instructions`: Set the profile instruction capture interval, 128 by default
* `--profile-file path`: File path for profile dump
* `--roi`: Enable Region-of-Interest mode for profiling

## Arguments:

* `command`: Command to emulate

<!-- Auto-generated through ArgsParser -->
