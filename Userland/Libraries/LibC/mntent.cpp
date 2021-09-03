/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Format.h>
#include <assert.h>
#include <mntent.h>

extern "C" {

struct mntent* getmntent(FILE*)
{
    dbgln("FIXME: Implement getmntent()");
    TODO();
    return nullptr;
}
}
