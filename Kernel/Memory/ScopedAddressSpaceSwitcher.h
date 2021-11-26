/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Forward.h>

namespace Kernel {

class ScopedAddressSpaceSwitcher {
public:
    explicit ScopedAddressSpaceSwitcher(Process&);
    ~ScopedAddressSpaceSwitcher();

private:
    u32 m_previous_cr3 { 0 };
};

}
