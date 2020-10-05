/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
