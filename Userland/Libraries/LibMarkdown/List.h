/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/OwnPtr.h>
#include <YAK/Vector.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/Text.h>

namespace Markdown {

class List final : public Block {
public:
    List(Vector<Text>&& text, bool is_ordered)
        : m_items(move(text))
        , m_is_ordered(is_ordered)
    {
    }
    virtual ~List() override { }

    virtual String render_to_html() const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;

    static OwnPtr<List> parse(Vector<StringView>::ConstIterator& lines);

private:
    // TODO: List items should be considered blocks of their own kind.
    Vector<Text> m_items;
    bool m_is_ordered { false };
};

}
