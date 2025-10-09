/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/archctl_numbers.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

int archctl(int option, ...);

__END_DECLS
