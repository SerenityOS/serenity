/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/LineIterator.h>
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

        RecursionDecision walk(Visitor&) const;
    };

    Table() = default;
    virtual ~Table() override = default;

    virtual ByteString render_to_html(bool tight = false) const override;
    virtual Vector<ByteString> render_lines_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;
    static OwnPtr<Table> parse(LineIterator& lines);

    Vector<Column> const& columns() const { return m_columns; }

private:
    Vector<Column> m_columns;
    size_t m_total_width { 1 };
    size_t m_row_count { 0 };
};

}
