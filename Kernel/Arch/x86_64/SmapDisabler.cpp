/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SmapDisabler.h>

#include <Kernel/Arch/x86_64/ASM_wrapper.h>

namespace Kernel {

SmapDisabler::SmapDisabler()
    : m_flags(cpu_flags())
{
    stac();
}

SmapDisabler::~SmapDisabler()
{
    if (!(m_flags & 0x40000))
        clac();
}

}
