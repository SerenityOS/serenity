/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/netinet/in.h>

__BEGIN_DECLS

in_addr_t inet_addr(char const*);

__END_DECLS
