/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#define LOCK_SH (1 << 0)
#define LOCK_EX (1 << 1)
#define LOCK_NB (1 << 2)
#define LOCK_UN (1 << 3)

int flock(int fd, int operation);

__END_DECLS
