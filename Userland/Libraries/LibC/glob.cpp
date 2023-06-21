/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <assert.h>
#include <glob.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/glob.html
int glob(char const*, int, int (*)(char const* epath, int eerrno), glob_t*)
{
    dbgln("FIXME: Implement glob()");
    TODO();
}

void globfree(glob_t*)
{
    dbgln("FIXME: Implement globfree()");
    TODO();
}
}
