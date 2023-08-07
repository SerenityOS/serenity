/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PageNode.h"
#include "SectionNode.h"
#include <AK/RefPtr.h>

namespace Manual {

Node const* PageNode::parent() const
{
    return m_section.ptr();
}

ErrorOr<Span<NonnullRefPtr<Node const>>> PageNode::children() const
{
    static Vector<NonnullRefPtr<Node const>> empty_vector;
    return empty_vector.span();
}

ErrorOr<String> PageNode::path() const
{
    return TRY(String::formatted("{}/{}.md", TRY(m_section->path()), m_page));
}

unsigned PageNode::section_number() const
{
    return m_section->section_number();
}

ErrorOr<NonnullRefPtr<PageNode>> PageNode::help_index_page()
{
    static NonnullRefPtr<PageNode> const help_index_page = TRY(try_make_ref_counted<PageNode>(sections[7 - 1], "Help-index"_string));
    return help_index_page;
}

}
