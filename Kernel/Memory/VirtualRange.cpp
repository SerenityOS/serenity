/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/VirtualRange.h>

namespace Kernel::Memory {

Vector<VirtualRange, 2> VirtualRange::carve(VirtualRange const& taken) const
{
    VERIFY((taken.size() % PAGE_SIZE) == 0);

    Vector<VirtualRange, 2> parts;
    if (taken == *this)
        return {};
    if (taken.base() > base())
        parts.append({ base(), taken.base().get() - base().get() });
    if (taken.end() < end())
        parts.append({ taken.end(), end().get() - taken.end().get() });
    return parts;
}

bool VirtualRange::intersects(VirtualRange const& other) const
{
    auto a = *this;
    auto b = other;

    if (a.base() > b.base())
        swap(a, b);

    return a.base() < b.end() && b.base() < a.end();
}

VirtualRange VirtualRange::intersect(VirtualRange const& other) const
{
    if (*this == other) {
        return *this;
    }
    auto new_base = max(base(), other.base());
    auto new_end = min(end(), other.end());
    VERIFY(new_base < new_end);
    return VirtualRange(new_base, (new_end - new_base).get());
}

ErrorOr<VirtualRange> VirtualRange::expand_to_page_boundaries(FlatPtr address, size_t size)
{
    if ((address + size) < address)
        return EINVAL;

    auto base = VirtualAddress { address }.page_base();
    auto end = TRY(page_round_up(address + size));
    return VirtualRange { base, end - base.get() };
}

}
