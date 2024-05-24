/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Runtime.h>

void halt()
{
    asm volatile("hlt");
    __builtin_unreachable();
}
