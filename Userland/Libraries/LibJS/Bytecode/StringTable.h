/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/DistinctNumeric.h>
#include <YAK/String.h>
#include <YAK/Vector.h>

namespace JS::Bytecode {

TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, false, true, false, false, false, false, StringTableIndex);

class StringTable {
    YAK_MAKE_NONMOVABLE(StringTable);
    YAK_MAKE_NONCOPYABLE(StringTable);

public:
    StringTable() = default;

    StringTableIndex insert(StringView string);
    String const& get(StringTableIndex) const;
    void dump() const;
    bool is_empty() const { return m_strings.is_empty(); }

private:
    Vector<String> m_strings;
};

}
