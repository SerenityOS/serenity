/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SubsectionNode.h"
#include "PageNode.h"
#include <AK/TypeCasts.h>

namespace Manual {

SubsectionNode::SubsectionNode(NonnullRefPtr<SectionNode const> parent, StringView name, RefPtr<PageNode> page)
    : SectionNode(name, name)
    , m_parent(move(parent))
    , m_page(move(page))
{
}

Node const* SubsectionNode::parent() const { return m_parent; }

PageNode const* SubsectionNode::document() const { return m_page; }

ErrorOr<String> SubsectionNode::name() const { return m_name; }

ErrorOr<String> SubsectionNode::path() const
{
    return String::formatted("{}/{}", TRY(m_parent->path()), m_section);
}

}
