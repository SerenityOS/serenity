/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/Arch/aarch64/PrivilegedAccessNever.h>

namespace Kernel {
SmapDisabler::SmapDisabler()
    : m_flags(Aarch64::Asm::is_pan_set())
{
    Aarch64::Asm::clear_pan();
}

SmapDisabler::~SmapDisabler()
{
    if (m_flags)
        Aarch64::Asm::set_pan();
}

}
