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
    // FIXME: Implement this using FEAT_PAN.
    //        Once implemented, make sure to use PageFault::set_was_smap_disabled()
    //        appropriately in the page fault handler.
}

SmapDisabler::~SmapDisabler() = default;

}
