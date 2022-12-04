/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Layout/BlockFormattingContext.h>

namespace Web::Layout {

class TableFormattingContext final : public BlockFormattingContext {
public:
    explicit TableFormattingContext(LayoutState&, BlockContainer const&, FormattingContext* parent);
    ~TableFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) override;
    virtual float automatic_content_height() const override;

private:
    void calculate_row_column_grid(Box const&);
    void compute_table_measures();
    void compute_table_width(float&);
    void distribute_width_to_columns(float extra_width);
    void determine_intrisic_size_of_table_container(AvailableSpace const& available_space);

    float m_automatic_content_height { 0 };

    struct Column {
        float left_offset { 0 };
        float min_width { 0 };
        float max_width { 0 };
        float used_width { 0 };
    };

    struct Row {
        Box& box;
        float used_width { 0 };
        float baseline { 0 };
    };

    struct Cell {
        Box& box;
        size_t column_index;
        size_t row_index;
        size_t column_span;
        size_t raw_span;
        float baseline { 0 };
    };

    Vector<Cell> m_cells;
    Vector<Column> m_columns;
    Vector<Row> m_rows;
};

}
