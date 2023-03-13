/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableWrapper.h>

namespace Web::Layout {

class TableFormattingContext final : public FormattingContext {
public:
    explicit TableFormattingContext(LayoutState&, TableBox const&, FormattingContext* parent);
    ~TableFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) override;
    virtual CSSPixels automatic_content_height() const override;

    TableBox const& table_box() const { return static_cast<TableBox const&>(context_box()); }
    TableWrapper const& table_wrapper() const
    {
        return verify_cast<TableWrapper>(*table_box().containing_block());
    }

private:
    void calculate_row_column_grid(Box const&);
    void compute_table_measures();
    void compute_table_width();
    void distribute_width_to_columns();
    void determine_intrisic_size_of_table_container(AvailableSpace const& available_space);
    void calculate_row_heights(LayoutMode layout_mode);
    void position_row_boxes(CSSPixels&);
    void position_cell_boxes();

    CSSPixels m_automatic_content_height { 0 };

    Optional<AvailableSpace> m_available_space;

    enum class ColumnType {
        Percent,
        Pixel,
        Auto
    };

    struct Column {
        ColumnType type { ColumnType::Auto };
        CSSPixels left_offset { 0 };
        CSSPixels min_width { 0 };
        CSSPixels max_width { 0 };
        CSSPixels used_width { 0 };
        float percentage_width { 0 };
    };

    struct Row {
        JS::NonnullGCPtr<Box> box;
        CSSPixels used_height { 0 };
        CSSPixels baseline { 0 };
    };

    struct Cell {
        JS::NonnullGCPtr<Box> box;
        size_t column_index;
        size_t row_index;
        size_t column_span;
        size_t row_span;
        CSSPixels baseline { 0 };
        CSSPixels min_width { 0 };
        CSSPixels max_width { 0 };
    };

    Vector<Cell> m_cells;
    Vector<Column> m_columns;
    Vector<Row> m_rows;
};

}
