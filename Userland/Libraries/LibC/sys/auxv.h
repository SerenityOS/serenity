/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/auxv.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

long getauxval(long type);

__END_DECLS
