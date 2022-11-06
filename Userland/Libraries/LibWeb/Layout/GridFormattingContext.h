/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class GridFormattingContext final : public BlockFormattingContext {
public:
    explicit GridFormattingContext(LayoutState&, BlockContainer const&, FormattingContext* parent);
    ~GridFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const& available_space) override;
    virtual float automatic_content_height() const override;

private:
    float m_automatic_content_height { 0 };
    bool is_auto_positioned_row(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
    bool is_auto_positioned_column(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
    bool is_auto_positioned_track(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;

    struct GridTrackSizeConstraints {
        CSS::GridSize min_track_sizing_function;
        CSS::GridSize max_track_sizing_function;
        float base_size { 0 };
        float growth_limit { 0 };
        float space_to_distribute { 0 };
        float planned_increase { 0 };
    };

    Vector<GridTrackSizeConstraints> m_grid_rows;
    Vector<GridTrackSizeConstraints> m_grid_columns;

    float get_free_space_x(AvailableSpace const& available_space);
    float get_free_space_y(Box const&);

    int get_line_index_by_line_name(String const& line_name, CSS::GridTrackSizeList);
};

class OccupationGrid {
public:
    OccupationGrid(int column_count, int row_count);

    void maybe_add_column(int needed_number_of_columns);
    void maybe_add_row(int needed_number_of_rows);
    void set_occupied(int column_start, int column_end, int row_start, int row_end);
    void set_occupied(int column_index, int row_index);

    int column_count() { return static_cast<int>(m_occupation_grid[0].size()); }
    int row_count() { return static_cast<int>(m_occupation_grid.size()); }
    bool is_occupied(int column_index, int row_index);

private:
    Vector<Vector<bool>> m_occupation_grid;
};

}
