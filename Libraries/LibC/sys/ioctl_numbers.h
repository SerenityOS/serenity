#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

struct winsize {
    unsigned short ws_row;
    unsigned short ws_col;
};

struct FBResolution {
    int pitch;
    int width;
    int height;
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
    FB_IOCTL_GET_SIZE_IN_BYTES,
    FB_IOCTL_GET_RESOLUTION,
    FB_IOCTL_SET_RESOLUTION,
    FB_IOCTL_GET_BUFFER,
    FB_IOCTL_SET_BUFFER,
    SIOCSIFADDR,
    SIOCGIFADDR,
    SIOCGIFHWADDR,
};
