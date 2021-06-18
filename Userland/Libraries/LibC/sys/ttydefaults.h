/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define TTYDEF_IFLAG (ICRNL | IGNBRK | IGNPAR | IUTF8)
#define TTYDEF_OFLAG (OPOST | ONLCR)
#define TTYDEF_LFLAG_NOECHO (ISIG | ICANON | IEXTEN)
#define TTYDEF_LFLAG_ECHO (TTYDEF_LFLAG_NOECHO | ECHO | ECHOE | ECHOKE | ECHONL)
#define TTYDEF_LFLAG TTYDEF_LFLAG_ECHO
#define TTYDEF_CFLAG (CS8 | CREAD)
#define TTYDEF_SPEED (B9600)

#define CTRL(c) (c & 0x1F)
#define CINTR CTRL('c')
#define CQUIT 034
#define CERASE 010
#define CKILL CTRL('u')
#define CEOF CTRL('d')
#define CTIME 0
#define CMIN 1
#define CSWTC 0
#define CSTART CTRL('q')
#define CSTOP CTRL('s')
#define CSUSP CTRL('z')
#define CEOL 0
#define CREPRINT CTRL('r')
#define CDISCARD CTRL('o')
#define CWERASE CTRL('w')
#define CLNEXT CTRL('v')
#define CEOL2 CEOL

#define CEOT CEOF
#define CBRK CEOL
#define CRPRNT CREPRINT
#define CFLUSH CDISCARD

#ifdef TTYDEFCHARS
#    ifdef KERNEL
#        include <Kernel/UnixTypes.h>
#    else
#        include <termios.h>
#    endif
#    include <sys/cdefs.h>

#    define TTYDEF_CC          \
        [VINTR] = CINTR,       \
        [VQUIT] = CQUIT,       \
        [VERASE] = CERASE,     \
        [VKILL] = CKILL,       \
        [VEOF] = CEOF,         \
        [VTIME] = CTIME,       \
        [VMIN] = CMIN,         \
        [VSWTC] = CSWTC,       \
        [VSTART] = CSTART,     \
        [VSTOP] = CSTOP,       \
        [VSUSP] = CSUSP,       \
        [VEOL] = CEOL,         \
        [VREPRINT] = CREPRINT, \
        [VDISCARD] = CDISCARD, \
        [VWERASE] = CWERASE,   \
        [VLNEXT] = CLNEXT,     \
        [VEOL2] = CEOL2

__BEGIN_DECLS
static const cc_t ttydefchars[NCCS] = {
    TTYDEF_CC
};
__END_DECLS
#endif
