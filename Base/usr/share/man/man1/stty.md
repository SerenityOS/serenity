## Name

stty - set or print terminal characteristics

## Synopsis

```sh
$ stty [-a] [-g] [-F device] [setting...]
```

## Description

`stty` sets or displays terminal settings for the current terminal or a specified terminal device. Without arguments, it prints the current terminal settings. With settings specified, it applies those settings to the terminal.

## Options

-   `-a`, `--all`: Print all current terminal settings in human-readable format, including default values
-   `-g`, `--save`: Print current terminal settings in a stty-readable format suitable for later restoration
-   `-F device`, `--file device`: Operate on the specified device instead of stdin

The `-g` and `-a` options are mutually exclusive.

## Settings

Settings can be specified in several ways. To disable a setting, prefix it with a dash (`-`).

### Special settings

-   `sane`: Reset all terminal settings to sensible defaults
-   `raw`: Enable raw mode (disable all input and output processing)
-   `nl`: Disable ICRNL (don't translate carriage return to newline)
-   `-nl`: Enable ICRNL (translate carriage return to newline)
-   `ek`: Reset erase and kill characters to their default values
-   `evenp`, `parity`: Enable even parity (7-bit characters)
-   `oddp`: Enable odd parity (7-bit characters)
-   `-parity`, `-evenp`, `-oddp`: Disable parity (8-bit characters)

### Baud rate

Specify a numeric value to set both input and output baud rates:

-   `50`, `75`, `110`, `134`, `150`, `200`, `300`, `600`, `1200`, `1800`, `2400`, `4800`, `9600`, `19200`, `38400`, `57600`, `115200`, `230400`, `460800`, `500000`, `576000`, `921600`, `1000000`, `1152000`, `1500000`, `2000000`, `2500000`, `3000000`, `3500000`, `4000000`

-   `ispeed N`: Set input baud rate to N
-   `ospeed N`: Set output baud rate to N

### Window size

-   `rows N`: Set terminal height to N rows
-   `columns N`, `cols N`: Set terminal width to N columns

### Control characters

Control characters can be set using various formats:

-   `intr`: Interrupt character (default: `^C`)
-   `quit`: Quit character (default: `^\`)
-   `erase`: Erase character (default: `^?`)
-   `kill`: Kill line character (default: `^U`)
-   `eof`: End-of-file character (default: `^D`)
-   `swtc`: Switch character
-   `start`: Start output character (default: `^Q`)
-   `stop`: Stop output character (default: `^S`)
-   `susp`: Suspend character (default: `^Z`)
-   `eol`: End-of-line character
-   `reprint`: Reprint line character (default: `^R`)
-   `discard`: Discard output character (default: `^O`)
-   `werase`: Word erase character (default: `^W`)
-   `lnext`: Literal next character (default: `^V`)
-   `eol2`: Alternate end-of-line character
-   `min`: Minimum number of characters for non-canonical read
-   `time`: Timeout in deciseconds for non-canonical read

Control character values can be specified as:

-   A single character: `a`
-   Caret notation: `^C` (Ctrl+C)
-   Hexadecimal: `0x03`
-   Octal: `03`
-   Decimal: `3`
-   Disable: `^-` or `undef`

### Input flags

-   `ignbrk`: Ignore break condition
-   `brkint`: Signal on break
-   `ignpar`: Ignore parity errors
-   `parmer`: Mark parity errors
-   `inpck`: Enable input parity checking
-   `istrip`: Strip high bit from input characters
-   `inlcr`: Translate newline to carriage return on input
-   `igncr`: Ignore carriage return on input
-   `icrnl`: Translate carriage return to newline on input
-   `iuclc`: Translate uppercase to lowercase on input
-   `ixon`: Enable XON/XOFF flow control on output
-   `ixany`: Allow any character to restart output
-   `ixoff`: Enable XON/XOFF flow control on input
-   `imaxbel`: Ring bell when input queue is full
-   `iutf8`: Assume input is UTF-8

### Output flags

-   `opost`: Enable output processing
-   `olcuc`: Translate lowercase to uppercase on output
-   `onlcr`: Translate newline to carriage return-newline on output
-   `onlret`: Newline performs carriage return function
-   `ofill`: Use fill characters for delays
-   `ofdel`: Use DEL instead of NUL for fill characters

### Control flags

-   `cs5`, `cs6`, `cs7`, `cs8`: Set character size to 5, 6, 7, or 8 bits
-   `cstopb`: Use two stop bits (one with `-`)
-   `cread`: Enable receiver
-   `parenb`: Enable parity generation and detection
-   `parodd`: Use odd parity (even with `-`)
-   `hupcl`: Hang up modem on last close
-   `clocal`: Ignore modem control lines

### Local flags

-   `isig`: Enable interrupt, quit, and suspend special characters
-   `icanon`: Enable canonical input (erase and kill processing)
-   `echo`: Echo input characters
-   `echoe`: Echo erase character as backspace-space-backspace
-   `echok`: Echo newline after kill character
-   `echonl`: Echo newline even if not echoing other characters
-   `noflsh`: Don't flush after interrupt and quit characters
-   `tostop`: Send SIGTTOU for background output
-   `iexten`: Enable extended input processing

## Examples

Display current terminal settings:

```sh
$ stty
speed 38400 baud; rows 24; columns 80;
```

Save terminal settings for later restoration:

```sh
$ stty -g > saved_settings
$ stty intr ^X  # Make some changes (changes the interrupt key to Ctrl + X)
$ stty $(cat saved_settings)  # Restore settings
```

Set the terminal to raw mode:

```sh
$ stty raw
```

Set the baud rate to 115200:

```sh
$ stty 115200
```

Set the terminal size:

```sh
$ stty rows 30 cols 100
```

Reset terminal to sane defaults:

```sh
$ stty sane
```

## See also

-   [`tty`(1)](help://man/1/tty)
