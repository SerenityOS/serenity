/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Range.h>

namespace Kernel {

Vector<Range, 2> Range::carve(const Range& taken) const
{
    VERIFY((taken.size() % PAGE_SIZE) == 0);

    Vector<Range, 2> parts;
    if (taken == *this)
        return {};
    if (taken.base() > base())
        parts.append({ base(), taken.base().get() - base().get() });
    if (taken.end() < end())
        parts.append({ taken.end(), end().get() - taken.end().get() });
    return parts;
}
Range Range::intersect(const Range& other) const
{
    if (*this == other) {
        return *this;
    }
    auto new_base = max(base(), other.base());
    auto new_end = min(end(), other.end());
    VERIFY(new_base < new_end);
    return Range(new_base, (new_end - new_base).get());
}

KResultOr<Range> Range::expand_to_page_boundaries(FlatPtr address, size_t size)
{
    if (page_round_up_would_wrap(size))
        return EINVAL;

    if ((address + size) < address)
        return EINVAL;

    if (page_round_up_would_wrap(address + size))
        return EINVAL;

    auto base = VirtualAddress { address }.page_base();
    auto end = page_round_up(address + size);

    return Range { base, end - base.get() };
}

}
