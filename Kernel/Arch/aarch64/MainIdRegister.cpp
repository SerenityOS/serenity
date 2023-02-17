/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/MainIdRegister.h>

namespace Kernel {

MainIdRegister::MainIdRegister()
{
    unsigned int mrs;
    asm volatile("mrs %x0, MIDR_EL1"
                 : "=r"(mrs));
    m_value = mrs;
}

}
