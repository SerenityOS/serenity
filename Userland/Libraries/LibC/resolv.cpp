/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <resolv.h>

extern "C" {

int res_query(char const*, int, int, unsigned char*, int)
{
    dbgln("FIXME: Implement res_query()");
    return 0;
}
}
