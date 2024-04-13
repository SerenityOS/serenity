/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibELF/Arch/tls.h>

namespace ELF {

void set_thread_pointer_register(FlatPtr value)
{
    asm volatile("mv tp, %0" ::"r"(value));
}

}
