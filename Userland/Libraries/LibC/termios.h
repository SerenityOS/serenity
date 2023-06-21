/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/termios.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

int tcdrain(int fd);
int tcgetattr(int fd, struct termios*);
int tcsetattr(int fd, int optional_actions, const struct termios*);
int tcsendbreak(int fd, int duration);
int tcflow(int fd, int action);
int tcflush(int fd, int queue_selector);

speed_t cfgetispeed(const struct termios*);
speed_t cfgetospeed(const struct termios*);
int cfsetispeed(struct termios*, speed_t);
int cfsetospeed(struct termios*, speed_t);
void cfmakeraw(struct termios*);

__END_DECLS
