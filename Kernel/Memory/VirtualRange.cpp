/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/VirtualRange.h>

namespace Kernel::Memory {

ErrorOr<VirtualRange> VirtualRange::expand_to_page_boundaries(FlatPtr address, size_t size)
{
    if ((address + size) < address)
        return EINVAL;

    auto base = VirtualAddress { address }.page_base();
    auto end = TRY(page_round_up(address + size));
    return VirtualRange { base, end - base.get() };
}

}
