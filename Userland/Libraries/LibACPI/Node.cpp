/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/RefPtr.h>
#include <LibACPI/NameString.h>
#include <LibACPI/Node.h>

#ifdef KERNEL
#    define warnln dbgln
#endif

namespace ACPI::AML {

ErrorOr<NonnullRefPtr<Node>> Node::find_child(NameSegment name) const
{
    for (auto const& child : m_children) {
        if (child->name() == name)
            return child;
    }

    warnln("ACPI: Node::find_child: Child {} does not exist relative to node {}!", name.to_string_view(), m_name.to_string_view());
    return Error::from_string_view_or_print_error_and_return_errno("Child does not exist."sv, EINVAL);
}

ErrorOr<NonnullRefPtr<Node>> Node::find_child(StringView name) const
{
    if (name.length() != 4)
        return Error::from_string_view_or_print_error_and_return_errno("Name must have a length of 4."sv, EINVAL);

    return find_child(TRY(NameSegment::from_string_view(name)));
}

ErrorOr<void> Node::insert_child(NameSegment name, NonnullRefPtr<Node> node)
{
    /// FIXME: Duplicate name detection.
    node->m_name = name;
    node->m_parent = this;
    TRY(m_children.try_append(node));
    return {};
}

ErrorOr<void> Node::insert_child(StringView name, NonnullRefPtr<Node> node)
{
    if (name.length() != 4)
        return Error::from_string_view_or_print_error_and_return_errno("Name must have a length of 4."sv, EINVAL);

    return insert_child(TRY(NameSegment::from_string_view(name)), node);
}

ErrorOr<void> Node::write_description(StringBuilder& builder)
{
    TRY(builder.try_append(kind()));
    return {};
}

ErrorOr<void> NameNode::write_description(StringBuilder& builder)
{
    if (m_data.is<u64>()) {
        auto value = MUST(m_data.as<u64>());
        TRY(builder.try_appendff("(Decimal: {}, Hexadecimal: {:X})", value, value));
    } else if (m_data.is<NameString>()) {
        auto value = MUST(m_data.as<NameString>());
        TRY(builder.try_appendff("{}"sv, TRY(value.to_string())));
    } else {
        TRY(builder.try_append("Invalid"sv));
    }

    return {};
}

ErrorOr<void> OperationRegionNode::write_description(StringBuilder& builder)
{
    TRY(builder.try_appendff("OperationRegion(Space: {}, Offset: {}, Length: {})", m_space_raw, m_offset, m_length));
    return {};
}

ErrorOr<void> FieldNode::write_description(StringBuilder& builder)
{
    TRY(builder.try_appendff("Field(Flags: {}, Offset: {}, Size: {})", m_flags, m_offset, m_size));
    return {};
}

}
