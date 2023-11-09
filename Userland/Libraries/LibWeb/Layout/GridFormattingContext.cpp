/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 * Copyright (c) 2022-2023, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Node.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/GridFormattingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

GridFormattingContext::GridTrack GridFormattingContext::GridTrack::create_from_definition(CSS::ExplicitGridTrack const& definition)
{
    // NOTE: repeat() is expected to be expanded beforehand.
    VERIFY(!definition.is_repeat());

    if (definition.is_minmax()) {
        return GridTrack {
            .min_track_sizing_function = definition.minmax().min_grid_size(),
            .max_track_sizing_function = definition.minmax().max_grid_size(),
        };
    }

    return GridTrack {
        .min_track_sizing_function = definition.grid_size(),
        .max_track_sizing_function = definition.grid_size(),
    };
}

GridFormattingContext::GridTrack GridFormattingContext::GridTrack::create_auto()
{
    return GridTrack {
        .min_track_sizing_function = CSS::GridSize::make_auto(),
        .max_track_sizing_function = CSS::GridSize::make_auto(),
    };
}

GridFormattingContext::GridTrack GridFormattingContext::GridTrack::create_gap(CSSPixels size)
{
    return GridTrack {
        .min_track_sizing_function = CSS::GridSize(CSS::Length::make_px(size)),
        .max_track_sizing_function = CSS::GridSize(CSS::Length::make_px(size)),
        .base_size = size,
        .is_gap = true,
    };
}

GridFormattingContext::GridFormattingContext(LayoutState& state, Box const& grid_container, FormattingContext* parent)
    : FormattingContext(Type::Grid, state, grid_container, parent)
{
}

GridFormattingContext::~GridFormattingContext() = default;

CSSPixels GridFormattingContext::resolve_definite_track_size(CSS::GridSize const& grid_size, AvailableSpace const& available_space)
{
    VERIFY(grid_size.is_definite());
    switch (grid_size.type()) {
    case CSS::GridSize::Type::LengthPercentage: {
        if (!grid_size.length_percentage().is_auto()) {
            return grid_size.css_size().to_px(grid_container(), available_space.width.to_px_or_zero());
        }
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    return 0;
}

int GridFormattingContext::get_count_of_tracks(Vector<CSS::ExplicitGridTrack> const& track_list, AvailableSpace const& available_space)
{
    auto track_count = 0;
    for (auto const& explicit_grid_track : track_list) {
        if (explicit_grid_track.is_repeat() && explicit_grid_track.repeat().is_default())
            track_count += explicit_grid_track.repeat().repeat_count() * explicit_grid_track.repeat().grid_track_size_list().track_list().size();
        else
            track_count += 1;
    }

    if (track_list.size() == 1
        && track_list.first().is_repeat()
        && (track_list.first().repeat().is_auto_fill() || track_list.first().repeat().is_auto_fit())) {
        track_count = count_of_repeated_auto_fill_or_fit_tracks(track_list, available_space);
    }

    return track_count;
}

int GridFormattingContext::count_of_repeated_auto_fill_or_fit_tracks(Vector<CSS::ExplicitGridTrack> const& track_list, AvailableSpace const& available_space)
{
    // https://www.w3.org/TR/css-grid-2/#auto-repeat
    // 7.2.3.2. Repeat-to-fill: auto-fill and auto-fit repetitions
    // On a subgridded axis, the auto-fill keyword is only valid once per <line-name-list>, and repeats
    // enough times for the name list to match the subgrid’s specified grid span (falling back to 0 if
    // the span is already fulfilled).

    // Otherwise on a standalone axis, when auto-fill is given as the repetition number
    // If the grid container has a definite size or max size in the relevant axis, then the number of
    // repetitions is the largest possible positive integer that does not cause the grid to overflow the
    // content box of its grid container

    CSSPixels sum_of_grid_track_sizes = 0;
    // (treating each track as its max track sizing function if that is definite or its minimum track sizing
    // function otherwise, flooring the max track sizing function by the min track sizing function if both
    // are definite, and taking gap into account)
    // FIXME: take gap into account
    for (auto& explicit_grid_track : track_list.first().repeat().grid_track_size_list().track_list()) {
        auto track_sizing_function = explicit_grid_track;
        if (track_sizing_function.is_minmax()) {
            if (track_sizing_function.minmax().max_grid_size().is_definite() && !track_sizing_function.minmax().min_grid_size().is_definite())
                sum_of_grid_track_sizes += resolve_definite_track_size(track_sizing_function.minmax().max_grid_size(), available_space);
            else if (track_sizing_function.minmax().min_grid_size().is_definite() && !track_sizing_function.minmax().max_grid_size().is_definite())
                sum_of_grid_track_sizes += resolve_definite_track_size(track_sizing_function.minmax().min_grid_size(), available_space);
            else if (track_sizing_function.minmax().min_grid_size().is_definite() && track_sizing_function.minmax().max_grid_size().is_definite())
                sum_of_grid_track_sizes += min(resolve_definite_track_size(track_sizing_function.minmax().min_grid_size(), available_space), resolve_definite_track_size(track_sizing_function.minmax().max_grid_size(), available_space));
        } else {
            sum_of_grid_track_sizes += min(resolve_definite_track_size(track_sizing_function.grid_size(), available_space), resolve_definite_track_size(track_sizing_function.grid_size(), available_space));
        }
    }
    return max(1, (get_free_space(available_space, GridDimension::Column).to_px_or_zero() / sum_of_grid_track_sizes).to_int());

    // For the purpose of finding the number of auto-repeated tracks in a standalone axis, the UA must
    // floor the track size to a UA-specified value to avoid division by zero. It is suggested that this
    // floor be 1px.
}

void GridFormattingContext::place_item_with_row_and_column_position(Box const& child_box)
{
    auto const& grid_row_start = child_box.computed_values().grid_row_start();
    auto const& grid_row_end = child_box.computed_values().grid_row_end();
    auto const& grid_column_start = child_box.computed_values().grid_column_start();
    auto const& grid_column_end = child_box.computed_values().grid_column_end();

    int row_start = 0, row_end = 0, column_start = 0, column_end = 0;

    if (grid_row_start.has_line_number())
        row_start = child_box.computed_values().grid_row_start().line_number() - 1;
    if (grid_row_end.has_line_number())
        row_end = child_box.computed_values().grid_row_end().line_number() - 1;
    if (grid_column_start.has_line_number())
        column_start = child_box.computed_values().grid_column_start().line_number() - 1;
    if (grid_column_end.has_line_number())
        column_end = child_box.computed_values().grid_column_end().line_number() - 1;

    // https://www.w3.org/TR/css-grid-2/#line-placement
    // 8.3. Line-based Placement: the grid-row-start, grid-column-start, grid-row-end, and grid-column-end properties

    // https://www.w3.org/TR/css-grid-2/#grid-placement-slot
    // First attempt to match the grid area’s edge to a named grid area: if there is a grid line whose
    // line name is <custom-ident>-start (for grid-*-start) / <custom-ident>-end (for grid-*-end),
    // contributes the first such line to the grid item’s placement.

    // Otherwise, treat this as if the integer 1 had been specified along with the <custom-ident>.

    // https://www.w3.org/TR/css-grid-2/#grid-placement-int
    // Contributes the Nth grid line to the grid item’s placement. If a negative integer is given, it
    // instead counts in reverse, starting from the end edge of the explicit grid.
    if (row_end < 0)
        row_end = m_occupation_grid.row_count() + row_end + 2;
    if (column_end < 0)
        column_end = m_occupation_grid.column_count() + column_end + 2;

    // If a name is given as a <custom-ident>, only lines with that name are counted. If not enough
    // lines with that name exist, all implicit grid lines are assumed to have that name for the purpose
    // of finding this position.

    // https://www.w3.org/TR/css-grid-2/#grid-placement-span-int
    // Contributes a grid span to the grid item’s placement such that the corresponding edge of the grid
    // item’s grid area is N lines from its opposite edge in the corresponding direction. For example,
    // grid-column-end: span 2 indicates the second grid line in the endward direction from the
    // grid-column-start line.
    size_t row_span = 1;
    size_t column_span = 1;
    if (grid_row_start.has_line_number() && grid_row_end.is_span())
        row_span = grid_row_end.span();
    if (grid_column_start.has_line_number() && grid_column_end.is_span())
        column_span = grid_column_end.span();
    if (grid_row_end.has_line_number() && child_box.computed_values().grid_row_start().is_span()) {
        row_span = grid_row_start.span();
        row_start = row_end - row_span;
    }
    if (grid_column_end.has_line_number() && grid_column_start.is_span()) {
        column_span = grid_column_start.span();
        column_start = column_end - column_span;
    }

    // If a name is given as a <custom-ident>, only lines with that name are counted. If not enough
    // lines with that name exist, all implicit grid lines on the side of the explicit grid
    // corresponding to the search direction are assumed to have that name for the purpose of counting
    // this span.

    // https://drafts.csswg.org/css-grid/#grid-placement-auto
    // auto
    // The property contributes nothing to the grid item’s placement, indicating auto-placement or a
    // default span of one. (See § 8 Placing Grid Items, above.)

    // https://www.w3.org/TR/css-grid-2/#common-uses-named-lines
    // 8.1.3. Named Lines and Spans
    // Instead of counting lines by number, lines can be referenced by their line name:
    if (grid_column_end.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_column_end.identifier()); maybe_grid_area.has_value())
            column_end = maybe_grid_area->column_end;
        else if (auto line_name_index = get_line_index_by_line_name(grid_column_end.identifier(), grid_container().computed_values().grid_template_columns()); line_name_index > -1)
            column_end = line_name_index;
        else
            column_end = 1;
        column_start = column_end - 1;
    }

    if (grid_column_start.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_column_start.identifier()); maybe_grid_area.has_value())
            column_start = maybe_grid_area->column_start;
        else if (auto line_name_index = get_line_index_by_line_name(grid_column_start.identifier(), grid_container().computed_values().grid_template_columns()); line_name_index > -1)
            column_start = line_name_index;
        else
            column_start = 0;
    }

    if (grid_row_end.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_row_end.identifier()); maybe_grid_area.has_value())
            row_end = maybe_grid_area->row_end;
        else if (auto line_name_index = get_line_index_by_line_name(grid_row_end.identifier(), grid_container().computed_values().grid_template_rows()); line_name_index > -1)
            row_end = line_name_index;
        else
            row_end = 1;
        row_start = row_end - 1;
    }

    if (grid_row_start.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_row_start.identifier()); maybe_grid_area.has_value())
            row_start = maybe_grid_area->row_start;
        else if (auto line_name_index = get_line_index_by_line_name(grid_row_start.identifier(), grid_container().computed_values().grid_template_rows()); line_name_index > -1)
            row_start = line_name_index;
        else
            row_start = 0;
    }

    // If there are multiple lines of the same name, they effectively establish a named set of grid
    // lines, which can be exclusively indexed by filtering the placement by name:

    // https://drafts.csswg.org/css-grid/#grid-placement-errors
    // 8.3.1. Grid Placement Conflict Handling
    // If the placement for a grid item contains two lines, and the start line is further end-ward than
    // the end line, swap the two lines. If the start line is equal to the end line, remove the end
    // line.
    if (grid_row_start.is_positioned() && grid_row_end.is_positioned()) {
        if (row_start > row_end)
            swap(row_start, row_end);
        if (row_start != row_end)
            row_span = row_end - row_start;
    }
    if (grid_column_start.is_positioned() && grid_column_end.is_positioned()) {
        if (column_start > column_end)
            swap(column_start, column_end);
        if (column_start != column_end)
            column_span = column_end - column_start;
    }

    // If the placement contains two spans, remove the one contributed by the end grid-placement
    // property.
    if (grid_row_start.is_span() && grid_row_end.is_span())
        row_span = grid_row_start.span();
    if (grid_column_start.is_span() && grid_column_end.is_span())
        column_span = grid_column_start.span();

    // FIXME: If the placement contains only a span for a named line, replace it with a span of 1.

    m_grid_items.append(GridItem {
        .box = child_box,
        .row = row_start,
        .row_span = row_span,
        .column = column_start,
        .column_span = column_span });

    m_occupation_grid.set_occupied(column_start, column_start + column_span, row_start, row_start + row_span);
}

