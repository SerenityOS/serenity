/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Edwin Hoksberg <mail@edwinhoksberg.nl>
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

struct FBProperties {
    unsigned char multihead_support;
    unsigned char doublebuffer_support;
    unsigned char flushing_support;
    unsigned char partial_flushing_support;
};

struct FBHeadProperties {
    int head_index;

    unsigned pitch;
    unsigned width;
    unsigned height;

    unsigned offset;
    unsigned buffer_length;
};

struct FBHeadResolution {
    int head_index;
    int pitch;
    int width;
    int height;
};

struct FBHeadEDID {
    int head_index;

    unsigned char* bytes;
    unsigned bytes_size;
};

struct FBHeadVerticalOffset {
    int head_index;
    int offsetted;
};

struct FBRect {
    int head_index;
    unsigned x;
    unsigned y;
    unsigned width;
    unsigned height;
};

struct FBBufferOffset {
    int buffer_index;
    unsigned offset;
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
    FB_IOCTL_GET_PROPERTIES,
    FB_IOCTL_GET_HEAD_PROPERTIES,
    FB_IOCTL_SET_HEAD_RESOLUTION,
    FB_IOCTL_GET_HEAD_EDID,
    FB_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER,
    FB_IOCTL_GET_HEAD_VERTICAL_OFFSET_BUFFER,
    FB_IOCTL_FLUSH_HEAD_BUFFERS,
    FB_IOCTL_FLUSH_HEAD,
    KEYBOARD_IOCTL_GET_NUM_LOCK,
    KEYBOARD_IOCTL_SET_NUM_LOCK,
    KEYBOARD_IOCTL_GET_CAPS_LOCK,
    KEYBOARD_IOCTL_SET_CAPS_LOCK,
    SIOCATMARK,
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
    SIOCSARP,
    SIOCDARP,
    FIBMAP,
    FIONBIO,
    FIONREAD,
    KCOV_SETBUFSIZE,
    KCOV_ENABLE,
    KCOV_DISABLE,
    SOUNDCARD_IOCTL_SET_SAMPLE_RATE,
    SOUNDCARD_IOCTL_GET_SAMPLE_RATE,
    STORAGE_DEVICE_GET_SIZE,
    STORAGE_DEVICE_GET_BLOCK_SIZE,
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
#define FB_IOCTL_GET_PROPERTIES FB_IOCTL_GET_PROPERTIES
#define FB_IOCTL_GET_HEAD_PROPERTIES FB_IOCTL_GET_HEAD_PROPERTIES
#define FB_IOCTL_GET_HEAD_EDID FB_IOCTL_GET_HEAD_EDID
#define FB_IOCTL_SET_HEAD_RESOLUTION FB_IOCTL_SET_HEAD_RESOLUTION
#define FB_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER FB_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER
#define FB_IOCTL_GET_HEAD_VERTICAL_OFFSET_BUFFER FB_IOCTL_GET_HEAD_VERTICAL_OFFSET_BUFFER
#define FB_IOCTL_FLUSH_HEAD_BUFFERS FB_IOCTL_FLUSH_HEAD_BUFFERS
#define FB_IOCTL_FLUSH_HEAD FB_IOCTL_FLUSH_HEAD
#define KEYBOARD_IOCTL_GET_NUM_LOCK KEYBOARD_IOCTL_GET_NUM_LOCK
#define KEYBOARD_IOCTL_SET_NUM_LOCK KEYBOARD_IOCTL_SET_NUM_LOCK
#define KEYBOARD_IOCTL_GET_CAPS_LOCK KEYBOARD_IOCTL_GET_CAPS_LOCK
#define KEYBOARD_IOCTL_SET_CAPS_LOCK KEYBOARD_IOCTL_SET_CAPS_LOCK
#define SIOCATMARK SIOCATMARK
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
#define SIOCSARP SIOCSARP
#define SIOCDARP SIOCDARP
#define FIBMAP FIBMAP
#define FIONBIO FIONBIO
#define FIONREAD FIONREAD
#define SOUNDCARD_IOCTL_SET_SAMPLE_RATE SOUNDCARD_IOCTL_SET_SAMPLE_RATE
#define SOUNDCARD_IOCTL_GET_SAMPLE_RATE SOUNDCARD_IOCTL_GET_SAMPLE_RATE
#define STORAGE_DEVICE_GET_SIZE STORAGE_DEVICE_GET_SIZE
#define STORAGE_DEVICE_GET_BLOCK_SIZE STORAGE_DEVICE_GET_BLOCK_SIZE
