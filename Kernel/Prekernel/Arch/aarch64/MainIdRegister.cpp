/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Arch/aarch64/MainIdRegister.h>

namespace Prekernel {

MainIdRegister::MainIdRegister()
{
    unsigned int mrs;
    asm volatile("mrs %x0, MIDR_EL1"
                 : "=r"(mrs));
    m_value = mrs;
}

}
