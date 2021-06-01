/*
 * Copyright (c) 2021, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <nl_types.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

enum {
    CODESET,
};

char* nl_langinfo(nl_item);

__END_DECLS