void GridFormattingContext::place_item_with_row_position(Box const& child_box)
{
    auto const& grid_row_start = child_box.computed_values().grid_row_start();
    auto const& grid_row_end = child_box.computed_values().grid_row_end();
    auto const& grid_column_start = child_box.computed_values().grid_column_start();

    int row_start = 0, row_end = 0;

    if (grid_row_start.has_line_number())
        row_start = grid_row_start.line_number() - 1;
    if (grid_row_end.has_line_number())
        row_end = grid_row_end.line_number() - 1;

    // https://www.w3.org/TR/css-grid-2/#line-placement
    // 8.3. Line-based Placement: the grid-row-start, grid-column-start, grid-row-end, and grid-column-end properties

    // https://www.w3.org/TR/css-grid-2/#grid-placement-slot
    // First attempt to match the grid area’s edge to a named grid area: if there is a grid line whose
    // line name is <custom-ident>-start (for grid-*-start) / <custom-ident>-end (for grid-*-end),
    // contributes the first such line to the grid item’s placement.

    // Otherwise, treat this as if the integer 1 had been specified along with the <custom-ident>.

    // https://www.w3.org/TR/css-grid-2/#grid-placement-int
    // Contributes the Nth grid line to the grid item’s placement. If a negative integer is given, it
    // instead counts in reverse, starting from the end edge of the explicit grid.
    if (row_end < 0)
        row_end = m_occupation_grid.row_count() + row_end + 2;

    // If a name is given as a <custom-ident>, only lines with that name are counted. If not enough
    // lines with that name exist, all implicit grid lines are assumed to have that name for the purpose
    // of finding this position.

    // https://www.w3.org/TR/css-grid-2/#grid-placement-span-int
    // Contributes a grid span to the grid item’s placement such that the corresponding edge of the grid
    // item’s grid area is N lines from its opposite edge in the corresponding direction. For example,
    // grid-column-end: span 2 indicates the second grid line in the endward direction from the
    // grid-column-start line.
    size_t row_span = 1;
    if (grid_row_start.has_line_number() && grid_row_end.is_span())
        row_span = grid_row_end.span();
    if (grid_row_end.has_line_number() && grid_row_start.is_span()) {
        row_span = grid_row_start.span();
        row_start = row_end - row_span;
        // FIXME: Remove me once have implemented spans overflowing into negative indexes, e.g., grid-row: span 2 / 1
        if (row_start < 0)
            row_start = 0;
    }

    // If a name is given as a <custom-ident>, only lines with that name are counted. If not enough
    // lines with that name exist, all implicit grid lines on the side of the explicit grid
    // corresponding to the search direction are assumed to have that name for the purpose of counting
    // this span.

    // https://drafts.csswg.org/css-grid/#grid-placement-auto
    // auto
    // The property contributes nothing to the grid item’s placement, indicating auto-placement or a
    // default span of one. (See § 8 Placing Grid Items, above.)

    // https://www.w3.org/TR/css-grid-2/#common-uses-named-lines
    // 8.1.3. Named Lines and Spans
    // Instead of counting lines by number, lines can be referenced by their line name:
    if (grid_row_end.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_row_end.identifier()); maybe_grid_area.has_value())
            row_end = maybe_grid_area->row_end;
        else if (auto line_name_index = get_line_index_by_line_name(grid_row_end.identifier(), grid_container().computed_values().grid_template_rows()); line_name_index > -1)
            row_end = line_name_index - 1;
        else
            row_end = 1;
        row_start = row_end - 1;
    }

    if (grid_row_start.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_row_start.identifier()); maybe_grid_area.has_value())
            row_start = maybe_grid_area->row_start;
        else if (auto line_name_index = get_line_index_by_line_name(grid_row_start.identifier(), grid_container().computed_values().grid_template_rows()); line_name_index > -1)
            row_start = line_name_index;
        else
            row_start = 0;
    }

    // If there are multiple lines of the same name, they effectively establish a named set of grid
    // lines, which can be exclusively indexed by filtering the placement by name:

    // https://drafts.csswg.org/css-grid/#grid-placement-errors
    // 8.3.1. Grid Placement Conflict Handling
    // If the placement for a grid item contains two lines, and the start line is further end-ward than
    // the end line, swap the two lines. If the start line is equal to the end line, remove the end
    // line.
    if (grid_row_start.is_positioned() && grid_row_end.is_positioned()) {
        if (row_start > row_end)
            swap(row_start, row_end);
        if (row_start != row_end)
            row_span = row_end - row_start;
    }
    // FIXME: Have yet to find the spec for this.
    if (!grid_row_start.is_positioned() && grid_row_end.is_positioned() && row_end == 0)
        row_start = 0;

    // If the placement contains two spans, remove the one contributed by the end grid-placement
    // property.
    if (grid_row_start.is_span() && grid_row_end.is_span())
        row_span = grid_row_start.span();

    // FIXME: If the placement contains only a span for a named line, replace it with a span of 1.

    int column_start = 0;
    size_t column_span = grid_column_start.is_span() ? grid_column_start.span() : 1;
    bool found_available_column = false;
    for (size_t column_index = column_start; column_index < m_occupation_grid.column_count(); column_index++) {
        if (!m_occupation_grid.is_occupied(column_index, row_start)) {
            found_available_column = true;
            column_start = column_index;
            break;
        }
    }
    if (!found_available_column) {
        column_start = m_occupation_grid.column_count();
    }
    m_occupation_grid.set_occupied(column_start, column_start + column_span, row_start, row_start + row_span);

    m_grid_items.append(GridItem {
        .box = child_box,
        .row = row_start,
        .row_span = row_span,
        .column = column_start,
        .column_span = column_span });
}

void GridFormattingContext::place_item_with_column_position(Box const& child_box, int& auto_placement_cursor_x, int& auto_placement_cursor_y)
{
    auto const& grid_row_start = child_box.computed_values().grid_row_start();
    auto const& grid_column_start = child_box.computed_values().grid_column_start();
    auto const& grid_column_end = child_box.computed_values().grid_column_end();

    int column_start = 0;
    if (grid_column_start.has_line_number() && grid_column_start.line_number() > 0) {
        column_start = grid_column_start.line_number() - 1;
    } else if (grid_column_start.has_line_number()) {
        // NOTE: Negative indexes count from the end side of the explicit grid
        column_start = m_explicit_columns_line_count + grid_column_start.line_number();
    }

    int column_end = 0;
    if (grid_column_end.has_line_number())
        column_end = grid_column_end.line_number() - 1;

    // https://www.w3.org/TR/css-grid-2/#line-placement
    // 8.3. Line-based Placement: the grid-row-start, grid-column-start, grid-row-end, and grid-column-end properties

    // https://www.w3.org/TR/css-grid-2/#grid-placement-slot
    // First attempt to match the grid area’s edge to a named grid area: if there is a grid line whose
    // line name is <custom-ident>-start (for grid-*-start) / <custom-ident>-end (for grid-*-end),
    // contributes the first such line to the grid item’s placement.

    // Otherwise, treat this as if the integer 1 had been specified along with the <custom-ident>.

    // https://www.w3.org/TR/css-grid-2/#grid-placement-int
    // Contributes the Nth grid line to the grid item’s placement. If a negative integer is given, it
    // instead counts in reverse, starting from the end edge of the explicit grid.
    if (column_end < 0)
        column_end = m_occupation_grid.column_count() + column_end + 2;

    // If a name is given as a <custom-ident>, only lines with that name are counted. If not enough
    // lines with that name exist, all implicit grid lines are assumed to have that name for the purpose
    // of finding this position.

    // https://www.w3.org/TR/css-grid-2/#grid-placement-span-int
    // Contributes a grid span to the grid item’s placement such that the corresponding edge of the grid
    // item’s grid area is N lines from its opposite edge in the corresponding direction. For example,
    // grid-column-end: span 2 indicates the second grid line in the endward direction from the
    // grid-column-start line.
    size_t column_span = 1;
    size_t row_span = grid_row_start.is_span() ? grid_row_start.span() : 1;
    if (grid_column_start.has_line_number() && grid_column_end.is_span())
        column_span = grid_column_end.span();
    if (grid_column_end.has_line_number() && grid_column_start.is_span()) {
        column_span = grid_column_start.span();
        column_start = column_end - column_span;
        // FIXME: Remove me once have implemented spans overflowing into negative indexes, e.g., grid-column: span 2 / 1
        if (column_start < 0)
            column_start = 0;
    }
    // FIXME: Have yet to find the spec for this.
    if (!grid_column_start.is_positioned() && grid_column_end.is_positioned() && column_end == 0)
        column_start = 0;

    // If a name is given as a <custom-ident>, only lines with that name are counted. If not enough
    // lines with that name exist, all implicit grid lines on the side of the explicit grid
    // corresponding to the search direction are assumed to have that name for the purpose of counting
    // this span.

    // https://drafts.csswg.org/css-grid/#grid-placement-auto
    // auto
    // The property contributes nothing to the grid item’s placement, indicating auto-placement or a
    // default span of one. (See § 8 Placing Grid Items, above.)

    // https://www.w3.org/TR/css-grid-2/#common-uses-named-lines
    // 8.1.3. Named Lines and Spans
    // Instead of counting lines by number, lines can be referenced by their line name:
    if (grid_column_end.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_column_end.identifier()); maybe_grid_area.has_value())
            column_end = maybe_grid_area->column_end;
        else if (auto line_name_index = get_line_index_by_line_name(grid_column_end.identifier(), grid_container().computed_values().grid_template_columns()); line_name_index > -1)
            column_end = line_name_index;
        else
            column_end = 1;
        column_start = column_end - 1;
    }

    if (grid_column_start.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_column_start.identifier()); maybe_grid_area.has_value())
            column_start = maybe_grid_area->column_start;
        else if (auto line_name_index = get_line_index_by_line_name(grid_column_start.identifier(), grid_container().computed_values().grid_template_columns()); line_name_index > -1) {
            column_start = line_name_index;
        } else {
            column_start = 0;
        }
    }

    // If there are multiple lines of the same name, they effectively establish a named set of grid
    // lines, which can be exclusively indexed by filtering the placement by name:

    // https://drafts.csswg.org/css-grid/#grid-placement-errors
    // 8.3.1. Grid Placement Conflict Handling
    // If the placement for a grid item contains two lines, and the start line is further end-ward than
    // the end line, swap the two lines. If the start line is equal to the end line, remove the end
    // line.
    if (grid_column_start.is_positioned() && grid_column_end.is_positioned()) {
        if (column_start > column_end)
            swap(column_start, column_end);
        if (column_start != column_end)
            column_span = column_end - column_start;
    }

    // If the placement contains two spans, remove the one contributed by the end grid-placement
    // property.
    if (grid_column_start.is_span() && grid_column_end.is_span())
        column_span = grid_column_start.span();

    // FIXME: If the placement contains only a span for a named line, replace it with a span of 1.

    // 4.1.1.1. Set the column position of the cursor to the grid item's column-start line. If this is
    // less than the previous column position of the cursor, increment the row position by 1.
    if (column_start < auto_placement_cursor_x)
        auto_placement_cursor_y++;
    auto_placement_cursor_x = column_start;

    // 4.1.1.2. Increment the cursor's row position until a value is found where the grid item does not
    // overlap any occupied grid cells (creating new rows in the implicit grid as necessary).
    while (true) {
        if (!m_occupation_grid.is_occupied(column_start, auto_placement_cursor_y)) {
            break;
        }
        auto_placement_cursor_y++;
    }
    // 4.1.1.3. Set the item's row-start line to the cursor's row position, and set the item's row-end
    // line according to its span from that position.
    m_occupation_grid.set_occupied(column_start, column_start + column_span, auto_placement_cursor_y, auto_placement_cursor_y + row_span);

    m_grid_items.append(GridItem {
        .box = child_box,
        .row = auto_placement_cursor_y,
        .row_span = row_span,
        .column = column_start,
        .column_span = column_span });
}

void GridFormattingContext::place_item_with_no_declared_position(Box const& child_box, int& auto_placement_cursor_x, int& auto_placement_cursor_y)
{
    auto const& grid_row_start = child_box.computed_values().grid_row_start();
    auto const& grid_row_end = child_box.computed_values().grid_row_end();
    auto const& grid_column_start = child_box.computed_values().grid_column_start();
    auto const& grid_column_end = child_box.computed_values().grid_column_end();

    // 4.1.2.1. Increment the column position of the auto-placement cursor until either this item's grid
    // area does not overlap any occupied grid cells, or the cursor's column position, plus the item's
    // column span, overflow the number of columns in the implicit grid, as determined earlier in this
    // algorithm.
    auto column_start = 0;
    size_t column_span = 1;
    if (grid_column_start.is_span())
        column_span = grid_column_start.span();
    else if (grid_column_end.is_span())
        column_span = grid_column_end.span();
    auto row_start = 0;
    size_t row_span = 1;
    if (grid_row_start.is_span())
        row_span = grid_row_start.span();
    else if (grid_row_end.is_span())
        row_span = grid_row_end.span();
    auto found_unoccupied_area = false;

    auto const& auto_flow = grid_container().computed_values().grid_auto_flow();

    while (true) {
        while (auto_placement_cursor_x <= m_occupation_grid.max_column_index()) {
            if (auto_placement_cursor_x + static_cast<int>(column_span) <= m_occupation_grid.max_column_index() + 1) {
                auto found_all_available = true;
                for (size_t span_index = 0; span_index < column_span; span_index++) {
                    if (m_occupation_grid.is_occupied(auto_placement_cursor_x + span_index, auto_placement_cursor_y))
                        found_all_available = false;
                }
                if (found_all_available) {
                    found_unoccupied_area = true;
                    column_start = auto_placement_cursor_x;
                    row_start = auto_placement_cursor_y;
                    break;
                }
            }

            auto_placement_cursor_x++;
        }

        if (found_unoccupied_area) {
            break;
        }

        // 4.1.2.2. If a non-overlapping position was found in the previous step, set the item's row-start
        // and column-start lines to the cursor's position. Otherwise, increment the auto-placement cursor's
        // row position (creating new rows in the implicit grid as necessary), set its column position to the
        // start-most column line in the implicit grid, and return to the previous step.
        if (!found_unoccupied_area) {
            if (auto_flow.row) {
                auto_placement_cursor_x = m_occupation_grid.min_column_index();
                auto_placement_cursor_y++;
            } else {
                m_occupation_grid.set_max_column_index(auto_placement_cursor_x);
                auto_placement_cursor_x = 0;
                auto_placement_cursor_y = m_occupation_grid.min_row_index();
            }
        }
    }

    m_occupation_grid.set_occupied(column_start, column_start + column_span, row_start, row_start + row_span);
    m_grid_items.append(GridItem {
        .box = child_box,
        .row = row_start,
        .row_span = row_span,
        .column = column_start,
        .column_span = column_span });
}

