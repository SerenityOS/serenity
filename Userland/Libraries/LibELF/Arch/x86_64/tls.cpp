/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibELF/Arch/tls.h>
#include <sys/archctl.h>

namespace ELF {

void set_thread_pointer_register(FlatPtr)
{
    TODO();
}

}
