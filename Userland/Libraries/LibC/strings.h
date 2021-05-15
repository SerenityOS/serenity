/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

int strcasecmp(const char*, const char*);
int strncasecmp(const char*, const char*, size_t);
void bzero(void*, size_t);
void bcopy(const void*, void*, size_t);

__END_DECLS
