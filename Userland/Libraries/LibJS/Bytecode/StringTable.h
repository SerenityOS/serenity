/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/DistinctNumeric.h>
#include <AK/Vector.h>

namespace JS::Bytecode {

AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, StringTableIndex, Comparison);

class StringTable {
    AK_MAKE_NONMOVABLE(StringTable);
    AK_MAKE_NONCOPYABLE(StringTable);

public:
    StringTable() = default;

    StringTableIndex insert(DeprecatedString);
    DeprecatedString const& get(StringTableIndex) const;
    void dump() const;
    bool is_empty() const { return m_strings.is_empty(); }

private:
    Vector<DeprecatedString> m_strings;
};

}
