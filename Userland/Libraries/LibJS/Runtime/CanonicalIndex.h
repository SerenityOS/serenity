/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

class CanonicalIndex {
public:
    enum class Type {
        Index,
        Numeric,
        Undefined,
    };

    CanonicalIndex(Type type, u32 index)
        : m_type(type)
        , m_index(index)
    {
    }

    u32 as_index() const
    {
        VERIFY(is_index());
        return m_index;
    }

    bool is_index() const { return m_type == Type::Index; }
    bool is_undefined() const { return m_type == Type::Undefined; }

private:
    Type m_type;
    u32 m_index;
};
