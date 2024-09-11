## Name

beep - beep the pc speaker

## Synopsis

```**sh
$ beep
```

## Description

beep allows the user to beep the PC speaker.

## Options

-   `-f frequency`, `--beep-tone frequency`: Beep tone (frequency in Hz)
-   `-n N`, `--duration N`: Duration (N in milliseconds)

## Notes

If the user disabled the usage of PC speaker in the kernel commandline, the program
will fail to use the PC speaker.

## Examples

```sh
# Use beep with default tone
$ beep
# Use beep with tone of 1000Hz
$ beep -f 1000
# Use beep with tone of 1000Hz for 1 second
$ beep -f 1000 -n 1000
```

## See also

-   [`boot_parameters`(7)](help://man/7/boot_parameters)
