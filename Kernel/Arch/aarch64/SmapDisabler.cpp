/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SmapDisabler.h>

namespace Kernel {

SmapDisabler::SmapDisabler()
    : m_flags(0)
{
}

SmapDisabler::~SmapDisabler() = default;

}
