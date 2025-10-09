/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibELF/Arch/tls.h>

namespace ELF {

void set_thread_pointer_register(FlatPtr value)
{
    asm volatile("msr tpidr_el0, %0" ::"r"(value));
}

}
