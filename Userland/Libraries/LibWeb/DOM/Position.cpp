/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/Position.h>

namespace Web::DOM {

Position::Position(Node& node, unsigned offset)
    : m_node(node)
    , m_offset(offset)
{
}

Position::~Position()
{
}

String Position::to_string() const
{
    if (!node())
        return String::formatted("DOM::Position(nullptr, {})", offset());
    return String::formatted("DOM::Position({} ({})), {})", node()->node_name(), node(), offset());
}

}
