/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/prctl_numbers.h>
#include <sys/types.h>

__BEGIN_DECLS

int prctl(int option, uintptr_t arg1, uintptr_t arg2);

__END_DECLS
