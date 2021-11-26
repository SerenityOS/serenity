/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace PDF {

class Reference {
    // We store refs as u32, with 18 bits for the index and 14 bits for the
    // generation index. The generation index is stored in the higher bits.
    // This may need to be rethought later, as the max generation index is
    // 2^16 and the max for the object index is probably 2^32 (I don't know
    // exactly)
    static constexpr auto MAX_REF_INDEX = (1 << 19) - 1;            // 2 ^ 19 - 1
    static constexpr auto MAX_REF_GENERATION_INDEX = (1 << 15) - 1; // 2 ^ 15 - 1

public:
    Reference(u32 index, u32 generation_index)
    {
        VERIFY(index < MAX_REF_INDEX);
        VERIFY(generation_index < MAX_REF_GENERATION_INDEX);
        m_combined = (generation_index << 14) | index;
    }

    [[nodiscard]] ALWAYS_INLINE u32 as_ref_index() const
    {
        return m_combined & 0x3ffff;
    }

    [[nodiscard]] ALWAYS_INLINE u32 as_ref_generation_index() const
    {
        return m_combined >> 18;
    }

private:
    u32 m_combined;
};

}
