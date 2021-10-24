/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/FlyString.h>
#include <AK/Vector.h>

namespace JS::Bytecode {

TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, false, true, false, false, false, false, IdentifierTableIndex);

class IdentifierTable {
    AK_MAKE_NONMOVABLE(IdentifierTable);
    AK_MAKE_NONCOPYABLE(IdentifierTable);

public:
    IdentifierTable() = default;

    IdentifierTableIndex insert(FlyString);
    FlyString const& get(IdentifierTableIndex) const;
    void dump() const;
    bool is_empty() const { return m_identifiers.is_empty(); }

private:
    Vector<FlyString> m_identifiers;
};

}
