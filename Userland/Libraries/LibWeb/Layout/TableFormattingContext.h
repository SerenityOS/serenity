/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Layout/BlockFormattingContext.h>

namespace Web::Layout {

struct ColumnWidth {
    CSSPixels min { 0 };
    CSSPixels max { 0 };
    CSSPixels used { 0 };
    bool is_auto { true };
};

class TableFormattingContext final : public BlockFormattingContext {
public:
    explicit TableFormattingContext(LayoutState&, BlockContainer const&, FormattingContext* parent);
    ~TableFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) override;
    virtual CSSPixels automatic_content_height() const override;

private:
    void calculate_column_widths(Box const& row, CSS::Length const& table_width, Vector<ColumnWidth>& column_widths, AvailableSpace const&);
    void layout_row(Box const& row, Vector<ColumnWidth>& column_widths, AvailableSpace const&);

    CSSPixels m_automatic_content_height { 0 };
};

}
