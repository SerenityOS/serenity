/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
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

struct GridPosition {
    int row;
    int column;
    inline bool operator==(GridPosition const&) const = default;
};

struct GridItem {
    JS::NonnullGCPtr<Box const> box;

    int row;
    size_t row_span;
    int column;
    size_t column_span;

    [[nodiscard]] size_t span(GridDimension const dimension) const
    {
        return dimension == GridDimension::Column ? column_span : row_span;
    }

    [[nodiscard]] int raw_position(GridDimension const dimension) const
    {
        return dimension == GridDimension::Column ? column : row;
    }

    [[nodiscard]] CSSPixels add_margin_box_sizes(CSSPixels content_size, GridDimension dimension, LayoutState const& state) const
    {
        auto const& box_state = state.get(box);
        if (dimension == GridDimension::Column)
            return box_state.margin_box_left() + content_size + box_state.margin_box_right();
        return box_state.margin_box_top() + content_size + box_state.margin_box_bottom();
    }

    [[nodiscard]] int gap_adjusted_row(Box const& grid_box) const;
    [[nodiscard]] int gap_adjusted_column(Box const& grid_box) const;
};

class OccupationGrid {
public:
    OccupationGrid(size_t columns_count, size_t rows_count)
    {
        m_max_column_index = max(0, columns_count - 1);
        m_max_row_index = max(0, rows_count - 1);
    }
    OccupationGrid() {};

    void set_occupied(int column_start, int column_end, int row_start, int row_end);

    size_t column_count() const
    {
        return abs(m_min_column_index) + m_max_column_index + 1;
    }

    size_t row_count() const
    {
        return abs(m_min_row_index) + m_max_row_index + 1;
    }

    void set_max_column_index(size_t max_column_index) { m_max_column_index = max_column_index; }

    int min_column_index() const { return m_min_column_index; }
    int max_column_index() const { return m_max_column_index; }
    int min_row_index() const { return m_min_row_index; }
    int max_row_index() const { return m_max_row_index; }

    bool is_occupied(int column_index, int row_index) const;

private:
    HashTable<GridPosition> m_occupation_grid;

    int m_min_column_index { 0 };
    int m_max_column_index { 0 };
    int m_min_row_index { 0 };
    int m_max_row_index { 0 };
};

class GridFormattingContext final : public FormattingContext {
public:
    explicit GridFormattingContext(LayoutState&, Box const& grid_container, FormattingContext* parent);
    ~GridFormattingContext();

    virtual bool inhibits_floating() const override { return true; }

    virtual void run(Box const&, LayoutMode, AvailableSpace const& available_space) override;
    virtual CSSPixels automatic_content_width() const override;
    virtual CSSPixels automatic_content_height() const override;

    Box const& grid_container() const { return context_box(); }

private:
    CSS::JustifyItems justification_for_item(Box const& box) const;
    CSS::AlignItems alignment_for_item(Box const& box) const;

    void resolve_items_box_metrics(GridDimension const dimension);

