/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/DistinctNumeric.h>
#include <AK/Vector.h>

namespace JS::Bytecode {

AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, IdentifierTableIndex, Comparison);

class IdentifierTable {
    AK_MAKE_NONMOVABLE(IdentifierTable);
    AK_MAKE_NONCOPYABLE(IdentifierTable);

public:
    IdentifierTable() = default;

    IdentifierTableIndex insert(DeprecatedFlyString);
    DeprecatedFlyString const& get(IdentifierTableIndex) const;
    void dump() const;
    bool is_empty() const { return m_identifiers.is_empty(); }

private:
    Vector<DeprecatedFlyString> m_identifiers;
};

}
