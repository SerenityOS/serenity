/*
 * Copyright (c) 2022-2023, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Length.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

enum class GridDimension {
    Row,
    Column
};

class OccupationGrid {
public:
    OccupationGrid(size_t column_count, size_t row_count);
    OccupationGrid();

    void maybe_add_column(size_t needed_number_of_columns);
    void maybe_add_row(size_t needed_number_of_rows);
    void set_occupied(size_t column_start, size_t column_end, size_t row_start, size_t row_end);
    void set_occupied(size_t column_index, size_t row_index);

    size_t column_count() { return m_occupation_grid[0].size(); }
    size_t row_count() { return m_occupation_grid.size(); }
    bool is_occupied(size_t column_index, size_t row_index);

private:
    Vector<Vector<bool>> m_occupation_grid;
};

class GridItem {
public:
    GridItem(Box const& box, int row, int row_span, int column, int column_span)
        : m_box(box)
        , m_row(row)
        , m_row_span(row_span)
        , m_column(column)
        , m_column_span(column_span)
    {
    }

    Box const& box() const { return m_box; }

    size_t span(GridDimension const dimension) const
    {
        return dimension == GridDimension::Column ? m_column_span : m_row_span;
    }

    size_t raw_position(GridDimension const dimension) const
    {
        return dimension == GridDimension::Column ? m_column : m_row;
    }

    CSSPixels add_border_box_sizes(CSSPixels content_size, GridDimension dimension, LayoutState const& state) const
    {
        auto& box_state = state.get(box());
        if (dimension == GridDimension::Column) {
            return box_state.border_left + box_state.padding_left + content_size + box_state.padding_right + box_state.border_right;
        } else {
            return box_state.border_top + box_state.padding_top + content_size + box_state.padding_bottom + box_state.border_bottom;
        }
    }

    size_t raw_row() const { return m_row; }
    size_t raw_column() const { return m_column; }

    size_t raw_row_span() const { return m_row_span; }
    size_t raw_column_span() const { return m_column_span; }

    size_t gap_adjusted_row(Box const& grid_box) const;
    size_t gap_adjusted_column(Box const& grid_box) const;

private:
    JS::NonnullGCPtr<Box const> m_box;
    size_t m_row { 0 };
    size_t m_row_span { 1 };
    size_t m_column { 0 };
    size_t m_column_span { 1 };
};

class GridFormattingContext final : public FormattingContext {
public:
    explicit GridFormattingContext(LayoutState&, Box const& grid_container, FormattingContext* parent);
    ~GridFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const& available_space) override;
    virtual CSSPixels automatic_content_width() const override;
    virtual CSSPixels automatic_content_height() const override;

    Box const& grid_container() const { return context_box(); }

private:
    CSSPixels m_automatic_content_height { 0 };
    bool is_auto_positioned_row(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
    bool is_auto_positioned_column(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
    bool is_auto_positioned_track(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;

    struct TemporaryTrack {
        CSS::GridSize min_track_sizing_function;
        CSS::GridSize max_track_sizing_function;

        CSSPixels base_size { 0 };
        bool base_size_frozen { false };

        CSSPixels growth_limit { 0 };
        bool growth_limit_frozen { false };
        bool infinitely_growable { false };

        CSSPixels space_to_distribute { 0 };
        CSSPixels planned_increase { 0 };
        CSSPixels item_incurred_increase { 0 };

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

        TemporaryTrack(CSSPixels size, bool is_gap)
            : min_track_sizing_function(CSS::GridSize(CSS::Length::make_px(size)))
            , max_track_sizing_function(CSS::GridSize(CSS::Length::make_px(size)))
            , base_size(size)
            , is_gap(is_gap)
        {
        }

        TemporaryTrack()
            : min_track_sizing_function(CSS::GridSize::make_auto())
            , max_track_sizing_function(CSS::GridSize::make_auto())
        {
        }
    };

    struct GridArea {
        String name;
        size_t row_start { 0 };
        size_t row_end { 1 };
        size_t column_start { 0 };
        size_t column_end { 1 };
    };
    Vector<GridArea> m_valid_grid_areas;

    Vector<TemporaryTrack> m_grid_rows;
    Vector<TemporaryTrack> m_grid_columns;

    template<typename Callback>
    void for_each_spanned_track_by_item(GridItem const& item, GridDimension const dimension, Callback callback)
    {
        auto& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
        for (size_t span = 0; span < item.span(dimension); span++) {
            if (item.raw_position(dimension) + span >= tracks.size())
                break;

            auto& track = tracks[item.raw_position(dimension) + span];
            callback(track);
        }
    }

    template<typename Callback>
    void for_each_spanned_track_by_item(GridItem const& item, GridDimension const dimension, Callback callback) const
    {
        auto const& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
        for (size_t span = 0; span < item.span(dimension); span++) {
            if (item.raw_position(dimension) + span >= tracks.size())
                break;

            auto const& track = tracks[item.raw_position(dimension) + span];
            callback(track);
        }
    }

    Vector<TemporaryTrack> m_row_gap_tracks;
    Vector<TemporaryTrack> m_column_gap_tracks;

    Vector<TemporaryTrack&> m_grid_rows_and_gaps;
    Vector<TemporaryTrack&> m_grid_columns_and_gaps;

    OccupationGrid m_occupation_grid;
    Vector<GridItem> m_grid_items;
    Vector<JS::NonnullGCPtr<Box const>> m_boxes_to_place;

    void determine_grid_container_height();
    void determine_intrinsic_size_of_grid_container(AvailableSpace const& available_space);

    void resolve_grid_item_widths();
    void resolve_grid_item_heights();

    AvailableSize get_free_space(AvailableSpace const&, GridDimension const) const;

    int get_line_index_by_line_name(String const& line_name, CSS::GridTrackSizeList);
    CSSPixels resolve_definite_track_size(CSS::GridSize const&, AvailableSpace const&);
    int count_of_repeated_auto_fill_or_fit_tracks(Vector<CSS::ExplicitGridTrack> const& track_list, AvailableSpace const&);
    int get_count_of_tracks(Vector<CSS::ExplicitGridTrack> const&, AvailableSpace const&);

    void build_valid_grid_areas();
    int find_valid_grid_area(String const& needle);

    void place_grid_items(AvailableSpace const& available_space);
    void place_item_with_row_and_column_position(Box const& child_box);
    void place_item_with_row_position(Box const& child_box);
    void place_item_with_column_position(Box const& child_box, int& auto_placement_cursor_x, int& auto_placement_cursor_y);
    void place_item_with_no_declared_position(Box const& child_box, int& auto_placement_cursor_x, int& auto_placement_cursor_y);

    void initialize_grid_tracks_from_definition(AvailableSpace const& available_space, Vector<CSS::ExplicitGridTrack> const& tracks_definition, Vector<TemporaryTrack>& tracks);
    void initialize_grid_tracks_for_columns_and_rows(AvailableSpace const&);
    void initialize_gap_tracks(AvailableSpace const&);

    void initialize_track_sizes(AvailableSpace const&, GridDimension const);
    void resolve_intrinsic_track_sizes(AvailableSpace const&, GridDimension const);
    void distribute_extra_space_across_spanned_tracks_base_size(CSSPixels item_size_contribution, Vector<TemporaryTrack&>& spanned_tracks);
    void distribute_extra_space_across_spanned_tracks_growth_limit(CSSPixels item_size_contribution, Vector<TemporaryTrack&>& spanned_tracks);
    void increase_sizes_to_accommodate_spanning_items_crossing_content_sized_tracks(AvailableSpace const&, GridDimension const, size_t span);
    void increase_sizes_to_accommodate_spanning_items_crossing_flexible_tracks(GridDimension const);
    void maximize_tracks(AvailableSpace const&, GridDimension const);
    void expand_flexible_tracks(AvailableSpace const&, GridDimension const);
    void stretch_auto_tracks(AvailableSpace const&, GridDimension const);
    void run_track_sizing(AvailableSpace const&, GridDimension const);

    CSS::Size const& get_item_preferred_size(GridItem const&, GridDimension const) const;

    CSSPixels calculate_min_content_size(GridItem const&, GridDimension const) const;
    CSSPixels calculate_max_content_size(GridItem const&, GridDimension const) const;

    CSSPixels calculate_min_content_contribution(GridItem const&, GridDimension const) const;
    CSSPixels calculate_max_content_contribution(GridItem const&, GridDimension const) const;

    CSSPixels calculate_limited_min_content_contribution(GridItem const&, GridDimension const) const;
    CSSPixels calculate_limited_max_content_contribution(GridItem const&, GridDimension const) const;

    CSSPixels containing_block_size_for_item(GridItem const&, GridDimension const) const;
    AvailableSpace get_available_space_for_item(GridItem const&) const;

    CSS::Size const& get_item_minimum_size(GridItem const&, GridDimension const) const;
    CSSPixels content_size_suggestion(GridItem const&, GridDimension const) const;
    Optional<CSSPixels> specified_size_suggestion(GridItem const&, GridDimension const) const;
    CSSPixels content_based_minimum_size(GridItem const&, GridDimension const) const;
    CSSPixels automatic_minimum_size(GridItem const&, GridDimension const) const;
    CSSPixels calculate_minimum_contribution(GridItem const&, GridDimension const) const;
};

}
