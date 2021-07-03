/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

struct winsize {
    unsigned short ws_row;
    unsigned short ws_col;
    unsigned short ws_xpixel;
    unsigned short ws_ypixel;
};

struct FBResolution {
    unsigned pitch;
    unsigned width;
    unsigned height;
};

struct FBRect {
    unsigned x;
    unsigned y;
    unsigned width;
    unsigned height;
};

struct FBFlushRects {
    int buffer_index;
    unsigned count;
    struct FBRect const* rects;
};

__END_DECLS

enum IOCtlNumber {
    TIOCGPGRP,
    TIOCSPGRP,
    TCGETS,
    TCSETS,
    TCSETSW,
    TCSETSF,
    TCFLSH,
    TIOCGWINSZ,
    TIOCSCTTY,
    TIOCSTI,
    TIOCNOTTY,
    TIOCSWINSZ,
    FB_IOCTL_GET_SIZE_IN_BYTES,
    FB_IOCTL_GET_RESOLUTION,
    FB_IOCTL_SET_RESOLUTION,
    FB_IOCTL_GET_BUFFER,
    FB_IOCTL_SET_BUFFER,
    FB_IOCTL_FLUSH_BUFFERS,
    SIOCSIFADDR,
    SIOCGIFADDR,
    SIOCGIFHWADDR,
    SIOCGIFNETMASK,
    SIOCSIFNETMASK,
    SIOCGIFBRDADDR,
    SIOCGIFMTU,
    SIOCGIFFLAGS,
    SIOCGIFCONF,
    SIOCADDRT,
    SIOCDELRT,
    FIBMAP,
    FIONBIO,
};

#define TIOCGPGRP TIOCGPGRP
#define TIOCSPGRP TIOCSPGRP
#define TCGETS TCGETS
#define TCSETS TCSETS
#define TCSETSW TCSETSW
#define TCSETSF TCSETSF
#define TCFLSH TCFLSH
#define TIOCGWINSZ TIOCGWINSZ
#define TIOCSCTTY TIOCSCTTY
#define TIOCSTI TIOCSTI
#define TIOCNOTTY TIOCNOTTY
#define TIOCSWINSZ TIOCSWINSZ
#define FB_IOCTL_GET_SIZE_IN_BYTES FB_IOCTL_GET_SIZE_IN_BYTES
#define FB_IOCTL_GET_RESOLUTION FB_IOCTL_GET_RESOLUTION
#define FB_IOCTL_SET_RESOLUTION FB_IOCTL_SET_RESOLUTION
#define FB_IOCTL_GET_BUFFER FB_IOCTL_GET_BUFFER
#define FB_IOCTL_SET_BUFFER FB_IOCTL_SET_BUFFER
#define FB_IOCTL_FLUSH_BUFFERS FB_IOCTL_FLUSH_BUFFERS
#define SIOCSIFADDR SIOCSIFADDR
#define SIOCGIFADDR SIOCGIFADDR
#define SIOCGIFHWADDR SIOCGIFHWADDR
#define SIOCGIFNETMASK SIOCGIFNETMASK
#define SIOCSIFNETMASK SIOCSIFNETMASK
#define SIOCGIFBRDADDR SIOCGIFBRDADDR
#define SIOCGIFMTU SIOCGIFMTU
#define SIOCGIFFLAGS SIOCGIFFLAGS
#define SIOCGIFCONF SIOCGIFCONF
#define SIOCADDRT SIOCADDRT
#define SIOCDELRT SIOCDELRT
#define FIBMAP FIBMAP
#define FIONBIO FIONBIO
