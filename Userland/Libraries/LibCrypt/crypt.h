/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

/* standard symbolic constants and types
 *
 * values from POSIX standard unix specification
 *
 * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/unistd.h.html
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

struct crypt_data {
    int initialized;
    char result[65];
};

char* crypt(char const* key, char const* salt);
char* crypt_r(char const* key, char const* salt, struct crypt_data* data);

__END_DECLS