void GridFormattingContext::initialize_grid_tracks_from_definition(AvailableSpace const& available_space, Vector<CSS::ExplicitGridTrack> const& tracks_definition, Vector<GridTrack>& tracks)
{
    auto track_count = get_count_of_tracks(tracks_definition, available_space);
    for (auto const& track_definition : tracks_definition) {
        auto repeat_count = (track_definition.is_repeat() && track_definition.repeat().is_default()) ? track_definition.repeat().repeat_count() : 1;
        if (track_definition.is_repeat()) {
            if (track_definition.repeat().is_auto_fill() || track_definition.repeat().is_auto_fit())
                repeat_count = track_count;
        }
        for (auto _ = 0; _ < repeat_count; _++) {
            switch (track_definition.type()) {
            case CSS::ExplicitGridTrack::Type::Default:
            case CSS::ExplicitGridTrack::Type::MinMax:
                tracks.append(GridTrack::create_from_definition(track_definition));
                break;
            case CSS::ExplicitGridTrack::Type::Repeat:
                for (auto& explicit_grid_track : track_definition.repeat().grid_track_size_list().track_list()) {
                    tracks.append(GridTrack::create_from_definition(explicit_grid_track));
                }
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }
    }
}

void GridFormattingContext::initialize_grid_tracks_for_columns_and_rows(AvailableSpace const& available_space)
{
    auto const& grid_computed_values = grid_container().computed_values();

    auto const& grid_auto_columns = grid_computed_values.grid_auto_columns().track_list();
    size_t implicit_column_index = 0;
    // NOTE: If there are implicit tracks created by items with negative indexes they should prepend explicitly defined tracks
    auto negative_index_implied_column_tracks_count = abs(m_occupation_grid.min_column_index());
    for (int column_index = 0; column_index < negative_index_implied_column_tracks_count; column_index++) {
        if (grid_auto_columns.size() > 0) {
            auto definition = grid_auto_columns[implicit_column_index % grid_auto_columns.size()];
            m_grid_columns.append(GridTrack::create_from_definition(definition));
        } else {
            m_grid_columns.append(GridTrack::create_auto());
        }
        implicit_column_index++;
    }
    initialize_grid_tracks_from_definition(available_space, grid_computed_values.grid_template_columns().track_list(), m_grid_columns);
    for (size_t column_index = m_grid_columns.size(); column_index < m_occupation_grid.column_count(); column_index++) {
        if (grid_auto_columns.size() > 0) {
            auto definition = grid_auto_columns[implicit_column_index % grid_auto_columns.size()];
            m_grid_columns.append(GridTrack::create_from_definition(definition));
        } else {
            m_grid_columns.append(GridTrack::create_auto());
        }
        implicit_column_index++;
    }

    auto const& grid_auto_rows = grid_computed_values.grid_auto_rows().track_list();
    size_t implicit_row_index = 0;
    // NOTE: If there are implicit tracks created by items with negative indexes they should prepend explicitly defined tracks
    auto negative_index_implied_row_tracks_count = abs(m_occupation_grid.min_row_index());
    for (int row_index = 0; row_index < negative_index_implied_row_tracks_count; row_index++) {
        if (grid_auto_rows.size() > 0) {
            auto definition = grid_auto_rows[implicit_row_index % grid_auto_rows.size()];
            m_grid_rows.append(GridTrack::create_from_definition(definition));
        } else {
            m_grid_rows.append(GridTrack::create_auto());
        }
        implicit_row_index++;
    }
    initialize_grid_tracks_from_definition(available_space, grid_computed_values.grid_template_rows().track_list(), m_grid_rows);
    for (size_t row_index = m_grid_rows.size(); row_index < m_occupation_grid.row_count(); row_index++) {
        if (grid_auto_rows.size() > 0) {
            auto definition = grid_auto_rows[implicit_row_index % grid_auto_rows.size()];
            m_grid_rows.append(GridTrack::create_from_definition(definition));
        } else {
            m_grid_rows.append(GridTrack::create_auto());
        }
        implicit_row_index++;
    }
}

void GridFormattingContext::initialize_gap_tracks(AvailableSpace const& available_space)
{
    // https://www.w3.org/TR/css-grid-2/#gutters
    // 11.1. Gutters: the row-gap, column-gap, and gap properties
    // For the purpose of track sizing, each gutter is treated as an extra, empty, fixed-size track of
    // the specified size, which is spanned by any grid items that span across its corresponding grid
    // line.
    if (!grid_container().computed_values().column_gap().is_auto() && m_grid_columns.size() > 0) {
        auto column_gap_width = grid_container().computed_values().column_gap().to_px(grid_container(), available_space.width.to_px_or_zero());
        m_column_gap_tracks.ensure_capacity(m_grid_columns.size() - 1);
        for (size_t column_index = 0; column_index < m_grid_columns.size(); column_index++) {
            m_grid_columns_and_gaps.append(m_grid_columns[column_index]);
            if (column_index != m_grid_columns.size() - 1) {
                m_column_gap_tracks.append(GridTrack::create_gap(column_gap_width));
                m_grid_columns_and_gaps.append(m_column_gap_tracks.last());
            }
        }
    } else {
        for (auto& track : m_grid_columns) {
            m_grid_columns_and_gaps.append(track);
        }
    }
    if (!grid_container().computed_values().row_gap().is_auto() && m_grid_rows.size() > 0) {
        auto row_gap_height = grid_container().computed_values().row_gap().to_px(grid_container(), available_space.height.to_px_or_zero());
        m_row_gap_tracks.ensure_capacity(m_grid_rows.size() - 1);
        for (size_t row_index = 0; row_index < m_grid_rows.size(); row_index++) {
            m_grid_rows_and_gaps.append(m_grid_rows[row_index]);
            if (row_index != m_grid_rows.size() - 1) {
                m_row_gap_tracks.append(GridTrack::create_gap(row_gap_height));
                m_grid_rows_and_gaps.append(m_row_gap_tracks.last());
            }
        }
    } else {
        for (auto& track : m_grid_rows) {
            m_grid_rows_and_gaps.append(track);
        }
    }
}

void GridFormattingContext::initialize_track_sizes(AvailableSpace const& available_space, GridDimension const dimension)
{
    // https://www.w3.org/TR/css-grid-2/#algo-init
    // 12.4. Initialize Track Sizes
    // Initialize each track’s base size and growth limit.

    auto& tracks_and_gaps = dimension == GridDimension::Column ? m_grid_columns_and_gaps : m_grid_rows_and_gaps;
    auto& available_size = dimension == GridDimension::Column ? available_space.width : available_space.height;

    for (auto& track : tracks_and_gaps) {
        if (track.is_gap)
            continue;

        if (track.min_track_sizing_function.is_fixed(available_size)) {
            track.base_size = track.min_track_sizing_function.css_size().to_px(grid_container(), available_size.to_px_or_zero());
        } else if (track.min_track_sizing_function.is_intrinsic(available_size)) {
            track.base_size = 0;
        }

        if (track.max_track_sizing_function.is_fixed(available_size)) {
            track.growth_limit = track.max_track_sizing_function.css_size().to_px(grid_container(), available_size.to_px_or_zero());
        } else if (track.max_track_sizing_function.is_flexible_length()) {
            track.growth_limit = {};
        } else if (track.max_track_sizing_function.is_intrinsic(available_size)) {
            track.growth_limit = {};
        } else {
            VERIFY_NOT_REACHED();
        }

        // In all cases, if the growth limit is less than the base size, increase the growth limit to match
        // the base size.
        if (track.growth_limit.has_value() && track.growth_limit.value() < track.base_size)
            track.growth_limit = track.base_size;
    }
}

void GridFormattingContext::resolve_intrinsic_track_sizes(AvailableSpace const& available_space, GridDimension const dimension)
{
    // https://www.w3.org/TR/css-grid-2/#algo-content
    // 12.5. Resolve Intrinsic Track Sizes
    // This step resolves intrinsic track sizing functions to absolute lengths. First it resolves those
    // sizes based on items that are contained wholly within a single track. Then it gradually adds in
    // the space requirements of items that span multiple tracks, evenly distributing the extra space
    // across those tracks insofar as possible.

    auto& tracks_and_gaps = dimension == GridDimension::Column ? m_grid_columns_and_gaps : m_grid_rows_and_gaps;

    // FIXME: 1. Shim baseline-aligned items so their intrinsic size contributions reflect their baseline alignment.

    // 2. Size tracks to fit non-spanning items:
    increase_sizes_to_accommodate_spanning_items_crossing_content_sized_tracks(available_space, dimension, 1);

    // 3. Increase sizes to accommodate spanning items crossing content-sized tracks: Next, consider the
    // items with a span of 2 that do not span a track with a flexible sizing function.
    // Repeat incrementally for items with greater spans until all items have been considered.
    size_t max_item_span = 1;
    for (auto& item : m_grid_items)
        max_item_span = max(item.span(dimension), max_item_span);
    for (size_t span = 2; span <= max_item_span; span++)
        increase_sizes_to_accommodate_spanning_items_crossing_content_sized_tracks(available_space, dimension, span);

    // 4. Increase sizes to accommodate spanning items crossing flexible tracks: Next, repeat the previous
    // step instead considering (together, rather than grouped by span size) all items that do span a
    // track with a flexible sizing function while
    increase_sizes_to_accommodate_spanning_items_crossing_flexible_tracks(dimension);

    // 5. If any track still has an infinite growth limit (because, for example, it had no items placed in
    // it or it is a flexible track), set its growth limit to its base size.
    for (auto& track : tracks_and_gaps) {
        if (!track.growth_limit.has_value())
            track.growth_limit = track.base_size;
    }
}

template<typename Match>
void GridFormattingContext::distribute_extra_space_across_spanned_tracks_base_size(GridDimension dimension, CSSPixels item_size_contribution, SpaceDistributionPhase phase, Vector<GridTrack&>& spanned_tracks, Match matcher)
{
    auto& available_size = dimension == GridDimension::Column ? m_available_space->width : m_available_space->height;

    Vector<GridTrack&> affected_tracks;
    for (auto& track : spanned_tracks) {
        if (matcher(track))
            affected_tracks.append(track);
    }

    if (affected_tracks.size() == 0)
        return;

    for (auto& track : affected_tracks)
        track.item_incurred_increase = 0;

    // 1. Find the space to distribute:
    CSSPixels spanned_tracks_sizes_sum = 0;
    for (auto& track : spanned_tracks)
        spanned_tracks_sizes_sum += track.base_size;

    // Subtract the corresponding size of every spanned track from the item’s size contribution to find the item’s
    // remaining size contribution.
    auto extra_space = max(CSSPixels(0), item_size_contribution - spanned_tracks_sizes_sum);

    // 2. Distribute space up to limits:
    while (extra_space > 0) {
        auto all_frozen = all_of(affected_tracks, [](auto const& track) { return track.base_size_frozen; });
        if (all_frozen)
            break;

        // Find the item-incurred increase for each spanned track with an affected size by: distributing the space
        // equally among such tracks, freezing a track’s item-incurred increase as its affected size + item-incurred
        // increase reaches its limit
        CSSPixels increase_per_track = max(CSSPixels::smallest_positive_value(), extra_space / affected_tracks.size());
        for (auto& track : affected_tracks) {
            if (track.base_size_frozen)
                continue;

            auto increase = min(increase_per_track, extra_space);

            if (track.growth_limit.has_value()) {
                auto maximum_increase = track.growth_limit.value() - track.base_size;
                if (track.item_incurred_increase + increase >= maximum_increase) {
                    track.base_size_frozen = true;
                    increase = maximum_increase - track.item_incurred_increase;
                }
            }
            track.item_incurred_increase += increase;
            extra_space -= increase;
        }
    }

    // 3. Distribute space beyond limits
    if (extra_space > 0) {
        Vector<GridTrack&> tracks_to_grow_beyond_limits;

        // If space remains after all tracks are frozen, unfreeze and continue to
        // distribute space to the item-incurred increase of...
        if (phase == SpaceDistributionPhase::AccommodateMinimumContribution || phase == SpaceDistributionPhase::AccommodateMinContentContribution) {
            // when accommodating minimum contributions or accommodating min-content contributions: any affected track
            // that happens to also have an intrinsic max track sizing function
            for (auto& track : affected_tracks) {
                if (track.max_track_sizing_function.is_intrinsic(available_size))
                    tracks_to_grow_beyond_limits.append(track);
            }

            // if there are no such tracks, then all affected tracks.
            if (tracks_to_grow_beyond_limits.size() == 0)
                tracks_to_grow_beyond_limits = affected_tracks;
        }
        // FIXME: when accommodating max-content contributions: any affected track that happens to also have a
        //        max-content max track sizing function; if there are no such tracks, then all affected tracks.

        CSSPixels increase_per_track = extra_space / affected_tracks.size();
        for (auto& track : affected_tracks) {
            auto increase = min(increase_per_track, extra_space);
            track.item_incurred_increase += increase;
            extra_space -= increase;
        }
    }

    // 4. For each affected track, if the track’s item-incurred increase is larger than the track’s planned increase
    //    set the track’s planned increase to that value.
    for (auto& track : affected_tracks) {
        if (track.item_incurred_increase > track.planned_increase)
            track.planned_increase = track.item_incurred_increase;
    }
}

template<typename Match>
void GridFormattingContext::distribute_extra_space_across_spanned_tracks_growth_limit(CSSPixels item_size_contribution, Vector<GridTrack&>& spanned_tracks, Match matcher)
{
    Vector<GridTrack&> affected_tracks;
    for (auto& track : spanned_tracks) {
        if (matcher(track))
            affected_tracks.append(track);
    }

    for (auto& track : affected_tracks)
        track.item_incurred_increase = 0;

    if (affected_tracks.size() == 0)
        return;

    // 1. Find the space to distribute:
    CSSPixels spanned_tracks_sizes_sum = 0;
    for (auto& track : spanned_tracks) {
        if (track.growth_limit.has_value()) {
            spanned_tracks_sizes_sum += track.growth_limit.value();
        } else {
            spanned_tracks_sizes_sum += track.base_size;
        }
    }

    // Subtract the corresponding size of every spanned track from the item’s size contribution to find the item’s
    // remaining size contribution.
    auto extra_space = max(CSSPixels(0), item_size_contribution - spanned_tracks_sizes_sum);

    // 2. Distribute space up to limits:
    while (extra_space > 0) {
        auto all_frozen = all_of(affected_tracks, [](auto const& track) { return track.growth_limit_frozen; });
        if (all_frozen)
            break;

        // Find the item-incurred increase for each spanned track with an affected size by: distributing the space
        // equally among such tracks, freezing a track’s item-incurred increase as its affected size + item-incurred
        // increase reaches its limit
        CSSPixels increase_per_track = max(CSSPixels::smallest_positive_value(), extra_space / affected_tracks.size());
        for (auto& track : affected_tracks) {
            if (track.growth_limit_frozen)
                continue;

            auto increase = min(increase_per_track, extra_space);

            // For growth limits, the limit is infinity if it is marked as infinitely growable, and equal to the
            // growth limit otherwise.
            if (!track.infinitely_growable && track.growth_limit.has_value()) {
                auto maximum_increase = track.growth_limit.value() - track.base_size;
                if (track.item_incurred_increase + increase >= maximum_increase) {
                    track.growth_limit_frozen = true;
                    increase = maximum_increase - track.item_incurred_increase;
                }
            }
            track.item_incurred_increase += increase;
            extra_space -= increase;
        }
    }

    // FIXME: 3. Distribute space beyond limits

    // 4. For each affected track, if the track’s item-incurred increase is larger than the track’s planned increase
    //    set the track’s planned increase to that value.
    for (auto& track : spanned_tracks) {
        if (track.item_incurred_increase > track.planned_increase)
            track.planned_increase = track.item_incurred_increase;
    }
}

void GridFormattingContext::increase_sizes_to_accommodate_spanning_items_crossing_content_sized_tracks(AvailableSpace const& available_space, GridDimension const dimension, size_t span)
{
    auto& available_size = dimension == GridDimension::Column ? available_space.width : available_space.height;
    auto& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
    for (auto& item : m_grid_items) {
        auto const item_span = item.span(dimension);
        if (item_span != span)
            continue;

        Vector<GridTrack&> spanned_tracks;
        for_each_spanned_track_by_item(item, dimension, [&](GridTrack& track) {
            spanned_tracks.append(track);
        });

        auto item_spans_tracks_with_flexible_sizing_function = any_of(spanned_tracks, [](auto& track) {
            return track.min_track_sizing_function.is_flexible_length() || track.max_track_sizing_function.is_flexible_length();
        });
        if (item_spans_tracks_with_flexible_sizing_function)
            continue;

        // 1. For intrinsic minimums: First increase the base size of tracks with an intrinsic min track sizing
        //    function by distributing extra space as needed to accommodate these items’ minimum contributions.
        auto item_size_contribution = [&] {
            // If the grid container is being sized under a min- or max-content constraint, use the items’ limited
            // min-content contributions in place of their minimum contributions here.
            if (available_size.is_intrinsic_sizing_constraint())
                return calculate_limited_min_content_contribution(item, dimension);
            return calculate_minimum_contribution(item, dimension);
        }();
        distribute_extra_space_across_spanned_tracks_base_size(dimension, item_size_contribution, SpaceDistributionPhase::AccommodateMinimumContribution, spanned_tracks, [&](GridTrack const& track) {
            return track.min_track_sizing_function.is_intrinsic(available_size);
        });
        for (auto& track : spanned_tracks) {
            track.base_size += track.planned_increase;
            track.planned_increase = 0;
        }

        // 2. For content-based minimums: Next continue to increase the base size of tracks with a min track
        //    sizing function of min-content or max-content by distributing extra space as needed to account for
        //    these items' min-content contributions.
        auto item_min_content_contribution = calculate_min_content_contribution(item, dimension);
        distribute_extra_space_across_spanned_tracks_base_size(dimension, item_min_content_contribution, SpaceDistributionPhase::AccommodateMinContentContribution, spanned_tracks, [&](GridTrack const& track) {
            return track.min_track_sizing_function.is_min_content() || track.min_track_sizing_function.is_max_content();
        });
        for (auto& track : spanned_tracks) {
            track.base_size += track.planned_increase;
            track.planned_increase = 0;
        }

        // 3. For max-content minimums: Next, if the grid container is being sized under a max-content constraint,
        //    continue to increase the base size of tracks with a min track sizing function of auto or max-content by
        //    distributing extra space as needed to account for these items' limited max-content contributions.
        if (available_size.is_max_content()) {
            auto item_limited_max_content_contribution = calculate_limited_max_content_contribution(item, dimension);
            distribute_extra_space_across_spanned_tracks_base_size(dimension, item_limited_max_content_contribution, SpaceDistributionPhase::AccommodateMaxContentContribution, spanned_tracks, [&](GridTrack const& track) {
                return track.min_track_sizing_function.is_auto(available_size) || track.min_track_sizing_function.is_max_content();
            });
            for (auto& track : spanned_tracks) {
                track.base_size += track.planned_increase;
                track.planned_increase = 0;
            }
        }

        // 4. If at this point any track’s growth limit is now less than its base size, increase its growth limit to
        //    match its base size.
        for (auto& track : tracks) {
            if (track.growth_limit.has_value() && track.growth_limit.value() < track.base_size)
                track.growth_limit = track.base_size;
        }

        // 5. For intrinsic maximums: Next increase the growth limit of tracks with an intrinsic max track sizing
        distribute_extra_space_across_spanned_tracks_growth_limit(item_min_content_contribution, spanned_tracks, [&](GridTrack const& track) {
            return track.max_track_sizing_function.is_intrinsic(available_size);
        });
        for (auto& track : spanned_tracks) {
            if (!track.growth_limit.has_value()) {
                // If the affected size is an infinite growth limit, set it to the track’s base size plus the planned increase.
                track.growth_limit = track.base_size + track.planned_increase;
                // Mark any tracks whose growth limit changed from infinite to finite in this step as infinitely growable
                // for the next step.
                track.infinitely_growable = true;
            } else {
                track.growth_limit.value() += track.planned_increase;
            }
            track.planned_increase = 0;
        }

        // 6. For max-content maximums: Lastly continue to increase the growth limit of tracks with a max track
        //    sizing function of max-content by distributing extra space as needed to account for these items' max-
        //    content contributions.
        auto item_max_content_contribution = calculate_max_content_contribution(item, dimension);
        distribute_extra_space_across_spanned_tracks_growth_limit(item_max_content_contribution, spanned_tracks, [&](GridTrack const& track) {
            return track.max_track_sizing_function.is_max_content() || track.max_track_sizing_function.is_auto(available_size);
        });
        for (auto& track : spanned_tracks) {
            if (!track.growth_limit.has_value()) {
                // If the affected size is an infinite growth limit, set it to the track’s base size plus the planned increase.
                track.growth_limit = track.base_size + track.planned_increase;
            } else {
                track.growth_limit.value() += track.planned_increase;
            }
            track.planned_increase = 0;
        }
    }
}

void GridFormattingContext::increase_sizes_to_accommodate_spanning_items_crossing_flexible_tracks(GridDimension const dimension)
{
    auto& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
    for (auto& item : m_grid_items) {
        Vector<GridTrack&> spanned_tracks;
        for_each_spanned_track_by_item(item, dimension, [&](GridTrack& track) {
            spanned_tracks.append(track);
        });

        auto item_spans_tracks_with_flexible_sizing_function = any_of(spanned_tracks, [](auto& track) {
            return track.min_track_sizing_function.is_flexible_length() || track.max_track_sizing_function.is_flexible_length();
        });
        if (!item_spans_tracks_with_flexible_sizing_function)
            continue;

        // 1. For intrinsic minimums: First increase the base size of tracks with an intrinsic min track sizing
        //    function by distributing extra space as needed to accommodate these items’ minimum contributions.
        auto item_minimum_contribution = calculate_minimum_contribution(item, dimension);
        distribute_extra_space_across_spanned_tracks_base_size(dimension,
            item_minimum_contribution, SpaceDistributionPhase::AccommodateMinimumContribution, spanned_tracks, [&](GridTrack const& track) {
                return track.min_track_sizing_function.is_flexible_length();
            });

        for (auto& track : spanned_tracks) {
            track.base_size += track.planned_increase;
            track.planned_increase = 0;
        }

        // 4. If at this point any track’s growth limit is now less than its base size, increase its growth limit to
        //    match its base size.
        for (auto& track : tracks) {
            if (track.growth_limit.has_value() && track.growth_limit.value() < track.base_size)
                track.growth_limit = track.base_size;
        }
    }
}

void GridFormattingContext::maximize_tracks(AvailableSpace const& available_space, GridDimension const dimension)
{
    // https://www.w3.org/TR/css-grid-2/#algo-grow-tracks
    // 12.6. Maximize Tracks

    auto& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;

    auto get_free_space_px = [&]() -> CSSPixels {
        // For the purpose of this step: if sizing the grid container under a max-content constraint, the
        // free space is infinite; if sizing under a min-content constraint, the free space is zero.
        auto free_space = get_free_space(available_space, dimension);
        if (free_space.is_max_content() || free_space.is_indefinite()) {
            return CSSPixels::max();
        } else if (free_space.is_min_content()) {
            return 0;
        } else {
            return free_space.to_px_or_zero();
        }
    };

    auto free_space_px = get_free_space_px();

    // If the free space is positive, distribute it equally to the base sizes of all tracks, freezing
    // tracks as they reach their growth limits (and continuing to grow the unfrozen tracks as needed).
    while (free_space_px > 0) {
        auto free_space_to_distribute_per_track = free_space_px / tracks.size();
        for (auto& track : tracks) {
            if (track.base_size_frozen)
                continue;
            VERIFY(track.growth_limit.has_value());
            track.base_size = min(track.growth_limit.value(), track.base_size + free_space_to_distribute_per_track);
        }
        if (get_free_space_px() == free_space_px)
            break;
        free_space_px = get_free_space_px();
    }

    // FIXME: If this would cause the grid to be larger than the grid container’s inner size as limited by its
    // max-width/height, then redo this step, treating the available grid space as equal to the grid
    // container’s inner size when it’s sized to its max-width/height.
}

void GridFormattingContext::expand_flexible_tracks(AvailableSpace const& available_space, GridDimension const dimension)
{
    // https://drafts.csswg.org/css-grid/#algo-flex-tracks
    // 12.7. Expand Flexible Tracks
    // This step sizes flexible tracks using the largest value it can assign to an fr without exceeding
    // the available space.

    auto& tracks_and_gaps = dimension == GridDimension::Column ? m_grid_columns_and_gaps : m_grid_rows_and_gaps;
    auto& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
    auto& available_size = dimension == GridDimension::Column ? available_space.width : available_space.height;
    // FIXME: This should idealy take a Span, as that is more idomatic, but Span does not yet support holding references
    auto find_the_size_of_an_fr = [&](Vector<GridTrack&> const& tracks, CSSPixels space_to_fill) -> CSSPixelFraction {
        // https://www.w3.org/TR/css-grid-2/#algo-find-fr-size

        // 1. Let leftover space be the space to fill minus the base sizes of the non-flexible grid tracks.
        auto leftover_space = space_to_fill;
        for (auto& track : tracks) {
            if (!track.max_track_sizing_function.is_flexible_length()) {
                leftover_space -= track.base_size;
            }
        }

        // 2. Let flex factor sum be the sum of the flex factors of the flexible tracks.
        //    If this value is less than 1, set it to 1 instead.
        CSSPixels flex_factor_sum = 0;
        for (auto& track : tracks) {
            if (track.max_track_sizing_function.is_flexible_length())
                flex_factor_sum += CSSPixels::nearest_value_for(track.max_track_sizing_function.flex_factor());
        }
        if (flex_factor_sum < 1)
            flex_factor_sum = 1;

        // 3. Let the hypothetical fr size be the leftover space divided by the flex factor sum.
        auto hypothetical_fr_size = leftover_space / flex_factor_sum;

        // FIXME: 4. If the product of the hypothetical fr size and a flexible track’s flex factor is less than the track’s
        //    base size, restart this algorithm treating all such tracks as inflexible.

        // 5. Return the hypothetical fr size.
        return hypothetical_fr_size;
    };

    // First, find the grid’s used flex fraction:
    auto flex_fraction = [&]() -> CSSPixelFraction {
        auto free_space = get_free_space(available_space, dimension);
        // If the free space is zero or if sizing the grid container under a min-content constraint:
        if ((free_space.is_definite() && free_space.to_px_or_zero() == 0) || available_size.is_min_content()) {
            // The used flex fraction is zero.
            return 0;
            // Otherwise, if the free space is a definite length:
        } else if (free_space.is_definite()) {
            // The used flex fraction is the result of finding the size of an fr using all of the grid tracks and a space
            // to fill of the available grid space.
            return find_the_size_of_an_fr(tracks_and_gaps, available_size.to_px_or_zero());
        } else {
            // Otherwise, if the free space is an indefinite length:
            // The used flex fraction is the maximum of:
            CSSPixelFraction result = 0;
            // For each flexible track, if the flexible track’s flex factor is greater than one, the result of dividing
            // the track’s base size by its flex factor; otherwise, the track’s base size.
            for (auto& track : tracks) {
                if (track.max_track_sizing_function.is_flexible_length()) {
                    if (track.max_track_sizing_function.flex_factor() > 1) {
                        result = max(result, track.base_size / CSSPixels::nearest_value_for(track.max_track_sizing_function.flex_factor()));
                    } else {
                        result = max(result, track.base_size / 1);
                    }
                }
            }
            // For each grid item that crosses a flexible track, the result of finding the size of an fr using all the
            // grid tracks that the item crosses and a space to fill of the item’s max-content contribution.
            for (auto& item : m_grid_items) {
                Vector<GridTrack&> spanned_tracks;
                bool crosses_flexible_track = false;
                for_each_spanned_track_by_item(item, dimension, [&](GridTrack& track) {
                    spanned_tracks.append(track);
                    if (track.max_track_sizing_function.is_flexible_length())
                        crosses_flexible_track = true;
                });

                if (crosses_flexible_track)
                    result = max(result, find_the_size_of_an_fr(spanned_tracks, calculate_max_content_contribution(item, dimension)));
            }

            return result;
        }
    }();

    // For each flexible track, if the product of the used flex fraction and the track’s flex factor is greater than
    // the track’s base size, set its base size to that product.
    for (auto& track : tracks_and_gaps) {
        if (track.max_track_sizing_function.is_flexible_length()) {
            auto scaled_fraction = CSSPixels::nearest_value_for(track.max_track_sizing_function.flex_factor()) * flex_fraction;
            if (scaled_fraction > track.base_size) {
                track.base_size = scaled_fraction;
            }
        }
    }
}

void GridFormattingContext::stretch_auto_tracks(AvailableSpace const& available_space, GridDimension const dimension)
{
    // https://drafts.csswg.org/css-grid/#algo-stretch
    // 12.8. Stretch auto Tracks

    auto& tracks_and_gaps = dimension == GridDimension::Column ? m_grid_columns_and_gaps : m_grid_rows_and_gaps;
    auto& available_size = dimension == GridDimension::Column ? available_space.width : available_space.height;

    // When the content-distribution property of the grid container is normal or stretch in this axis,
    // this step expands tracks that have an auto max track sizing function by dividing any remaining
    // positive, definite free space equally amongst them. If the free space is indefinite, but the grid
    // container has a definite min-width/height, use that size to calculate the free space for this
    // step instead.
    CSSPixels used_space = 0;
    for (auto& track : tracks_and_gaps) {
        if (!track.max_track_sizing_function.is_auto(available_size))
            used_space += track.base_size;
    }

    CSSPixels remaining_space = available_size.is_definite() ? available_size.to_px_or_zero() - used_space : 0;
    auto count_of_auto_max_sizing_tracks = 0;
    for (auto& track : tracks_and_gaps) {
        if (track.max_track_sizing_function.is_auto(available_size))
            count_of_auto_max_sizing_tracks++;
    }

    for (auto& track : tracks_and_gaps) {
        if (track.max_track_sizing_function.is_auto(available_size))
            track.base_size = max(track.base_size, remaining_space / count_of_auto_max_sizing_tracks);
    }
}

void GridFormattingContext::run_track_sizing(AvailableSpace const& available_space, GridDimension const dimension)
{
    // https://www.w3.org/TR/css-grid-2/#algo-track-sizing
    // 12.3. Track Sizing Algorithm

    // 1. Initialize Track Sizes
    initialize_track_sizes(available_space, dimension);

    // 2. Resolve Intrinsic Track Sizes
    resolve_intrinsic_track_sizes(available_space, dimension);

    // 3. Maximize Tracks
    maximize_tracks(available_space, dimension);

    // 4. Expand Flexible Tracks
    expand_flexible_tracks(available_space, dimension);

    // 5. Expand Stretched auto Tracks
    stretch_auto_tracks(available_space, dimension);

    // If calculating the layout of a grid item in this step depends on the available space in the block
    // axis, assume the available space that it would have if any row with a definite max track sizing
    // function had that size and all other rows were infinite. If both the grid container and all
    // tracks have definite sizes, also apply align-content to find the final effective size of any gaps
    // spanned by such items; otherwise ignore the effects of track alignment in this estimation.
}

void GridFormattingContext::build_grid_areas()
{
    // https://www.w3.org/TR/css-grid-2/#grid-template-areas-property
    // If a named grid area spans multiple grid cells, but those cells do not form a single
    // filled-in rectangle, the declaration is invalid.
    auto const& rows = grid_container().computed_values().grid_template_areas();

    auto find_area_rectangle = [&](size_t x_start, size_t y_start, String const& name) {
        bool invalid = false;
        size_t x_end = x_start;
        size_t y_end = y_start;
        while (x_end < rows[y_start].size() && rows[y_start][x_end] == name)
            x_end++;
        while (y_end < rows.size() && rows[y_end][x_start] == name)
            y_end++;
        for (size_t y = y_start; y < y_end; y++) {
            for (size_t x = x_start; x < x_end; x++) {
                if (rows[y][x] != name) {
                    // If a named grid area spans multiple grid cells, but those cells do not form a single filled-in rectangle, the declaration is invalid.
                    invalid = true;
                    break;
                }
            }
        }
        m_grid_areas.set(name, { name, y_start, y_end, x_start, x_end, invalid });
    };

    for (size_t y = 0; y < rows.size(); y++) {
        for (size_t x = 0; x < rows[y].size(); x++) {
            auto name = rows[y][x];
            if (auto grid_area = m_grid_areas.get(name); grid_area.has_value())
                continue;
            find_area_rectangle(x, y, name);
        }
    }
}

void GridFormattingContext::place_grid_items(AvailableSpace const& available_space)
{
    auto grid_template_columns = grid_container().computed_values().grid_template_columns();
    auto grid_template_rows = grid_container().computed_values().grid_template_rows();
    auto column_count = get_count_of_tracks(grid_template_columns.track_list(), available_space);
    auto row_count = get_count_of_tracks(grid_template_rows.track_list(), available_space);

    // https://drafts.csswg.org/css-grid/#overview-placement
    // 2.2. Placing Items
    // The contents of the grid container are organized into individual grid items (analogous to
    // flex items), which are then assigned to predefined areas in the grid. They can be explicitly
    // placed using coordinates through the grid-placement properties or implicitly placed into
    // empty areas using auto-placement.
    HashMap<int, Vector<JS::NonnullGCPtr<Box const>>> order_item_bucket;
    grid_container().for_each_child_of_type<Box>([&](Box& child_box) {
        if (can_skip_is_anonymous_text_run(child_box))
            return IterationDecision::Continue;

        if (child_box.is_out_of_flow(*this))
            return IterationDecision::Continue;

        child_box.set_grid_item(true);

        auto& order_bucket = order_item_bucket.ensure(child_box.computed_values().order());
        order_bucket.append(child_box);

        return IterationDecision::Continue;
    });

    m_occupation_grid = OccupationGrid(column_count, row_count);

    build_grid_areas();

    // https://drafts.csswg.org/css-grid/#auto-placement-algo
    // 8.5. Grid Item Placement Algorithm

    auto keys = order_item_bucket.keys();
    quick_sort(keys, [](auto& a, auto& b) { return a < b; });

    // FIXME: 0. Generate anonymous grid items

    // 1. Position anything that's not auto-positioned.
    for (auto key : keys) {
        auto& boxes_to_place = order_item_bucket.get(key).value();
        for (size_t i = 0; i < boxes_to_place.size(); i++) {
            auto const& child_box = boxes_to_place[i];
            if (is_auto_positioned_row(child_box->computed_values().grid_row_start(), child_box->computed_values().grid_row_end())
                || is_auto_positioned_column(child_box->computed_values().grid_column_start(), child_box->computed_values().grid_column_end()))
                continue;
            place_item_with_row_and_column_position(child_box);
            boxes_to_place.remove(i);
            i--;
        }
    }

    // 2. Process the items locked to a given row.
    // FIXME: Do "dense" packing
    for (auto key : keys) {
        auto& boxes_to_place = order_item_bucket.get(key).value();
        for (size_t i = 0; i < boxes_to_place.size(); i++) {
            auto const& child_box = boxes_to_place[i];
            if (is_auto_positioned_row(child_box->computed_values().grid_row_start(), child_box->computed_values().grid_row_end()))
                continue;
            place_item_with_row_position(child_box);
            boxes_to_place.remove(i);
            i--;
        }
    }

    // 3. Determine the columns in the implicit grid.
    // NOTE: "implicit grid" here is the same as the m_occupation_grid

    // 3.1. Start with the columns from the explicit grid.
    // NOTE: Done in step 1.

    // 3.2. Among all the items with a definite column position (explicitly positioned items, items
    // positioned in the previous step, and items not yet positioned but with a definite column) add
    // columns to the beginning and end of the implicit grid as necessary to accommodate those items.
    // NOTE: "Explicitly positioned items" and "items positioned in the previous step" done in step 1
    // and 2, respectively. Adding columns for "items not yet positioned but with a definite column"
    // will be done in step 4.

    // 3.3. If the largest column span among all the items without a definite column position is larger
    // than the width of the implicit grid, add columns to the end of the implicit grid to accommodate
    // that column span.
    for (auto key : keys) {
        auto& boxes_to_place = order_item_bucket.get(key).value();
        for (auto const& child_box : boxes_to_place) {
            auto const& grid_column_start = child_box->computed_values().grid_column_start();
            auto const& grid_column_end = child_box->computed_values().grid_column_end();

            int column_span = 1;
            if (grid_column_start.is_span())
                column_span = grid_column_start.span();
            else if (grid_column_end.is_span())
                column_span = grid_column_end.span();

            if (column_span - 1 > m_occupation_grid.max_column_index())
                m_occupation_grid.set_max_column_index(column_span - 1);
        }
    }

    // 4. Position the remaining grid items.
    // For each grid item that hasn't been positioned by the previous steps, in order-modified document
    // order:
    auto auto_placement_cursor_x = 0;
    auto auto_placement_cursor_y = 0;
    for (auto key : keys) {
        auto& boxes_to_place = order_item_bucket.get(key).value();
        for (size_t i = 0; i < boxes_to_place.size(); i++) {
            auto const& child_box = boxes_to_place[i];
            // 4.1. For sparse packing:
            // FIXME: no distinction made. See #4.2

            // 4.1.1. If the item has a definite column position:
            if (!is_auto_positioned_column(child_box->computed_values().grid_column_start(), child_box->computed_values().grid_column_end()))
                place_item_with_column_position(child_box, auto_placement_cursor_x, auto_placement_cursor_y);

            // 4.1.2. If the item has an automatic grid position in both axes:
            else
                place_item_with_no_declared_position(child_box, auto_placement_cursor_x, auto_placement_cursor_y);

            boxes_to_place.remove(i);
            i--;

            // FIXME: 4.2. For dense packing:
        }
    }

    // NOTE: When final implicit grid sizes are known, we can offset their positions so leftmost grid track has 0 index.
    for (auto& item : m_grid_items) {
        item.row = item.row - m_occupation_grid.min_row_index();
        item.column = item.column - m_occupation_grid.min_column_index();
    }
}

void GridFormattingContext::determine_grid_container_height()
{
    CSSPixels total_y = 0;
    for (auto& grid_row : m_grid_rows_and_gaps)
        total_y += grid_row.base_size;
    m_automatic_content_height = total_y;
}

CSS::JustifyItems GridFormattingContext::justification_for_item(Box const& box) const
{
    switch (box.computed_values().justify_self()) {
    case CSS::JustifySelf::Auto:
        return grid_container().computed_values().justify_items();
    case CSS::JustifySelf::End:
        return CSS::JustifyItems::End;
    case CSS::JustifySelf::Normal:
        return CSS::JustifyItems::Normal;
    case CSS::JustifySelf::SelfStart:
        return CSS::JustifyItems::SelfStart;
    case CSS::JustifySelf::SelfEnd:
        return CSS::JustifyItems::SelfEnd;
    case CSS::JustifySelf::FlexStart:
        return CSS::JustifyItems::FlexStart;
    case CSS::JustifySelf::FlexEnd:
        return CSS::JustifyItems::FlexEnd;
    case CSS::JustifySelf::Center:
        return CSS::JustifyItems::Center;
    case CSS::JustifySelf::Baseline:
        return CSS::JustifyItems::Baseline;
    case CSS::JustifySelf::Start:
        return CSS::JustifyItems::Start;
    case CSS::JustifySelf::Stretch:
        return CSS::JustifyItems::Stretch;
    case CSS::JustifySelf::Safe:
        return CSS::JustifyItems::Safe;
    case CSS::JustifySelf::Unsafe:
        return CSS::JustifyItems::Unsafe;
    default:
        VERIFY_NOT_REACHED();
    }
}

CSS::AlignItems GridFormattingContext::alignment_for_item(Box const& box) const
{
    switch (box.computed_values().align_self()) {
    case CSS::AlignSelf::Auto:
        return grid_container().computed_values().align_items();
    case CSS::AlignSelf::End:
        return CSS::AlignItems::End;
    case CSS::AlignSelf::Normal:
        return CSS::AlignItems::Normal;
    case CSS::AlignSelf::SelfStart:
        return CSS::AlignItems::SelfStart;
    case CSS::AlignSelf::SelfEnd:
        return CSS::AlignItems::SelfEnd;
    case CSS::AlignSelf::FlexStart:
        return CSS::AlignItems::FlexStart;
    case CSS::AlignSelf::FlexEnd:
        return CSS::AlignItems::FlexEnd;
    case CSS::AlignSelf::Center:
        return CSS::AlignItems::Center;
    case CSS::AlignSelf::Baseline:
        return CSS::AlignItems::Baseline;
    case CSS::AlignSelf::Start:
        return CSS::AlignItems::Start;
    case CSS::AlignSelf::Stretch:
        return CSS::AlignItems::Stretch;
    case CSS::AlignSelf::Safe:
        return CSS::AlignItems::Safe;
    case CSS::AlignSelf::Unsafe:
        return CSS::AlignItems::Unsafe;
    default:
        VERIFY_NOT_REACHED();
    }
}

void GridFormattingContext::resolve_grid_item_widths()
{
    for (auto& item : m_grid_items) {
        CSSPixels containing_block_width = containing_block_size_for_item(item, GridDimension::Column);

        auto& box_state = m_state.get_mutable(item.box);

        auto const& computed_values = item.box->computed_values();
        auto const& computed_width = computed_values.width();

        struct ItemAlignment {
            CSSPixels margin_left;
            CSSPixels margin_right;
            CSSPixels width;
        };

        ItemAlignment initial {
            .margin_left = box_state.margin_left,
            .margin_right = box_state.margin_right,
            .width = box_state.content_width()
        };

        auto try_compute_width = [&](CSSPixels a_width) -> ItemAlignment {
            ItemAlignment result = initial;
            result.width = a_width;

            // Auto margins absorb positive free space prior to alignment via the box alignment properties.
            auto free_space_left_for_margins = containing_block_width - result.width - box_state.border_left - box_state.border_right - box_state.padding_left - box_state.padding_right - box_state.margin_left - box_state.margin_right;
            if (computed_values.margin().left().is_auto() && computed_values.margin().right().is_auto()) {
                result.margin_left = free_space_left_for_margins / 2;
                result.margin_right = free_space_left_for_margins / 2;
            } else if (computed_values.margin().left().is_auto()) {
                result.margin_left = free_space_left_for_margins;
            } else if (computed_values.margin().right().is_auto()) {
                result.margin_right = free_space_left_for_margins;
            } else if (computed_values.width().is_auto()) {
                result.width += free_space_left_for_margins;
            }

            auto free_space_left_for_alignment = containing_block_width - a_width - box_state.border_left - box_state.border_right - box_state.padding_left - box_state.padding_right - box_state.margin_left - box_state.margin_right;
            switch (justification_for_item(item.box)) {
            case CSS::JustifyItems::Normal:
            case CSS::JustifyItems::Stretch:
                break;
            case CSS::JustifyItems::Center:
                result.margin_left += free_space_left_for_alignment / 2;
                result.margin_right += free_space_left_for_alignment / 2;
                result.width = a_width;
                break;
            case CSS::JustifyItems::Start:
            case CSS::JustifyItems::FlexStart:
                result.margin_right += free_space_left_for_alignment;
                result.width = a_width;
                break;
            case CSS::JustifyItems::End:
            case CSS::JustifyItems::FlexEnd:
                result.margin_left += free_space_left_for_alignment;
                result.width = a_width;
                break;
            default:
                break;
            }

            return result;
        };

        ItemAlignment used_alignment;
        AvailableSpace available_space { AvailableSize::make_definite(containing_block_width), AvailableSize::make_indefinite() };
        if (computed_width.is_auto()) {
            used_alignment = try_compute_width(calculate_fit_content_width(item.box, available_space));
        } else if (computed_width.is_fit_content()) {
            used_alignment = try_compute_width(calculate_fit_content_width(item.box, available_space));
        } else {
            auto width_px = calculate_inner_width(item.box, available_space.width, computed_width).to_px(item.box);
            used_alignment = try_compute_width(width_px);
        }

        if (!should_treat_max_width_as_none(item.box, m_available_space->width)) {
            auto max_width_px = calculate_inner_width(item.box, available_space.width, computed_values.max_width()).to_px(item.box);
            auto max_width_alignment = try_compute_width(max_width_px);
            if (used_alignment.width > max_width_alignment.width) {
                used_alignment = max_width_alignment;
            }
        }

        if (!computed_values.min_width().is_auto()) {
            auto min_width_px = calculate_inner_width(item.box, available_space.width, computed_values.min_width()).to_px(item.box);
            auto min_width_alignment = try_compute_width(min_width_px);
            if (used_alignment.width < min_width_alignment.width) {
                used_alignment = min_width_alignment;
            }
        }

        box_state.margin_left = used_alignment.margin_left;
        box_state.margin_right = used_alignment.margin_right;
        box_state.set_content_width(used_alignment.width);
    }
}

void GridFormattingContext::resolve_grid_item_heights()
{
    for (auto& item : m_grid_items) {
        CSSPixels containing_block_height = containing_block_size_for_item(item, GridDimension::Row);

        auto& box_state = m_state.get_mutable(item.box);

        auto const& computed_values = item.box->computed_values();
        auto const& computed_height = computed_values.height();

        struct ItemAlignment {
            CSSPixels margin_top;
            CSSPixels margin_bottom;
            CSSPixels height;
        };

        ItemAlignment initial {
            .margin_top = box_state.margin_top,
            .margin_bottom = box_state.margin_bottom,
            .height = box_state.content_height()
        };

        auto try_compute_height = [&](CSSPixels a_height) -> ItemAlignment {
            ItemAlignment result = initial;
            result.height = a_height;

            CSSPixels height = a_height;
            auto underflow_px = containing_block_height - height - box_state.border_top - box_state.border_bottom - box_state.padding_top - box_state.padding_bottom - box_state.margin_top - box_state.margin_bottom;
            if (computed_values.margin().top().is_auto() && computed_values.margin().bottom().is_auto()) {
                auto half_of_the_underflow = underflow_px / 2;
                result.margin_top = half_of_the_underflow;
                result.margin_bottom = half_of_the_underflow;
            } else if (computed_values.margin().top().is_auto()) {
                result.margin_top = underflow_px;
            } else if (computed_values.margin().bottom().is_auto()) {
                result.margin_bottom = underflow_px;
            } else if (computed_values.height().is_auto()) {
                height += underflow_px;
            }

            switch (alignment_for_item(item.box)) {
            case CSS::AlignItems::Baseline:
                // FIXME: Not implemented
            case CSS::AlignItems::Stretch:
            case CSS::AlignItems::Normal:
                result.height = height;
                break;
            case CSS::AlignItems::Start:
            case CSS::AlignItems::FlexStart:
            case CSS::AlignItems::SelfStart:
                result.margin_bottom += underflow_px;
                break;
            case CSS::AlignItems::End:
            case CSS::AlignItems::SelfEnd:
            case CSS::AlignItems::FlexEnd:
                result.margin_top += underflow_px;
                break;
            case CSS::AlignItems::Center:
                result.margin_top += underflow_px / 2;
                result.margin_bottom += underflow_px / 2;
                break;
            default:
                break;
            }

            return result;
        };

        ItemAlignment used_alignment;
        if (computed_height.is_auto()) {
            used_alignment = try_compute_height(calculate_fit_content_height(item.box, get_available_space_for_item(item)));
        } else if (computed_height.is_fit_content()) {
            used_alignment = try_compute_height(calculate_fit_content_height(item.box, get_available_space_for_item(item)));
        } else {
            used_alignment = try_compute_height(computed_height.to_px(grid_container(), containing_block_height));
        }

        if (!should_treat_max_height_as_none(item.box, m_available_space->height)) {
            auto max_height_alignment = try_compute_height(computed_values.max_height().to_px(grid_container(), containing_block_height));
            if (used_alignment.height > max_height_alignment.height) {
                used_alignment = max_height_alignment;
            }
        }

        if (!computed_values.min_height().is_auto()) {
            auto min_height_alignment = try_compute_height(computed_values.min_height().to_px(grid_container(), containing_block_height));
            if (used_alignment.height < min_height_alignment.height) {
                used_alignment = min_height_alignment;
            }
        }

        box_state.margin_top = used_alignment.margin_top;
        box_state.margin_bottom = used_alignment.margin_bottom;
        box_state.set_content_height(used_alignment.height);
    }
}

void GridFormattingContext::resolve_items_box_metrics(GridDimension const dimension)
{
    for (auto& item : m_grid_items) {
        auto& box_state = m_state.get_mutable(item.box);
        auto& computed_values = item.box->computed_values();

        if (dimension == GridDimension::Column) {
            CSSPixels containing_block_width = containing_block_size_for_item(item, GridDimension::Column);

            box_state.padding_right = computed_values.padding().right().to_px(grid_container(), containing_block_width);
            box_state.padding_left = computed_values.padding().left().to_px(grid_container(), containing_block_width);

            box_state.margin_right = computed_values.margin().right().to_px(grid_container(), containing_block_width);
            box_state.margin_left = computed_values.margin().left().to_px(grid_container(), containing_block_width);

            box_state.border_right = computed_values.border_right().width;
            box_state.border_left = computed_values.border_left().width;
        } else {
            CSSPixels containing_block_height = containing_block_size_for_item(item, GridDimension::Row);

            box_state.padding_top = computed_values.padding().top().to_px(grid_container(), containing_block_height);
            box_state.padding_bottom = computed_values.padding().bottom().to_px(grid_container(), containing_block_height);

            box_state.margin_top = computed_values.margin().top().to_px(grid_container(), containing_block_height);
            box_state.margin_bottom = computed_values.margin().bottom().to_px(grid_container(), containing_block_height);

            box_state.border_top = computed_values.border_top().width;
            box_state.border_bottom = computed_values.border_bottom().width;
        }
    }
}

void GridFormattingContext::collapse_auto_fit_tracks_if_needed(GridDimension const dimension)
{
    // https://www.w3.org/TR/css-grid-2/#auto-repeat
    // The auto-fit keyword behaves the same as auto-fill, except that after grid item placement any
    // empty repeated tracks are collapsed. An empty track is one with no in-flow grid items placed into
    // or spanning across it. (This can result in all tracks being collapsed, if they’re all empty.)
    auto const& grid_computed_values = grid_container().computed_values();
    auto const& tracks_definition = dimension == GridDimension::Column ? grid_computed_values.grid_template_columns().track_list() : grid_computed_values.grid_template_rows().track_list();
    auto& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
    if (tracks_definition.size() == 1 && tracks_definition.first().is_repeat() && tracks_definition.first().repeat().is_auto_fit()) {
        for (size_t track_index = 0; track_index < tracks.size(); track_index++) {
            if (m_occupation_grid.is_occupied(dimension == GridDimension::Column ? track_index : 0, dimension == GridDimension::Row ? track_index : 0))
                continue;

            // NOTE: A collapsed track is treated as having a fixed track sizing function of 0px
            tracks[track_index].min_track_sizing_function = CSS::GridSize(CSS::Length::make_px(0));
            tracks[track_index].max_track_sizing_function = CSS::GridSize(CSS::Length::make_px(0));
        }
    }
}

CSSPixelRect GridFormattingContext::get_grid_area_rect(GridItem const& grid_item) const
{
    auto const& row_gap = grid_container().computed_values().row_gap();
    auto resolved_row_span = row_gap.is_auto() ? grid_item.row_span : grid_item.row_span * 2;
    if (!row_gap.is_auto() && grid_item.gap_adjusted_row(grid_container()) == 0)
        resolved_row_span -= 1;
    if (grid_item.gap_adjusted_row(grid_container()) + resolved_row_span > m_grid_rows.size())
        resolved_row_span = m_grid_rows_and_gaps.size() - grid_item.gap_adjusted_row(grid_container());

    auto const& column_gap = grid_container().computed_values().column_gap();
    auto resolved_column_span = column_gap.is_auto() ? grid_item.column_span : grid_item.column_span * 2;
    if (!column_gap.is_auto() && grid_item.gap_adjusted_column(grid_container()) == 0)
        resolved_column_span -= 1;
    if (grid_item.gap_adjusted_column(grid_container()) + resolved_column_span > m_grid_columns_and_gaps.size())
        resolved_column_span = m_grid_columns_and_gaps.size() - grid_item.gap_adjusted_column(grid_container());

    int row_start = grid_item.gap_adjusted_row(grid_container());
    int row_end = grid_item.gap_adjusted_row(grid_container()) + resolved_row_span;
    int column_start = grid_item.gap_adjusted_column(grid_container());
    int column_end = grid_item.gap_adjusted_column(grid_container()) + resolved_column_span;

    CSSPixels x_start = 0;
    CSSPixels x_end = 0;
    CSSPixels y_start = 0;
    CSSPixels y_end = 0;
    for (int i = 0; i < column_start; i++)
        x_start += m_grid_columns_and_gaps[i].base_size;
    for (int i = 0; i < column_end; i++)
        x_end += m_grid_columns_and_gaps[i].base_size;
    for (int i = 0; i < row_start; i++)
        y_start += m_grid_rows_and_gaps[i].base_size;
    for (int i = 0; i < row_end; i++) {
        y_end += m_grid_rows_and_gaps[i].base_size;
    }

    return { x_start, y_start, x_end - x_start, y_end - y_start };
}

void GridFormattingContext::run(Box const&, LayoutMode, AvailableSpace const& available_space)
{
    m_available_space = available_space;

    auto const& grid_computed_values = grid_container().computed_values();

    // NOTE: We store explicit grid sizes to later use in determining the position of items with negative index.
    m_explicit_columns_line_count = get_count_of_tracks(grid_computed_values.grid_template_columns().track_list(), available_space) + 1;
    m_explicit_rows_line_count = get_count_of_tracks(grid_computed_values.grid_template_rows().track_list(), available_space) + 1;

    place_grid_items(available_space);

    initialize_grid_tracks_for_columns_and_rows(available_space);

    initialize_gap_tracks(available_space);

    collapse_auto_fit_tracks_if_needed(GridDimension::Column);
    collapse_auto_fit_tracks_if_needed(GridDimension::Row);

    for (auto& item : m_grid_items) {
        auto& box_state = m_state.get_mutable(item.box);
        auto& computed_values = item.box->computed_values();

        // NOTE: As the containing blocks of grid items are created by implicit grid areas that are not present in the
        // layout tree, the initial value of has_definite_width/height computed by LayoutState::UsedValues::set_node
        // will be incorrect for anything other (auto, percentage, calculated) than fixed lengths.
        // Therefor, it becomes necessary to reset this value to indefinite.
        // TODO: Handle this in LayoutState::UsedValues::set_node
        if (!computed_values.width().is_length())
            box_state.set_indefinite_content_width();
        if (!computed_values.height().is_length())
            box_state.set_indefinite_content_height();

        if (item.box->is_replaced_box()) {
            auto& replaced_box = static_cast<Layout::ReplacedBox const&>(*item.box);
            // FIXME: This const_cast is gross.
            const_cast<Layout::ReplacedBox&>(replaced_box).prepare_for_replaced_layout();
        }
    }

    // Do the first pass of resolving grid items box metrics to compute values that are independent of a track width
    resolve_items_box_metrics(GridDimension::Column);

    run_track_sizing(available_space, GridDimension::Column);

    // Do the second pass of resolving box metrics to compute values that depend on a track width
    resolve_items_box_metrics(GridDimension::Column);

    // Once the sizes of column tracks, which determine the widths of the grid areas forming the containing blocks
    // for grid items, ara calculated, it becomes possible to determine the final widths of the grid items.
    resolve_grid_item_widths();

    // Do the first pass of resolving grid items box metrics to compute values that are independent of a track height
    resolve_items_box_metrics(GridDimension::Row);

    run_track_sizing(available_space, GridDimension::Row);

    // Do the second pass of resolving box metrics to compute values that depend on a track height
    resolve_items_box_metrics(GridDimension::Row);

    resolve_grid_item_heights();

    determine_grid_container_height();

    auto const& containing_block_state = m_state.get(*grid_container().containing_block());
    auto height_of_containing_block = containing_block_state.content_height();
    auto min_height = grid_container().computed_values().min_height().to_px(grid_container(), height_of_containing_block);

    // If automatic grid container height is less than min-height, we need to re-run the track sizing algorithm
    if (m_automatic_content_height < min_height) {
        resolve_items_box_metrics(GridDimension::Row);

        AvailableSize width(available_space.width);
        AvailableSize height(AvailableSize::make_definite(min_height));
        run_track_sizing(AvailableSpace(width, height), GridDimension::Row);

        resolve_items_box_metrics(GridDimension::Row);

        resolve_grid_item_heights();

        determine_grid_container_height();
    }

    if (available_space.height.is_intrinsic_sizing_constraint() || available_space.width.is_intrinsic_sizing_constraint()) {
        determine_intrinsic_size_of_grid_container(available_space);
        return;
    }

    for (auto& grid_item : m_grid_items) {
        auto& grid_item_box_state = m_state.get_mutable(grid_item.box);
        CSSPixelPoint margin_offset = { grid_item_box_state.margin_box_left(), grid_item_box_state.margin_box_top() };
        grid_item_box_state.offset = get_grid_area_rect(grid_item).top_left() + margin_offset;
        compute_inset(grid_item.box);

        auto available_space_for_children = AvailableSpace(AvailableSize::make_definite(grid_item_box_state.content_width()), AvailableSize::make_definite(grid_item_box_state.content_height()));
        if (auto independent_formatting_context = layout_inside(grid_item.box, LayoutMode::Normal, available_space_for_children))
            independent_formatting_context->parent_context_did_dimension_child_root_box();
    }
}

void GridFormattingContext::layout_absolutely_positioned_element(Box const& box, AvailableSpace const& available_space)
{
    auto& containing_block_state = m_state.get_mutable(*box.containing_block());
    auto& box_state = m_state.get_mutable(box);
    auto const& computed_values = box.computed_values();

    auto const& grid_row_start = computed_values.grid_row_start();
    auto const& grid_row_end = computed_values.grid_row_end();
    auto const& grid_column_start = computed_values.grid_column_start();
    auto const& grid_column_end = computed_values.grid_column_end();

    int row_start = 0, row_end = 0, column_start = 0, column_end = 0;

    if (grid_column_end.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_column_end.identifier()); maybe_grid_area.has_value())
            column_end = maybe_grid_area->column_end;
        else if (auto line_name_index = get_line_index_by_line_name(grid_column_end.identifier(), grid_container().computed_values().grid_template_columns()); line_name_index > -1)
            column_end = line_name_index;
        else
            column_end = 1;
        column_start = column_end - 1;
    }

    if (grid_column_start.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_column_start.identifier()); maybe_grid_area.has_value())
            column_start = maybe_grid_area->column_start;
        else if (auto line_name_index = get_line_index_by_line_name(grid_column_start.identifier(), grid_container().computed_values().grid_template_columns()); line_name_index > -1)
            column_start = line_name_index;
        else
            column_start = 0;
    }

    if (grid_row_end.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_row_end.identifier()); maybe_grid_area.has_value())
            row_end = maybe_grid_area->row_end;
        else if (auto line_name_index = get_line_index_by_line_name(grid_row_end.identifier(), grid_container().computed_values().grid_template_rows()); line_name_index > -1)
            row_end = line_name_index;
        else
            row_end = 1;
        row_start = row_end - 1;
    }

    if (grid_row_start.has_identifier()) {
        if (auto maybe_grid_area = m_grid_areas.get(grid_row_start.identifier()); maybe_grid_area.has_value())
            row_start = maybe_grid_area->row_start;
        else if (auto line_name_index = get_line_index_by_line_name(grid_row_start.identifier(), grid_container().computed_values().grid_template_rows()); line_name_index > -1)
            row_start = line_name_index;
        else
            row_start = 0;
    }

    size_t row_span = row_end - row_start;
    size_t column_span = column_end - column_start;

    GridItem item { box, row_start, row_span, column_start, column_span };

    // The border computed values are not changed by the compute_height & width calculations below.
    // The spec only adjusts and computes sizes, insets and margins.
    box_state.border_left = box.computed_values().border_left().width;
    box_state.border_right = box.computed_values().border_right().width;
    box_state.border_top = box.computed_values().border_top().width;
    box_state.border_bottom = box.computed_values().border_bottom().width;

    compute_width_for_absolutely_positioned_element(box, available_space);

    // NOTE: We compute height before *and* after doing inside layout.
    //       This is done so that inside layout can resolve percentage heights.
    //       In some situations, e.g with non-auto top & bottom values, the height can be determined early.
    compute_height_for_absolutely_positioned_element(box, available_space, BeforeOrAfterInsideLayout::Before);

    auto independent_formatting_context = layout_inside(box, LayoutMode::Normal, box_state.available_inner_space_or_constraints_from(available_space));

    compute_height_for_absolutely_positioned_element(box, available_space, BeforeOrAfterInsideLayout::After);

    if (computed_values.inset().left().is_auto() && computed_values.inset().right().is_auto()) {
        auto containing_block_width = containing_block_state.content_width();
        auto width_left_for_alignment = containing_block_width - box_state.margin_box_width();
        switch (justification_for_item(box)) {
        case CSS::JustifyItems::Normal:
        case CSS::JustifyItems::Stretch:
            break;
        case CSS::JustifyItems::Center:
            box_state.inset_left = width_left_for_alignment / 2;
            box_state.inset_right = width_left_for_alignment / 2;
            break;
        case CSS::JustifyItems::Start:
        case CSS::JustifyItems::FlexStart:
            box_state.inset_right = width_left_for_alignment;
            break;
        case CSS::JustifyItems::End:
        case CSS::JustifyItems::FlexEnd:
            box_state.inset_left = width_left_for_alignment;
            break;
        default:
            break;
        }
    }

    if (computed_values.inset().top().is_auto() && computed_values.inset().bottom().is_auto()) {
        auto containing_block_height = containing_block_state.content_height();
        auto height_left_for_alignment = containing_block_height - box_state.margin_box_height();
        switch (alignment_for_item(box)) {
        case CSS::AlignItems::Baseline:
            // FIXME: Not implemented
        case CSS::AlignItems::Stretch:
        case CSS::AlignItems::Normal:
            break;
        case CSS::AlignItems::Start:
        case CSS::AlignItems::FlexStart:
        case CSS::AlignItems::SelfStart:
            box_state.inset_bottom = height_left_for_alignment;
            break;
        case CSS::AlignItems::End:
        case CSS::AlignItems::SelfEnd:
        case CSS::AlignItems::FlexEnd: {
            box_state.inset_top = height_left_for_alignment;
            break;
        }
        case CSS::AlignItems::Center:
            box_state.inset_top = height_left_for_alignment / 2;
            box_state.inset_bottom = height_left_for_alignment / 2;
            break;
        default:
            break;
        }
    }

    // If an absolutely positioned element’s containing block is generated by a grid container,
    // the containing block corresponds to the grid area determined by its grid-placement properties.
    // The offset properties (top/right/bottom/left) then indicate offsets inwards from the corresponding
    // edges of this containing block, as normal.
    CSSPixelPoint used_offset;
    auto grid_area_offset = get_grid_area_rect(item);
    used_offset.set_x(grid_area_offset.x() + box_state.inset_left + box_state.margin_box_left());
    used_offset.set_y(grid_area_offset.y() + box_state.inset_top + box_state.margin_box_top());

    // NOTE: Absolutely positioned boxes are relative to the *padding edge* of the containing block.
    used_offset.translate_by(-containing_block_state.padding_left, -containing_block_state.padding_top);
    box_state.set_content_offset(used_offset);

    if (independent_formatting_context)
        independent_formatting_context->parent_context_did_dimension_child_root_box();
}

