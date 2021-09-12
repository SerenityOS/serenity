/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/Text.h>

namespace Markdown {

class Table final : public Block {
public:
    enum class Alignment {
        Center,
        Left,
        Right,
    };

    struct Column {
        Text header;
        Vector<Text> rows;
        Alignment alignment { Alignment::Left };
        size_t relative_width { 0 };
    };

    Table() { }
    virtual ~Table() override { }

    virtual String render_to_html() const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;
    static OwnPtr<Table> parse(Vector<StringView>::ConstIterator& lines);

private:
    Vector<Column> m_columns;
    size_t m_total_width { 1 };
    size_t m_row_count { 0 };
};

}
