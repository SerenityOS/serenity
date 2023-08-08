/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/VirtualAddress.h>
#include <Kernel/Memory/VirtualRange.h>
#include <Kernel/Sections.h>

namespace Kernel::Memory {

inline bool is_user_address(VirtualAddress vaddr)
{
    return vaddr.get() < USER_RANGE_CEILING;
}

inline bool is_user_range(VirtualAddress vaddr, size_t size)
{
    if (vaddr.offset(size) < vaddr)
        return false;
    if (!is_user_address(vaddr))
        return false;
    if (size <= 1)
        return true;
    return is_user_address(vaddr.offset(size - 1));
}

inline bool is_user_range(VirtualRange const& range)
{
    return is_user_range(range.base(), range.size());
}

}