void GridFormattingContext::parent_context_did_dimension_child_root_box()
{
    grid_container().for_each_child_of_type<Box>([&](Layout::Box& box) {
        if (box.is_absolutely_positioned()) {
            auto& cb_state = m_state.get(*box.containing_block());
            auto available_width = AvailableSize::make_definite(cb_state.content_width() + cb_state.padding_left + cb_state.padding_right);
            auto available_height = AvailableSize::make_definite(cb_state.content_height() + cb_state.padding_top + cb_state.padding_bottom);
            layout_absolutely_positioned_element(box, AvailableSpace(available_width, available_height));
        }
    });
}

void GridFormattingContext::determine_intrinsic_size_of_grid_container(AvailableSpace const& available_space)
{
    // https://www.w3.org/TR/css-grid-1/#intrinsic-sizes
    // The max-content size (min-content size) of a grid container is the sum of the grid container’s track sizes
    // (including gutters) in the appropriate axis, when the grid is sized under a max-content constraint (min-content constraint).

    if (available_space.height.is_intrinsic_sizing_constraint()) {
        CSSPixels grid_container_height = 0;
        for (auto& track : m_grid_rows) {
            grid_container_height += track.base_size;
        }
        m_state.get_mutable(grid_container()).set_content_height(grid_container_height);
    }

    if (available_space.width.is_intrinsic_sizing_constraint()) {
        CSSPixels grid_container_width = 0;
        for (auto& track : m_grid_columns) {
            grid_container_width += track.base_size;
        }
        m_state.get_mutable(grid_container()).set_content_width(grid_container_width);
    }
}

