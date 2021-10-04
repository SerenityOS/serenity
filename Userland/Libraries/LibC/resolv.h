/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/types.h>

__BEGIN_DECLS

int res_query(const char* dname, int class_, int type, unsigned char* answer, int anslen);

__END_DECLS
