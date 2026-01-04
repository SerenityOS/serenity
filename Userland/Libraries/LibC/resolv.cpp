/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <resolv.h>

extern "C" {

int res_query([[maybe_unused]] char const* dname, [[maybe_unused]] int class_, [[maybe_unused]] int type, [[maybe_unused]] unsigned char* answer, [[maybe_unused]] int anslen)
{
    // FIXME: Implement res_query()
    return -1;
}
}
