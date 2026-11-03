/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

/* https://pubs.opengroup.org/onlinepubs/9799919799/basedefs/termios.h.html#tag_14_77_03_07 */
struct winsize {
    unsigned short ws_row;
    unsigned short ws_col;
    unsigned short ws_xpixel;
    unsigned short ws_ypixel;
};

#ifdef __cplusplus
}
#endif
