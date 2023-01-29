/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

int strcasecmp(char const*, char const*);
int strncasecmp(char const*, char const*, size_t);
void bzero(void*, size_t);
void bcopy(void const*, void*, size_t);
int ffs(int);
int ffsl(long int);
int ffsll(long long int);

__END_DECLS
