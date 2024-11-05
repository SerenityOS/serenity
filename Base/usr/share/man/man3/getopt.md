## Name

getopt - parse command-line options

## Synopsis

```**c++
#include <getopt.h>

extern int opterr;
extern int optopt;
extern int optind;
extern int optreset;
extern char* optarg;

struct option {
    const char* name;
    int has_arg;
    int* flag;
    int val;
};

int getopt(int argc, char** argv, const char* short_options);
int getopt_long(int argc, char** argv, const char* short_options, const struct option* long_options, int* out_long_option_index);
```

## Description

`getopt()` and `getopt_long()` parse options according to the syntax specified
in [`getopt`(5)](help://man/5/getopt). `getopt()` only supports short options;
`getopt_long()` supports both short and long options.

One invocation of either function extracts at most one option from command line
arguments, which are passed to it as the `argc`/`argv` pair, starting from
argument at index `optind`, which is initially set to 1 at program startup.

The `short_options` string should specify the short options to be recognized, as
single characters. If a short option requires a value, it is to be followed by a
colon character (`:`); if a short option optionally accepts a value, it is to be
followed by a double colon (`::`). If the first character in the `short_options`
is `+`, `getopt()` and `getopt_long()` won't look for further options once first
non-option argument is encountered.

`getopt_long()` additionally accepts an array of values describing long options
to be recognized. To specify whether a long option has a value, the `has_arg`
member of `struct option` must be set to one of the following predefined macro
values:

-   `no_argument`, if no value is accepted;
-   `required_argument`, if a value is optionally accepted;
-   `optional_argument`, if a value is optionally accepted.

If an option is parsed successfully, `getopt()` and `getopt_long()`
automatically increase the `optind` variable to point to the next command-line
argument to be parsed. This makes it possible to invoke `getopt()` or
`getopt_long()` in a loop unless they indicate either an error or the end of
options, and then treat the remaining command-line arguments, starting from the
one pointed to be `optind`, as non-option argument.

Unless `+` is specified as the first character of `short_options`, `getopt()`
and `getopt_long()` automatically reorder elements of `argv` to put options and
their values before any non-option arguments.

If, after having used `getopt()` or `getopt_long()` to parse a set of
command-line arguments, the program intends to use the `getopt()` or
`getopt_long()` to parse a different set of command-line arguments, it must ask
`getopt()` and `getopt_long()` to reset the internal state that they keep across
calls to handle some tricky cases. To do so, the program must either set the
`optreset` variable to a non-zero value, or set `optind` variable to 0. Doing
either of these things will reset the internal state, and option parsing will
start from command-line argument at index 1 in either case.

## Return value

If no option has been found, `getopt()` and `getopt_long()` return -1.

In case some invalid configuration of options and their values are passed in
`argv`, `getopt()` and `getopt_long()` return the `'?'` character. If the error
is related to a short option, the variable `optopt` is set to the option
character. If the variable `opterr` has a non-zero value (as it does by
default), an appropriate error message is printed to the standard error stream.

If a short option has been successfully parsed, `getopt()` and `getopt_long()`
return its character. Its value, if any, is assigned to the `optarg` variable.
If the option has been given no value, `optarg` is set to `nullptr`.

If a long option has been successfully parsed, `getopt_long()` return value
depends on the value of the `flag` pointer for that option. If `flag` is
`nullptr`, `getopt_long()` returns the value of `val` for that option.
Otherwise, the pointee of `flag` is set to `val`, and `getopt_long()` returns 0.
In either case, the index of the long option in the `long_options` array is
stored to the pointee of `out_long_option_index`, if it's not a `nullptr`. Same
as for short options, the `optarg` variable is set to point to the value of the
option, or to `nullptr` is none has been given.

## Examples

```c++
#include <getopt.h>

int verbose = 0;
const char* pad = nullptr;
const char* source = nullptr;

while (true) {
    // Accept short options: -h, -l, -s value, -p [value], -N.
    const char* short_options = "hls:p::N";
    // Accept long options: --pad [value], --verbose.
    const option long_options[] {
        { "pad", optional_argument, nullptr, 'p' },
        { "verbose", no_argument, &verbose, 1 },
    };
    int opt = getopt_long(argc, argv, short_options, long_options, nullptr);
    switch (opt) {
    case -1:
        // No more options.
        return true;
    case '?':
        // Some error; getopt() has already printed an error message.
        exit(1);
    case 'h':
        // Handle the -h option...
        break;
    case 'l':
        // Handle the -l option...
        break;
    case 's':
        // Handle the -s option.
        source = optarg;
        break;
    case 'p':
        // Handle the -p short option or the --pad long option.
        if (optarg)
            pad = optarg;
        else
            pad = ' ';
        break;
    case 'N':
        // Handle the -N option.
        break;
    case 0:
        // A long option (--verbose) has been parsed, but its
        // effect was setting the verbose variable to 1.
        break;
    }
}

const char* file_name = argv[optind];
```

## See also

-   [`getopt`(5)](help://man/5/getopt)
