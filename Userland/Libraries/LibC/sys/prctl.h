/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/prctl_numbers.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

int prctl(int option, ...);

__END_DECLS
