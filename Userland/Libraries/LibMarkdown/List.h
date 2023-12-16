/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/ContainerBlock.h>
#include <LibMarkdown/LineIterator.h>

namespace Markdown {

class List final : public Block {
public:
    List(Vector<OwnPtr<ContainerBlock>> items, bool is_ordered, bool is_tight, size_t start_number)
        : m_items(move(items))
        , m_is_ordered(is_ordered)
        , m_is_tight(is_tight)
        , m_start_number(start_number)
    {
    }
    virtual ~List() override = default;

    virtual ByteString render_to_html(bool tight = false) const override;
    virtual Vector<ByteString> render_lines_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;

    static OwnPtr<List> parse(LineIterator& lines);

private:
    Vector<OwnPtr<ContainerBlock>> m_items;
    bool m_is_ordered { false };
    bool m_is_tight { false };
    size_t m_start_number { 1 };
};

}