CSSPixels GridFormattingContext::automatic_content_width() const
{
    return m_state.get(grid_container()).content_width();
}

CSSPixels GridFormattingContext::automatic_content_height() const
{
    return m_automatic_content_height;
}

bool GridFormattingContext::is_auto_positioned_row(CSS::GridTrackPlacement const& grid_row_start, CSS::GridTrackPlacement const& grid_row_end) const
{
    return is_auto_positioned_track(grid_row_start, grid_row_end);
}

bool GridFormattingContext::is_auto_positioned_column(CSS::GridTrackPlacement const& grid_column_start, CSS::GridTrackPlacement const& grid_column_end) const
{
    return is_auto_positioned_track(grid_column_start, grid_column_end);
}

bool GridFormattingContext::is_auto_positioned_track(CSS::GridTrackPlacement const& grid_track_start, CSS::GridTrackPlacement const& grid_track_end) const
{
    return grid_track_start.is_auto_positioned() && grid_track_end.is_auto_positioned();
}

AvailableSize GridFormattingContext::get_free_space(AvailableSpace const& available_space, GridDimension const dimension) const
{
    // https://www.w3.org/TR/css-grid-2/#algo-terms
    // free space: Equal to the available grid space minus the sum of the base sizes of all the grid
    // tracks (including gutters), floored at zero. If available grid space is indefinite, the free
    // space is indefinite as well.
    auto& available_size = dimension == GridDimension::Column ? available_space.width : available_space.height;
    auto& tracks = dimension == GridDimension::Column ? m_grid_columns_and_gaps : m_grid_rows_and_gaps;
    if (available_size.is_definite()) {
        CSSPixels sum_base_sizes = 0;
        for (auto& track : tracks)
            sum_base_sizes += track.base_size;
        return AvailableSize::make_definite(max(CSSPixels(0), available_size.to_px_or_zero() - sum_base_sizes));
    }

    return available_size;
}

