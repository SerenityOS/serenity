/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ManualPageNode.h"
#include "ManualSectionNode.h"

const ManualNode* ManualPageNode::parent() const
{
    return &m_section;
}

NonnullOwnPtrVector<ManualNode>& ManualPageNode::children() const
{
    static NonnullOwnPtrVector<ManualNode> empty_vector;
    return empty_vector;
}

String ManualPageNode::path() const
{
    return String::formatted("{}/{}.md", m_section.path(), m_page);
}
