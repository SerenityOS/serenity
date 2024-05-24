/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/kmalloc.h>
#include <Kernel/Prekernel/Runtime.h>

void kfree_sized(void*, size_t)
{
    halt();
}

void* kmalloc(size_t)
{
    halt();
}

size_t kmalloc_good_size(size_t)
{
    halt();
}