int GridFormattingContext::get_line_index_by_line_name(String const& needle, CSS::GridTrackSizeList grid_track_size_list)
{
    if (grid_track_size_list.track_list().size() == 0)
        return -1;

    auto repeated_tracks_count = 0;
    for (size_t x = 0; x < grid_track_size_list.track_list().size(); x++) {
        if (grid_track_size_list.track_list()[x].is_repeat()) {
            // FIXME: Calculate amount of columns/rows if auto-fill/fit
            if (!grid_track_size_list.track_list()[x].repeat().is_default())
                return -1;
            auto repeat = grid_track_size_list.track_list()[x].repeat().grid_track_size_list();
            for (size_t y = 0; y < repeat.track_list().size(); y++) {
                for (size_t z = 0; z < repeat.line_names()[y].size(); z++) {
                    if (repeat.line_names()[y][z] == needle)
                        return x + repeated_tracks_count;
                    repeated_tracks_count++;
                }
            }
        } else {
            for (size_t y = 0; y < grid_track_size_list.line_names()[x].size(); y++) {
                if (grid_track_size_list.line_names()[x][y] == needle)
                    return x + repeated_tracks_count;
            }
        }
    }
    for (size_t y = 0; y < grid_track_size_list.line_names()[grid_track_size_list.track_list().size()].size(); y++) {
        if (grid_track_size_list.line_names()[grid_track_size_list.track_list().size()][y] == needle)
            return grid_track_size_list.track_list().size() + repeated_tracks_count;
    }
    return -1;
}

