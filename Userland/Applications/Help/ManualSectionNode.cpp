/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ManualSectionNode.h"
#include "ManualPageNode.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibCore/DirIterator.h>

String ManualSectionNode::path() const
{
    return String::formatted("/usr/share/man/man{}", m_section);
}

void ManualSectionNode::reify_if_needed() const
{
    if (m_reified)
        return;
    m_reified = true;

    Core::DirIterator dir_iter { path(), Core::DirIterator::Flags::SkipDots };

    Vector<String> page_names;
    while (dir_iter.has_next()) {
        LexicalPath lexical_path(dir_iter.next_path());
        if (lexical_path.extension() != "md")
            continue;
        page_names.append(lexical_path.title());
    }

    quick_sort(page_names);

    for (auto& page_name : page_names)
        m_children.append(make<ManualPageNode>(*this, move(page_name)));
}

void ManualSectionNode::set_open(bool open)
{
    if (m_open == open)
        return;
    m_open = open;
}
