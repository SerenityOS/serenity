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
public:
    Reference(u32 index, u32 generation_index)
        : m_ref_index(index)
        , m_generation_index(generation_index)
    {
    }

    [[nodiscard]] ALWAYS_INLINE u32 as_ref_index() const
    {
        return m_ref_index;
    }

    [[nodiscard]] ALWAYS_INLINE u32 as_ref_generation_index() const
    {
        return m_generation_index;
    }

private:
    u32 m_ref_index;
    u32 m_generation_index;
};

}