void OccupationGrid::set_occupied(int column_start, int column_end, int row_start, int row_end)
{
    for (int row_index = row_start; row_index < row_end; row_index++) {
        for (int column_index = column_start; column_index < column_end; column_index++) {
            m_min_column_index = min(m_min_column_index, column_index);
            m_max_column_index = max(m_max_column_index, column_index);
            m_min_row_index = min(m_min_row_index, row_index);
            m_max_row_index = max(m_max_row_index, row_index);

            m_occupation_grid.set(GridPosition { .row = row_index, .column = column_index });
        }
    }
}

bool OccupationGrid::is_occupied(int column_index, int row_index) const
{
    return m_occupation_grid.contains(GridPosition { row_index, column_index });
}

int GridItem::gap_adjusted_row(Box const& grid_box) const
{
    return grid_box.computed_values().row_gap().is_auto() ? row : row * 2;
}

int GridItem::gap_adjusted_column(Box const& grid_box) const
{
    return grid_box.computed_values().column_gap().is_auto() ? column : column * 2;
}

CSS::Size const& GridFormattingContext::get_item_preferred_size(GridItem const& item, GridDimension const dimension) const
{
    if (dimension == GridDimension::Column)
        return item.box->computed_values().width();
    return item.box->computed_values().height();
}

