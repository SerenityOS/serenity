/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibACPI/Nodes.h>

namespace ACPI::AML {

ErrorOr<void> NameNode::write_description(StringBuilder& builder)
{
    TRY(builder.try_appendff("{}"sv, m_data));
    return {};
}

ErrorOr<void> MethodNode::write_description(StringBuilder& builder)
{
    TRY(builder.try_appendff("Method(Table: {}, Arguments: {}, Start: {}, End: {}, Flags: {})", m_table_index, argument_count(), m_start, m_end, bit_cast<u8>(m_flags)));
    return {};
}

ErrorOr<void> OperationRegionNode::write_description(StringBuilder& builder)
{
    TRY(builder.try_appendff("OperationRegion(Space: {}, Offset: {}, Length: {})", m_space_raw, m_offset, m_length));
    return {};
}

ErrorOr<void> FieldNode::write_description(StringBuilder& builder)
{
    TRY(builder.try_appendff("Field(Flags: {}, Offset: {}, Length: {})", m_flags, m_offset, m_length));
    return {};
}

}
