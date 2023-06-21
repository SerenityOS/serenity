/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/ttydefaults.h>

#ifdef TTYDEFCHARS
#    include <termios.h>

#    include <Kernel/API/ttydefaultschars.h>
#endif