CSSPixels GridFormattingContext::calculate_min_content_size(GridItem const& item, GridDimension const dimension) const
{
    if (dimension == GridDimension::Column) {
        return calculate_min_content_width(item.box);
    } else {
        return calculate_min_content_height(item.box, get_available_space_for_item(item).width.to_px_or_zero());
    }
}

CSSPixels GridFormattingContext::calculate_max_content_size(GridItem const& item, GridDimension const dimension) const
{
    if (dimension == GridDimension::Column) {
        return calculate_max_content_width(item.box);
    } else {
        return calculate_max_content_height(item.box, get_available_space_for_item(item).width.to_px_or_zero());
    }
}

CSSPixels GridFormattingContext::containing_block_size_for_item(GridItem const& item, GridDimension const dimension) const
{
    CSSPixels containing_block_size = 0;
    for_each_spanned_track_by_item(item, dimension, [&](GridTrack const& track) {
        containing_block_size += track.base_size;
    });
    return containing_block_size;
}

AvailableSpace GridFormattingContext::get_available_space_for_item(GridItem const& item) const
{
    auto& item_box_state = m_state.get(item.box);
    AvailableSize available_width = item_box_state.has_definite_width() ? AvailableSize::make_definite(item_box_state.content_width()) : AvailableSize::make_indefinite();
    AvailableSize available_height = item_box_state.has_definite_height() ? AvailableSize::make_definite(item_box_state.content_height()) : AvailableSize::make_indefinite();
    return AvailableSpace(available_width, available_height);
}

static CSS::Size const& get_item_minimum_size(GridItem const& item, GridDimension const dimension)
{
    if (dimension == GridDimension::Column)
        return item.box->computed_values().min_width();
    return item.box->computed_values().min_height();
}

static CSS::Size const& get_item_maximum_size(GridItem const& item, GridDimension const dimension)
{
    if (dimension == GridDimension::Column)
        return item.box->computed_values().max_width();
    return item.box->computed_values().max_height();
}

CSSPixels GridFormattingContext::calculate_min_content_contribution(GridItem const& item, GridDimension const dimension) const
{
    auto available_space_for_item = get_available_space_for_item(item);

    auto should_treat_preferred_size_as_auto = [&] {
        if (dimension == GridDimension::Column)
            return should_treat_width_as_auto(item.box, available_space_for_item);
        return should_treat_height_as_auto(item.box, available_space_for_item);
    }();

    auto maxium_size = CSSPixels::max();
    if (auto const& css_maximum_size = get_item_maximum_size(item, dimension); css_maximum_size.is_length()) {
        maxium_size = css_maximum_size.length().to_px(item.box);
    }

    if (should_treat_preferred_size_as_auto) {
        auto result = item.add_margin_box_sizes(calculate_min_content_size(item, dimension), dimension, m_state);
        return min(result, maxium_size);
    }

    auto preferred_size = get_item_preferred_size(item, dimension);
    auto containing_block_size = containing_block_size_for_item(item, dimension);
    auto result = item.add_margin_box_sizes(preferred_size.to_px(grid_container(), containing_block_size), dimension, m_state);
    return min(result, maxium_size);
}

CSSPixels GridFormattingContext::calculate_max_content_contribution(GridItem const& item, GridDimension const dimension) const
{
    auto available_space_for_item = get_available_space_for_item(item);

    auto should_treat_preferred_size_as_auto = [&] {
        if (dimension == GridDimension::Column)
            return should_treat_width_as_auto(item.box, available_space_for_item);
        return should_treat_height_as_auto(item.box, available_space_for_item);
    }();

    auto maxium_size = CSSPixels::max();
    if (auto const& css_maximum_size = get_item_maximum_size(item, dimension); css_maximum_size.is_length()) {
        maxium_size = css_maximum_size.length().to_px(item.box);
    }

    auto preferred_size = get_item_preferred_size(item, dimension);
    if (should_treat_preferred_size_as_auto || preferred_size.is_fit_content()) {
        auto fit_content_size = dimension == GridDimension::Column ? calculate_fit_content_width(item.box, available_space_for_item) : calculate_fit_content_height(item.box, available_space_for_item);
        auto result = item.add_margin_box_sizes(fit_content_size, dimension, m_state);
        return min(result, maxium_size);
    }

    auto containing_block_size = containing_block_size_for_item(item, dimension);
    auto result = item.add_margin_box_sizes(preferred_size.to_px(grid_container(), containing_block_size), dimension, m_state);
    return min(result, maxium_size);
}

CSSPixels GridFormattingContext::calculate_limited_min_content_contribution(GridItem const& item, GridDimension const dimension) const
{
    // The limited min-content contribution of an item is its min-content contribution,
    // limited by the max track sizing function (which could be the argument to a fit-content() track
    // sizing function) if that is fixed and ultimately floored by its minimum contribution.
    // FIXME: limit by max track sizing function
    auto min_content_contribution = calculate_min_content_contribution(item, dimension);
    auto minimum_contribution = calculate_minimum_contribution(item, dimension);
    if (min_content_contribution < minimum_contribution)
        return minimum_contribution;
    return min_content_contribution;
}

CSSPixels GridFormattingContext::calculate_limited_max_content_contribution(GridItem const& item, GridDimension const dimension) const
{
    // The limited max-content contribution of an item is its max-content contribution,
    // limited by the max track sizing function (which could be the argument to a fit-content() track
    // sizing function) if that is fixed and ultimately floored by its minimum contribution.
    // FIXME: limit by max track sizing function
    auto max_content_contribution = calculate_max_content_contribution(item, dimension);
    auto minimum_contribution = calculate_minimum_contribution(item, dimension);
    if (max_content_contribution < minimum_contribution)
        return minimum_contribution;
    return max_content_contribution;
}

CSSPixels GridFormattingContext::content_size_suggestion(GridItem const& item, GridDimension const dimension) const
{
    // The content size suggestion is the min-content size in the relevant axis
    // FIXME: clamped, if it has a preferred aspect ratio, by any definite opposite-axis minimum and maximum sizes
    // converted through the aspect ratio.
    return calculate_min_content_size(item, dimension);
}

Optional<CSSPixels> GridFormattingContext::specified_size_suggestion(GridItem const& item, GridDimension const dimension) const
{
    // https://www.w3.org/TR/css-grid-1/#specified-size-suggestion
    // If the item’s preferred size in the relevant axis is definite, then the specified size suggestion is that size.
    // It is otherwise undefined.
    auto const& used_values = m_state.get(item.box);
    auto has_definite_preferred_size = dimension == GridDimension::Column ? used_values.has_definite_width() : used_values.has_definite_height();
    if (has_definite_preferred_size) {
        // FIXME: consider margins, padding and borders because it is outer size.
        auto containing_block_size = containing_block_size_for_item(item, dimension);
        return get_item_preferred_size(item, dimension).to_px(item.box, containing_block_size);
    }

    return {};
}

CSSPixels GridFormattingContext::content_based_minimum_size(GridItem const& item, GridDimension const dimension) const
{
    // https://www.w3.org/TR/css-grid-1/#content-based-minimum-size
    // The content-based minimum size for a grid item in a given dimension is its specified size suggestion if it exists,
    // otherwise its transferred size suggestion if that exists,
    // else its content size suggestion, see below.
    // In all cases, the size suggestion is additionally clamped by the maximum size in the affected axis, if it’s definite.

    auto maximum_size = CSSPixels::max();
    if (auto const& css_maximum_size = get_item_maximum_size(item, dimension); css_maximum_size.is_length()) {
        maximum_size = css_maximum_size.length().to_px(item.box);
    }

    if (auto specified_size_suggestion = this->specified_size_suggestion(item, dimension); specified_size_suggestion.has_value()) {
        return min(specified_size_suggestion.value(), maximum_size);
    }

    return min(content_size_suggestion(item, dimension), maximum_size);
}

CSSPixels GridFormattingContext::automatic_minimum_size(GridItem const& item, GridDimension const dimension) const
{
    // To provide a more reasonable default minimum size for grid items, the used value of its automatic minimum size
    // in a given axis is the content-based minimum size if all of the following are true:
    // - it is not a scroll container
    // - it spans at least one track in that axis whose min track sizing function is auto
    // FIXME: - if it spans more than one track in that axis, none of those tracks are flexible
    auto const& tracks = dimension == GridDimension::Column ? m_grid_columns : m_grid_rows;
    auto item_track_index = item.raw_position(dimension);

    // FIXME: Check all tracks spanned by an item
    AvailableSize const& available_size = dimension == GridDimension::Column ? m_available_space->width : m_available_space->height;
    auto item_spans_auto_tracks = tracks[item_track_index].min_track_sizing_function.is_auto(available_size);
    if (item_spans_auto_tracks && !item.box->is_scroll_container()) {
        return content_based_minimum_size(item, dimension);
    }

    // Otherwise, the automatic minimum size is zero, as usual.
    return 0;
}

CSSPixels GridFormattingContext::calculate_minimum_contribution(GridItem const& item, GridDimension const dimension) const
{
    // The minimum contribution of an item is the smallest outer size it can have.
    // Specifically, if the item’s computed preferred size behaves as auto or depends on the size of its
    // containing block in the relevant axis, its minimum contribution is the outer size that would
    // result from assuming the item’s used minimum size as its preferred size; else the item’s minimum
    // contribution is its min-content contribution. Because the minimum contribution often depends on
    // the size of the item’s content, it is considered a type of intrinsic size contribution.

    auto preferred_size = get_item_preferred_size(item, dimension);
    auto should_treat_preferred_size_as_auto = [&] {
        if (dimension == GridDimension::Column)
            return should_treat_width_as_auto(item.box, get_available_space_for_item(item));
        return should_treat_height_as_auto(item.box, get_available_space_for_item(item));
    }();

    if (should_treat_preferred_size_as_auto) {
        auto minimum_size = get_item_minimum_size(item, dimension);
        if (minimum_size.is_auto())
            return item.add_margin_box_sizes(automatic_minimum_size(item, dimension), dimension, m_state);
        auto containing_block_size = containing_block_size_for_item(item, dimension);
        return item.add_margin_box_sizes(minimum_size.to_px(grid_container(), containing_block_size), dimension, m_state);
    }

    return calculate_min_content_contribution(item, dimension);
}

}

namespace AK {
template<>
struct Traits<Web::Layout::GridPosition> : public DefaultTraits<Web::Layout::GridPosition> {
    static unsigned hash(Web::Layout::GridPosition const& key) { return pair_int_hash(key.row, key.column); }
};
}
