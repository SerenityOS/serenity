/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PageNode.h"
#include "SectionNode.h"

namespace Manual {

Node const* PageNode::parent() const
{
    return m_section.ptr();
}

NonnullRefPtrVector<Node>& PageNode::children() const
{
    static NonnullRefPtrVector<Node> empty_vector;
    return empty_vector;
}

ErrorOr<String> PageNode::path() const
{
    return TRY(String::formatted("{}/{}.md", TRY(m_section->path()), m_page));
}

}
