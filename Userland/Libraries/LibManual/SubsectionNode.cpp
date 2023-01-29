/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SubsectionNode.h"
#include "PageNode.h"
#include <AK/TypeCasts.h>

namespace Manual {

SubsectionNode::SubsectionNode(NonnullRefPtr<SectionNode> parent, StringView name)
    : SectionNode(name, name)
    , m_parent(move(parent))
{
}

Node const* SubsectionNode::parent() const { return m_parent; }

PageNode const* SubsectionNode::document() const
{
    auto maybe_siblings = parent()->children();
    if (maybe_siblings.is_error())
        return nullptr;
    auto siblings = maybe_siblings.release_value();
    for (auto const& sibling : siblings) {
        if (&*sibling == this)
            continue;
        auto sibling_name = sibling->name();
        if (sibling_name.is_error())
            continue;
        if (sibling_name.value() == m_name && is<PageNode>(*sibling))
            return static_cast<PageNode*>(&*sibling);
    }
    return nullptr;
}

ErrorOr<String> SubsectionNode::name() const { return m_name; }

ErrorOr<String> SubsectionNode::path() const
{
    return String::formatted("{}/{}", TRY(m_parent->path()), m_section);
}

}
