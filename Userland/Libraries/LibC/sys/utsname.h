/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/utsname.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

int uname(struct utsname*);

__END_DECLS
