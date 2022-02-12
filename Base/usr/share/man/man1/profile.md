## Name

profile

## Synopsis

```sh
$ profile [-p PID] [-a] [-e] [-d] [-f] [-w] [-c command] [-t event_type]
```

## Options:

* `--help`: Display help message and exit
* `--version`: Print version
* `-p PID`: Target PID
* `-a`: Profile all processes (super-user only), result at /proc/profile
* `-e`: Enable
* `-d`: Disable
* `-f`: Free the profiling buffer for the associated process(es).
* `-w`: Enable profiling and wait for user input to disable.
* `-c command`: Command
* `-t event_type`: Enable tracking specific event type

Event type can be one of: sample, context_switch, page_fault, syscall, read, kmalloc and kfree.

<!-- Auto-generated through ArgsParser -->
