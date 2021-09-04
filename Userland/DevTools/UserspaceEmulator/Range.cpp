/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Range.h"
#include <AK/Vector.h>

namespace UserspaceEmulator {

Vector<Range, 2> Range::carve(Range const& taken) const
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

}
