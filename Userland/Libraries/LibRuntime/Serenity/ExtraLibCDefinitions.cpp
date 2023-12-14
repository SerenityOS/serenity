/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/POSIX/signal.h>

extern "C" {
// Needed by strsignal
char const* sys_siglist[NSIG] = {
#define DESCRIPTION(name, description) description,
    __ENUMERATE_SIGNALS(DESCRIPTION)
#undef __ENUMERATE_SIGNAL
};
}
