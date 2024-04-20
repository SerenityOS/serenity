/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
#include <link.h>

using DlIteratePhdrCallbackFunction = int (*)(struct dl_phdr_info*, size_t, void*);
[[gnu::weak]] extern int __dl_iterate_phdr(DlIteratePhdrCallbackFunction, void*) asm("__dl_iterate_phdr");

extern "C" int dl_iterate_phdr(int (*callback)(struct dl_phdr_info* info, size_t size, void* data), void* data)
{
    return __dl_iterate_phdr(callback, data);
}
