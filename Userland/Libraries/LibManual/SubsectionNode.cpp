/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SubsectionNode.h"

namespace Manual {

SubsectionNode::SubsectionNode(NonnullRefPtr<SectionNode> parent, StringView name)
    : SectionNode(name, name)
    , m_parent(move(parent))
{
}

Node const* SubsectionNode::parent() const { return m_parent; }

ErrorOr<String> SubsectionNode::name() const { return m_name; }

ErrorOr<String> SubsectionNode::path() const
{
    return String::formatted("{}/{}", TRY(m_parent->path()), m_section);
}

}