    CSSPixels m_automatic_content_height { 0 };
    bool is_auto_positioned_row(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
    bool is_auto_positioned_column(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
    bool is_auto_positioned_track(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;

    struct GridTrack {
        CSS::GridSize min_track_sizing_function;
        CSS::GridSize max_track_sizing_function;

        CSSPixels base_size { 0 };
        bool base_size_frozen { false };

        Optional<CSSPixels> growth_limit { 0 };
        bool growth_limit_frozen { false };
        bool infinitely_growable { false };

        CSSPixels space_to_distribute { 0 };
        CSSPixels planned_increase { 0 };
        CSSPixels item_incurred_increase { 0 };

        bool is_gap { false };

        static GridTrack create_from_definition(CSS::ExplicitGridTrack const& definition);
        static GridTrack create_auto();
        static GridTrack create_gap(CSSPixels size);
    };

    struct GridArea {
        String name;
        size_t row_start { 0 };
        size_t row_end { 1 };
        size_t column_start { 0 };
        size_t column_end { 1 };
        bool invalid { false }; /* FIXME: Ignore ignore invalid areas during layout */
    };

    HashMap<String, GridArea> m_grid_areas;

    Vector<GridTrack> m_grid_rows;
    Vector<GridTrack> m_grid_columns;

    bool has_gaps(GridDimension const dimension) const
    {
        if (dimension == GridDimension::Column) {
            return !grid_container().computed_values().column_gap().is_auto();
        } else {
            return !grid_container().computed_values().row_gap().is_auto();
        }
    }

    template<typename Callback>
    void for_each_spanned_track_by_item(GridItem const& item, GridDimension const dimension, Callback callback)
    {
        auto& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
        auto& gaps = dimension == GridDimension::Column ? m_column_gap_tracks : m_row_gap_tracks;
        auto has_gaps = this->has_gaps(dimension);
        auto item_span = item.span(dimension);
        auto item_index = item.raw_position(dimension);
        for (size_t span = 0; span < item_span; span++) {
            auto track_index = item_index + span;
            if (track_index >= tracks.size())
                break;

            auto& track = tracks[track_index];
            callback(track);

            auto is_last_spanned_track = span == item_span - 1;
            if (has_gaps && !is_last_spanned_track) {
                auto& gap = gaps[track_index];
                callback(gap);
            }
        }
    }

    template<typename Callback>
    void for_each_spanned_track_by_item(GridItem const& item, GridDimension const dimension, Callback callback) const
    {
        auto& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
        auto& gaps = dimension == GridDimension::Column ? m_column_gap_tracks : m_row_gap_tracks;
        auto has_gaps = this->has_gaps(dimension);
        auto item_span = item.span(dimension);
        auto item_index = item.raw_position(dimension);
        for (size_t span = 0; span < item_span; span++) {
            auto track_index = item_index + span;
            if (track_index >= tracks.size())
                break;

            auto& track = tracks[track_index];
            callback(track);

            auto is_last_spanned_track = span == item_span - 1;
            if (has_gaps && !is_last_spanned_track) {
                auto& gap = gaps[track_index];
                callback(gap);
            }
        }
    }

    Vector<GridTrack> m_row_gap_tracks;
    Vector<GridTrack> m_column_gap_tracks;

    Vector<GridTrack&> m_grid_rows_and_gaps;
    Vector<GridTrack&> m_grid_columns_and_gaps;

    size_t m_explicit_rows_line_count { 0 };
    size_t m_explicit_columns_line_count { 0 };

    OccupationGrid m_occupation_grid;
    Vector<GridItem> m_grid_items;

    Optional<AvailableSpace> m_available_space;

    void determine_grid_container_height();
    void determine_intrinsic_size_of_grid_container(AvailableSpace const& available_space);

    void layout_absolutely_positioned_element(Box const&, AvailableSpace const&);
    virtual void parent_context_did_dimension_child_root_box() override;

    void resolve_grid_item_widths();
    void resolve_grid_item_heights();

    AvailableSize get_free_space(AvailableSpace const&, GridDimension const) const;

    int get_line_index_by_line_name(String const& line_name, CSS::GridTrackSizeList);
    CSSPixels resolve_definite_track_size(CSS::GridSize const&, AvailableSpace const&);
    int count_of_repeated_auto_fill_or_fit_tracks(Vector<CSS::ExplicitGridTrack> const& track_list, AvailableSpace const&);
    int get_count_of_tracks(Vector<CSS::ExplicitGridTrack> const&, AvailableSpace const&);

    void build_grid_areas();

    void place_grid_items(AvailableSpace const& available_space);
    void place_item_with_row_and_column_position(Box const& child_box);
    void place_item_with_row_position(Box const& child_box);
    void place_item_with_column_position(Box const& child_box, int& auto_placement_cursor_x, int& auto_placement_cursor_y);
    void place_item_with_no_declared_position(Box const& child_box, int& auto_placement_cursor_x, int& auto_placement_cursor_y);

    void initialize_grid_tracks_from_definition(AvailableSpace const& available_space, Vector<CSS::ExplicitGridTrack> const& tracks_definition, Vector<GridTrack>& tracks);
    void initialize_grid_tracks_for_columns_and_rows(AvailableSpace const&);
    void initialize_gap_tracks(AvailableSpace const&);

    void collapse_auto_fit_tracks_if_needed(GridDimension const);

    enum class SpaceDistributionPhase {
        AccommodateMinimumContribution,
        AccommodateMinContentContribution,
        AccommodateMaxContentContribution
    };

    template<typename Match>
    void distribute_extra_space_across_spanned_tracks_base_size(GridDimension dimension, CSSPixels item_size_contribution, SpaceDistributionPhase phase, Vector<GridTrack&>& spanned_tracks, Match matcher);

    template<typename Match>
    void distribute_extra_space_across_spanned_tracks_growth_limit(CSSPixels item_size_contribution, Vector<GridTrack&>& spanned_tracks, Match matcher);

    void initialize_track_sizes(GridDimension);
    void resolve_intrinsic_track_sizes(GridDimension);
    void increase_sizes_to_accommodate_spanning_items_crossing_content_sized_tracks(GridDimension, size_t span);
    void increase_sizes_to_accommodate_spanning_items_crossing_flexible_tracks(GridDimension);
    void maximize_tracks_using_available_size(AvailableSpace const& available_space, GridDimension dimension);
    void maximize_tracks(GridDimension);
    void expand_flexible_tracks(GridDimension);
    void stretch_auto_tracks(GridDimension);
    void run_track_sizing(GridDimension);

    CSSPixels calculate_grid_container_maximum_size(GridDimension const) const;

    CSS::Size const& get_item_preferred_size(GridItem const&, GridDimension const) const;

    CSSPixels calculate_min_content_size(GridItem const&, GridDimension const) const;
    CSSPixels calculate_max_content_size(GridItem const&, GridDimension const) const;

    CSSPixels calculate_min_content_contribution(GridItem const&, GridDimension const) const;
    CSSPixels calculate_max_content_contribution(GridItem const&, GridDimension const) const;

    CSSPixels calculate_limited_min_content_contribution(GridItem const&, GridDimension const) const;
    CSSPixels calculate_limited_max_content_contribution(GridItem const&, GridDimension const) const;

    CSSPixels containing_block_size_for_item(GridItem const&, GridDimension const) const;
    AvailableSpace get_available_space_for_item(GridItem const&) const;

    CSSPixelRect get_grid_area_rect(GridItem const&) const;

    CSSPixels content_size_suggestion(GridItem const&, GridDimension const) const;
    Optional<CSSPixels> specified_size_suggestion(GridItem const&, GridDimension const) const;
    CSSPixels content_based_minimum_size(GridItem const&, GridDimension const) const;
    CSSPixels automatic_minimum_size(GridItem const&, GridDimension const) const;
    CSSPixels calculate_minimum_contribution(GridItem const&, GridDimension const) const;
};

}
