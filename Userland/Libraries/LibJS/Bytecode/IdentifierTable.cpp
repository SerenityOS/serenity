/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/IdentifierTable.h>

namespace JS::Bytecode {

IdentifierTableIndex IdentifierTable::insert(DeprecatedFlyString string)
{
    m_identifiers.append(move(string));
    return m_identifiers.size() - 1;
}

DeprecatedFlyString const& IdentifierTable::get(IdentifierTableIndex index) const
{
    return m_identifiers[index.value()];
}

void IdentifierTable::dump() const
{
    outln("Identifier Table:");
    for (size_t i = 0; i < m_identifiers.size(); i++)
        outln("{}: {}", i, m_identifiers[i]);
}

}
