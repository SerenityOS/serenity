# Patches for ncurses on SerenityOS

## `0001-Teach-configure-about-serenity.patch`

Teach configure about serenity


## `0002-Disable-mixed-case-directory-names-when-building-on-.patch`

Disable mixed-case directory names when building on macOS

Since macOS's filesystem is case-insensitive, its `tic` only generates
terminfo directory names that are hex numbers instead of letters, such
as 78/xterm instead of x/xterm. However, the configure script still
enables the mixed-case directory name feature by default. As a result,
ncurses will fail when trying to find terminfo entries like x/xterm if
they are generated on macOS.

It seems like there is no way to change the behavior of `tic` to create
alphabetical directories, so we can only disable this option explicitly.

