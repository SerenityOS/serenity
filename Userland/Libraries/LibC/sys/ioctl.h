/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/Ioctl.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

int ioctl(int fd, unsigned request, ...);

__END_DECLS
