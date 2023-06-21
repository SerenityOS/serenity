/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Forward.h>

namespace Kernel {

class ScopedAddressSpaceSwitcher {
public:
    explicit ScopedAddressSpaceSwitcher(Process&);
    ~ScopedAddressSpaceSwitcher();

private:
    LockRefPtr<Memory::PageDirectory> m_previous_page_directory;
};

}
