/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/IdentifierTable.h>

namespace JS::Bytecode {

IdentifierTableIndex IdentifierTable::insert(FlyString string)
{
    for (size_t i = 0; i < m_identifiers.size(); i++) {
        if (m_identifiers[i] == string)
            return i;
    }
    m_identifiers.append(move(string));
    return m_identifiers.size() - 1;
}

FlyString const& IdentifierTable::get(IdentifierTableIndex index) const
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
