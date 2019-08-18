#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

struct winsize {
    unsigned short ws_row;
    unsigned short ws_col;
};

__END_DECLS

enum IOCtlNumber {
    TIOCGPGRP,
    TIOCSPGRP,
    TCGETS,
    TCSETS,
    TCSETSW,
    TCSETSF,
    TIOCGWINSZ,
    TIOCSCTTY,
    TIOCNOTTY,
    TIOCSWINSZ,
};
