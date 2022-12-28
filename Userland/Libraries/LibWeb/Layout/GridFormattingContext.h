/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Length.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class OccupationGrid {
public:
    OccupationGrid(int column_count, int row_count);
    OccupationGrid();

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

    struct PositionedBox {
        Box const& box;
        int row { 0 };
        int row_span { 1 };
        int column { 0 };
        int column_span { 1 };
    };

    struct TemporaryTrack {
        CSS::GridSize min_track_sizing_function;
        CSS::GridSize max_track_sizing_function;
        float base_size { 0 };
        float growth_limit { 0 };
        float space_to_distribute { 0 };
        float planned_increase { 0 };
        bool is_gap { false };

        TemporaryTrack(CSS::GridSize min_track_sizing_function, CSS::GridSize max_track_sizing_function)
            : min_track_sizing_function(min_track_sizing_function)
            , max_track_sizing_function(max_track_sizing_function)
        {
        }

        TemporaryTrack(CSS::GridSize track_sizing_function)
            : min_track_sizing_function(track_sizing_function)
            , max_track_sizing_function(track_sizing_function)
        {
        }

        TemporaryTrack(float size_in_px, bool is_gap)
            : min_track_sizing_function(CSS::GridSize(CSS::Length(size_in_px, CSS::Length::Type::Px)))
            , max_track_sizing_function(CSS::GridSize(CSS::Length(size_in_px, CSS::Length::Type::Px)))
            , base_size(size_in_px)
            , is_gap(is_gap)
        {
        }

        TemporaryTrack()
            : min_track_sizing_function(CSS::GridSize::make_auto())
            , max_track_sizing_function(CSS::GridSize::make_auto())
        {
        }
    };

    Vector<TemporaryTrack> m_grid_rows;
    Vector<TemporaryTrack> m_grid_columns;

    OccupationGrid m_occupation_grid;
    Vector<PositionedBox> m_positioned_boxes;
    Vector<Box const&> m_boxes_to_place;

    float get_free_space_x(AvailableSpace const& available_space);
    float get_free_space_y(Box const&);

    int get_line_index_by_line_name(DeprecatedString const& line_name, CSS::GridTrackSizeList);
    float resolve_definite_track_size(CSS::GridSize const&, AvailableSpace const&, Box const&);
    size_t count_of_gap_columns();
    size_t count_of_gap_rows();
    float resolve_size(CSS::Size const&, AvailableSize const&, Box const&);
    int count_of_repeated_auto_fill_or_fit_tracks(Vector<CSS::ExplicitGridTrack> const& track_list, AvailableSpace const&, Box const&);
    int get_count_of_tracks(Vector<CSS::ExplicitGridTrack> const&, AvailableSpace const&, Box const&);

    void place_item_with_row_and_column_position(Box const& box, Box const& child_box);
    void place_item_with_row_position(Box const& box, Box const& child_box);
    void place_item_with_column_position(Box const& box, Box const& child_box, int& auto_placement_cursor_x, int& auto_placement_cursor_y);
    void place_item_with_no_declared_position(Box const& child_box, int& auto_placement_cursor_x, int& auto_placement_cursor_y);

    void initialize_grid_tracks(Box const&, AvailableSpace const&, int column_count, int row_count);
    void calculate_sizes_of_columns(Box const&, AvailableSpace const&);
    void calculate_sizes_of_rows(Box const&);
};

}
