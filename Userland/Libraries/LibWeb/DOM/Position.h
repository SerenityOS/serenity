/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

class Position {
public:
    Position() = default;
    Position(Node&, unsigned offset);

    bool is_valid() const { return m_node.ptr(); }

    Node* node() { return m_node.cell(); }
    Node const* node() const { return m_node.cell(); }

    unsigned offset() const { return m_offset; }
    bool offset_is_at_end_of_node() const;
    void set_offset(unsigned value) { m_offset = value; }
    bool increment_offset();
    bool decrement_offset();

    bool operator==(Position const& other) const
    {
        return m_node.ptr() == other.m_node.ptr() && m_offset == other.m_offset;
    }

    bool operator!=(Position const& other) const
    {
        return !(*this == other);
    }

    String to_string() const;

private:
    JS::Handle<Node> m_node;
    unsigned m_offset { 0 };
};

}

namespace AK {
template<>
struct Formatter<Web::DOM::Position> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::DOM::Position const& value)
    {
        return Formatter<StringView>::format(builder, value.to_string());
    }
};

}
