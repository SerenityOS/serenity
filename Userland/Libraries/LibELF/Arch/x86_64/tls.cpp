/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibELF/Arch/tls.h>
#include <sys/archctl.h>

namespace ELF {

void set_thread_pointer_register(FlatPtr value)
{
    // TODO: Consider if we want to support the FSGSBASE extension: https://www.intel.com/content/www/us/en/developer/articles/technical/software-security-guidance/best-practices/guidance-enabling-fsgsbase.html
    VERIFY(archctl(ARCHCTL_X86_64_SET_FS_BASE_FOR_CURRENT_THREAD, value) == 0);
}

}
