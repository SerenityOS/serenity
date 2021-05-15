/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
#include <link.h>

extern "C" {

using DlIteratePhdrCallbackFunction = int (*)(struct dl_phdr_info*, size_t, void*);
using DlIteratePhdrFunction = int (*)(DlIteratePhdrCallbackFunction, void*);

DlIteratePhdrFunction __dl_iterate_phdr;

int dl_iterate_phdr(int (*callback)(struct dl_phdr_info* info, size_t size, void* data), void* data)
{
    return __dl_iterate_phdr(callback, data);
}
}
