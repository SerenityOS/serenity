/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Node.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLTableCellElement.h>
#include <LibWeb/HTML/HTMLTableColElement.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/TableFormattingContext.h>

struct GridPosition {
    size_t x;
    size_t y;
};

inline bool operator==(GridPosition const& a, GridPosition const& b)
{
    return a.x == b.x && a.y == b.y;
}

namespace AK {
template<>
struct Traits<GridPosition> : public GenericTraits<GridPosition> {
    static unsigned hash(GridPosition const& key)
    {
        return pair_int_hash(key.x, key.y);
    }
};
}

namespace Web::Layout {

TableFormattingContext::TableFormattingContext(LayoutState& state, Box const& root, FormattingContext* parent)
    : FormattingContext(Type::Table, state, root, parent)
{
}

TableFormattingContext::~TableFormattingContext() = default;

static inline bool is_table_row_group(Box const& box)
{
    auto const& display = box.display();
    return display.is_table_row_group() || display.is_table_header_group() || display.is_table_footer_group();
}

static inline bool is_table_row(Box const& box)
{
    return box.display().is_table_row();
}

template<typename Matcher, typename Callback>
static void for_each_child_box_matching(Box const& parent, Matcher matcher, Callback callback)
{
    parent.for_each_child_of_type<Box>([&](Box const& child_box) {
        if (matcher(child_box))
            callback(child_box);
    });
}

CSSPixels TableFormattingContext::run_caption_layout(LayoutMode layout_mode, CSS::CaptionSide phase)
{
    CSSPixels caption_height = 0;
    for (auto* child = table_box().first_child(); child; child = child->next_sibling()) {
        if (!child->display().is_table_caption() || child->computed_values().caption_side() != phase) {
            continue;
        }
        // The caption boxes are principal block-level boxes that retain their own content, padding, margin, and border areas,
        // and are rendered as normal block boxes inside the table wrapper box, as described in https://www.w3.org/TR/CSS22/tables.html#model
        auto caption_context = make<BlockFormattingContext>(m_state, *verify_cast<BlockContainer>(child), this);
        caption_context->run(table_box(), layout_mode, *m_available_space);
        VERIFY(child->is_box());
        auto const& child_box = static_cast<Box const&>(*child);
        // FIXME: Since caption only has inline children, BlockFormattingContext doesn't resolve the vertical metrics.
        //        We need to do it manually here.
        caption_context->resolve_vertical_box_model_metrics(child_box);
        auto const& caption_state = m_state.get(child_box);
        if (phase == CSS::CaptionSide::Top) {
            m_state.get_mutable(table_box()).set_content_y(caption_state.margin_box_height());
        } else {
            m_state.get_mutable(child_box).set_content_y(
                m_state.get(table_box()).margin_box_height() + caption_state.margin_box_top());
        }
        caption_height += caption_state.margin_box_height();
    }
    return caption_height;
}

void TableFormattingContext::calculate_row_column_grid(Box const& box)
{
    // Implements https://html.spec.whatwg.org/multipage/tables.html#forming-a-table
    HashMap<GridPosition, bool> grid;

    size_t x_width = 0, y_height = 0;
    size_t x_current = 0, y_current = 0;

    // Implements https://html.spec.whatwg.org/multipage/tables.html#algorithm-for-processing-rows
    auto process_row = [&](auto& row) {
        if (y_height == y_current)
            y_height++;

        x_current = 0;

        for (auto* child = row.first_child(); child; child = child->next_sibling()) {
            if (child->display().is_table_cell()) {
                // Cells: While x_current is less than x_width and the slot with coordinate (x_current, y_current) already has a cell assigned to it, increase x_current by 1.
                while (x_current < x_width && grid.contains(GridPosition { x_current, y_current }))
                    x_current++;

                Box const* box = static_cast<Box const*>(child);
                if (x_current == x_width)
                    x_width++;

                size_t colspan = 1, rowspan = 1;
                if (box->dom_node() && is<HTML::HTMLTableCellElement>(*box->dom_node())) {
                    auto const& node = static_cast<HTML::HTMLTableCellElement const&>(*box->dom_node());
                    colspan = node.col_span();
                    rowspan = node.row_span();
                }

                if (x_width < x_current + colspan)
                    x_width = x_current + colspan;
                if (y_height < y_current + rowspan)
                    y_height = y_current + rowspan;

                for (size_t y = y_current; y < y_current + rowspan; y++)
                    for (size_t x = x_current; x < x_current + colspan; x++)
                        grid.set(GridPosition { x, y }, true);
                m_cells.append(Cell { *box, x_current, y_current, colspan, rowspan });

                x_current += colspan;
            }
        }

        m_rows.append(Row { row });
        y_current++;
    };

    for_each_child_box_matching(box, is_table_row_group, [&](auto& row_group_box) {
        for_each_child_box_matching(row_group_box, is_table_row, [&](auto& row_box) {
            process_row(row_box);
            return IterationDecision::Continue;
        });
    });

    for_each_child_box_matching(box, is_table_row, [&](auto& row_box) {
        process_row(row_box);
        return IterationDecision::Continue;
    });

    m_columns.resize(x_width);

    for (auto& cell : m_cells) {
        // Clip spans to the end of the table.
        cell.row_span = min(cell.row_span, m_rows.size() - cell.row_index);
        cell.column_span = min(cell.column_span, m_columns.size() - cell.column_index);
    }
}

void TableFormattingContext::compute_cell_measures(AvailableSpace const& available_space)
{
    auto const& containing_block = m_state.get(*table_wrapper().containing_block());
    auto table_width_is_auto = table_box().computed_values().width().is_auto();

    for (auto& cell : m_cells) {
        auto const& computed_values = cell.box->computed_values();
        CSSPixels padding_top = computed_values.padding().top().to_px(cell.box, containing_block.content_height());
        CSSPixels padding_bottom = computed_values.padding().bottom().to_px(cell.box, containing_block.content_height());
        CSSPixels padding_left = computed_values.padding().left().to_px(cell.box, containing_block.content_width());
        CSSPixels padding_right = computed_values.padding().right().to_px(cell.box, containing_block.content_width());

        auto const& cell_state = m_state.get(cell.box);
        auto is_collapse = cell.box->computed_values().border_collapse() == CSS::BorderCollapse::Collapse;
        CSSPixels border_top = is_collapse ? cell_state.border_top : computed_values.border_top().width;
        CSSPixels border_bottom = is_collapse ? cell_state.border_bottom : computed_values.border_bottom().width;
        CSSPixels border_left = is_collapse ? cell_state.border_left : computed_values.border_left().width;
        CSSPixels border_right = is_collapse ? cell_state.border_right : computed_values.border_right().width;

        auto height = computed_values.height().to_px(cell.box, containing_block.content_height());
        auto width = (computed_values.width().is_length() || !table_width_is_auto) ? computed_values.width().to_px(cell.box, containing_block.content_width()) : 0;
        auto min_content_height = calculate_min_content_height(cell.box, available_space.width);
        auto max_content_height = calculate_max_content_height(cell.box, available_space.width);
        auto min_content_width = calculate_min_content_width(cell.box);
        auto max_content_width = calculate_max_content_width(cell.box);

        CSSPixels min_height = min_content_height;
        CSSPixels min_width = min_content_width;
        if (!computed_values.min_height().is_auto())
            min_height = max(min_height, computed_values.min_height().to_px(cell.box, containing_block.content_height()));
        if (!computed_values.min_width().is_auto())
            min_width = max(min_width, computed_values.min_width().to_px(cell.box, containing_block.content_width()));

        CSSPixels max_height = computed_values.height().is_auto() ? max_content_height : height;
        CSSPixels max_width = (computed_values.width().is_length() || !table_width_is_auto) ? width : max_content_width;
        if (!should_treat_max_height_as_none(cell.box, available_space.height))
            max_height = min(max_height, computed_values.max_height().to_px(cell.box, containing_block.content_height()));
        if (!should_treat_max_width_as_none(cell.box, available_space.width))
            max_width = min(max_width, computed_values.max_width().to_px(cell.box, containing_block.content_width()));

        if (computed_values.height().is_percentage()) {
            m_rows[cell.row_index].type = SizeType::Percent;
            m_rows[cell.row_index].percentage_height = max(m_rows[cell.row_index].percentage_height, computed_values.height().percentage().value());
        } else {
            m_rows[cell.row_index].type = SizeType::Pixel;
        }
        if (computed_values.width().is_percentage()) {
            m_columns[cell.column_index].type = SizeType::Percent;
            m_columns[cell.column_index].percentage_width = max(m_columns[cell.column_index].percentage_width, computed_values.width().percentage().value());
        } else {
            m_columns[cell.column_index].type = SizeType::Pixel;
            if (computed_values.width().is_length())
                m_columns[cell.column_index].is_constrained = true;
        }

        auto cell_intrinsic_height_offsets = padding_top + padding_bottom + border_top + border_bottom;
        cell.min_height = min_height + cell_intrinsic_height_offsets;
        cell.max_height = max(max(height, min_height), max_height) + cell_intrinsic_height_offsets;

        auto cell_intrinsic_width_offsets = padding_left + padding_right + border_left + border_right;
        cell.min_width = min_width + cell_intrinsic_width_offsets;
        cell.max_width = max(max(width, min_width), max_width) + cell_intrinsic_width_offsets;
    }
}

template<>
void TableFormattingContext::initialize_table_measures<TableFormattingContext::Row>()
{
    auto const& containing_block = m_state.get(*table_wrapper().containing_block());

    for (auto& cell : m_cells) {
        auto const& computed_values = cell.box->computed_values();
        if (cell.row_span == 1) {
            // FIXME: Implement intrinsic percentage width of a column based on cells of span up to 1.
            auto specified_height = m_rows[cell.row_index].type == SizeType::Pixel
                ? computed_values.height().to_px(cell.box, containing_block.content_height())
                : 0;
            // https://www.w3.org/TR/css-tables-3/#row-layout makes specified cell height part of the initialization formula for row table measures:
            // This is done by running the same algorithm as the column measurement, with the span=1 value being initialized (for min-content) with
            // the largest of the resulting height of the previous row layout, the height specified on the corresponding table-row (if any), and
            // the largest height specified on cells that span this row only (the algorithm starts by considering cells of span 2 on top of that assignment).
            m_rows[cell.row_index].min_size = max(m_rows[cell.row_index].min_size, max(cell.min_height, specified_height));
            m_rows[cell.row_index].max_size = max(m_rows[cell.row_index].max_size, cell.max_height);
        }
    }
}

template<>
void TableFormattingContext::initialize_table_measures<TableFormattingContext::Column>()
{
    for (auto& cell : m_cells) {
        if (cell.column_span == 1) {
            m_columns[cell.column_index].min_size = max(m_columns[cell.column_index].min_size, cell.min_width);
            m_columns[cell.column_index].max_size = max(m_columns[cell.column_index].max_size, cell.max_width);
        }
    }
}

template<class RowOrColumn>
void TableFormattingContext::compute_table_measures()
{
    initialize_table_measures<RowOrColumn>();

    auto& rows_or_columns = table_rows_or_columns<RowOrColumn>();

    size_t max_cell_span = 1;
    for (auto& cell : m_cells) {
        max_cell_span = max(max_cell_span, cell_span<RowOrColumn>(cell));
    }

    for (size_t current_span = 2; current_span <= max_cell_span; current_span++) {
        // https://www.w3.org/TR/css-tables-3/#min-content-width-of-a-column-based-on-cells-of-span-up-to-n-n--1
        Vector<Vector<CSSPixels>> cell_min_contributions_by_rc_index;
        cell_min_contributions_by_rc_index.resize(rows_or_columns.size());
        // https://www.w3.org/TR/css-tables-3/#max-content-width-of-a-column-based-on-cells-of-span-up-to-n-n--1
        Vector<Vector<CSSPixels>> cell_max_contributions_by_rc_index;
        cell_max_contributions_by_rc_index.resize(rows_or_columns.size());
        for (auto& cell : m_cells) {
            auto cell_span_value = cell_span<RowOrColumn>(cell);
            if (cell_span_value == current_span) {
                // Define the baseline max-content size as the sum of the max-content sizes based on cells of span up to N-1 of all columns that the cell spans.
                CSSPixels baseline_max_content_size = 0;
                auto cell_start_rc_index = cell_index<RowOrColumn>(cell);
                auto cell_end_rc_index = cell_start_rc_index + cell_span_value;
                for (auto rc_index = cell_start_rc_index; rc_index < cell_end_rc_index; rc_index++) {
                    baseline_max_content_size += rows_or_columns[rc_index].max_size;
                }

                // Define the baseline border spacing as the sum of the horizontal border-spacing for any columns spanned by the cell, other than the one in which the cell originates.
                auto baseline_border_spacing = border_spacing<RowOrColumn>() * (cell_span_value - 1);

                // Add contribution from all rows / columns, since we've weighted the gap to the desired spanned size by the the
                // ratio of the max-content size based on cells of span up to N-1 of the row / column to the baseline max-content width.
                for (auto rc_index = cell_start_rc_index; rc_index < cell_end_rc_index; rc_index++) {
                    // The contribution of the cell is the sum of:
                    // the min-content size of the column based on cells of span up to N-1
                    auto cell_min_contribution = rows_or_columns[rc_index].min_size;
                    // and the product of:
                    // - the ratio of the max-content size based on cells of span up to N-1 of the column to the baseline max-content size
                    // - the outer min-content size of the cell minus the baseline max-content size and baseline border spacing, or 0 if this is negative
                    cell_min_contribution += (rows_or_columns[rc_index].max_size / baseline_max_content_size)
                        * max(CSSPixels(0), cell_min_size<RowOrColumn>(cell) - baseline_max_content_size - baseline_border_spacing);

                    // The contribution of the cell is the sum of:
                    // the max-content size of the column based on cells of span up to N-1
                    auto cell_max_contribution = rows_or_columns[rc_index].max_size;
                    // and the product of:
                    // - the ratio of the max-content size based on cells of span up to N-1 of the column to the baseline max-content size
                    // - the outer max-content size of the cell minus the baseline max-content size and the baseline border spacing, or 0 if this is negative
                    cell_max_contribution += (rows_or_columns[rc_index].max_size / baseline_max_content_size)
                        * max(CSSPixels(0), cell_max_size<RowOrColumn>(cell) - baseline_max_content_size - baseline_border_spacing);
                    cell_min_contributions_by_rc_index[rc_index].append(cell_min_contribution);
                    cell_max_contributions_by_rc_index[rc_index].append(cell_max_contribution);
                }
            }
        }

        for (size_t rc_index = 0; rc_index < rows_or_columns.size(); rc_index++) {
            // min-content size of a row / column based on cells of span up to N (N > 1) is
            // the largest of the min-content size of the row / column based on cells of span up to N-1 and
            // the contributions of the cells in the row / column whose rowSpan / colSpan is N
            for (auto min_contribution : cell_min_contributions_by_rc_index[rc_index])
                rows_or_columns[rc_index].min_size = max(rows_or_columns[rc_index].min_size, min_contribution);

            // max-content size of a row / column based on cells of span up to N (N > 1) is
            // the largest of the max-content size based on cells of span up to N-1 and the contributions of
            // the cells in the row / column whose rowSpan / colSpan is N
            for (auto max_contribution : cell_max_contributions_by_rc_index[rc_index])
                rows_or_columns[rc_index].max_size = max(rows_or_columns[rc_index].max_size, max_contribution);
        }
    }
}

CSSPixels TableFormattingContext::compute_capmin()
{
    // The caption width minimum (CAPMIN) is the largest of the table captions min-content contribution:
    // https://drafts.csswg.org/css-tables-3/#computing-the-table-width
    CSSPixels capmin = 0;
    for (auto* child = table_box().first_child(); child; child = child->next_sibling()) {
        if (!child->display().is_table_caption()) {
            continue;
        }
        VERIFY(child->is_box());
        capmin = max(calculate_min_content_width(static_cast<Box const&>(*child)), capmin);
    }
    return capmin;
}

void TableFormattingContext::compute_table_width()
{
    // https://drafts.csswg.org/css-tables-3/#computing-the-table-width

    auto& table_box_state = m_state.get_mutable(table_box());

    auto& computed_values = table_box().computed_values();

    CSSPixels width_of_table_containing_block = m_state.get(*table_box().containing_block()).content_width();

    // Percentages on 'width' and 'height' on the table are relative to the table wrapper box's containing block,
    // not the table wrapper box itself.
    CSSPixels width_of_table_wrapper_containing_block = m_state.get(*table_wrapper().containing_block()).content_width();

    // Compute undistributable space due to border spacing: https://www.w3.org/TR/css-tables-3/#computing-undistributable-space.
    auto undistributable_space = (m_columns.size() + 1) * border_spacing_horizontal();

    // The row/column-grid width minimum (GRIDMIN) width is the sum of the min-content width
    // of all the columns plus cell spacing or borders.
    CSSPixels grid_min = 0.0f;
    for (auto& column : m_columns) {
        grid_min += column.min_size;
    }
    grid_min += undistributable_space;

    // The row/column-grid width maximum (GRIDMAX) width is the sum of the max-content width
    // of all the columns plus cell spacing or borders.
    CSSPixels grid_max = 0.0f;
    for (auto& column : m_columns) {
        grid_max += column.max_size;
    }
    grid_max += undistributable_space;

    // The used min-width of a table is the greater of the resolved min-width, CAPMIN, and GRIDMIN.
    auto used_min_width = max(grid_min, compute_capmin());
    if (!computed_values.min_width().is_auto()) {
        used_min_width = max(used_min_width, computed_values.min_width().to_px(table_box(), width_of_table_wrapper_containing_block));
    }

    CSSPixels used_width;
    if (computed_values.width().is_auto()) {
        // If the table-root has 'width: auto', the used width is the greater of
        // min(GRIDMAX, the table’s containing block width), the used min-width of the table.
        used_width = max(min(grid_max, width_of_table_containing_block), used_min_width);
        // https://www.w3.org/TR/CSS22/tables.html#auto-table-layout
        // A percentage value for a column width is relative to the table width. If the table has 'width: auto',
        // a percentage represents a constraint on the column's width, which a UA should try to satisfy.
        CSSPixels adjusted_used_width = 0;
        for (auto& cell : m_cells) {
            auto const& cell_width = cell.box->computed_values().width();
            if (cell_width.is_percentage()) {
                adjusted_used_width = 100 / cell_width.percentage().value() * cell.min_width;
                used_width = min(max(used_width, adjusted_used_width), width_of_table_containing_block);
            }
        }
    } else {
        // If the table-root’s width property has a computed value (resolving to
        // resolved-table-width) other than auto, the used width is the greater
        // of resolved-table-width, and the used min-width of the table.
        CSSPixels resolved_table_width = computed_values.width().to_px(table_box(), width_of_table_wrapper_containing_block);
        // Since used_width is content width, we need to subtract the border spacing from the specified width for a consistent comparison.
        used_width = max(resolved_table_width - table_box_state.border_box_left() - table_box_state.border_box_right(), used_min_width);
        if (!should_treat_max_width_as_none(table_box(), m_available_space->width))
            used_width = min(used_width, computed_values.max_width().to_px(table_box(), width_of_table_wrapper_containing_block));
    }

    table_box_state.set_content_width(used_width);
}

void TableFormattingContext::distribute_width_to_columns()
{
    // Implements https://www.w3.org/TR/css-tables-3/#width-distribution-algorithm

    // The total horizontal border spacing is defined for each table:
    // - For tables laid out in separated-borders mode containing at least one column, the horizontal component of the computed value of the border-spacing property times one plus the number of columns in the table
    // - Otherwise, 0
    auto total_horizontal_border_spacing = m_columns.is_empty() ? 0 : (m_columns.size() + 1) * border_spacing_horizontal();

    // The assignable table width is the used width of the table minus the total horizontal border spacing (if any).
    // This is the width that we will be able to allocate to the columns.
    CSSPixels available_width = m_state.get(table_box()).content_width() - total_horizontal_border_spacing;

    auto columns_total_used_width = [&]() {
        CSSPixels total_used_width = 0;
        for (auto& column : m_columns) {
            total_used_width += column.used_width;
        }
        return total_used_width;
    };

    auto column_preferred_width = [&](Column& column) {
        switch (column.type) {
        case SizeType::Percent: {
            return max(column.min_size, column.percentage_width / 100 * available_width);
        }
        case SizeType::Pixel:
        case SizeType::Auto: {
            return column.max_size;
        }
        default: {
            VERIFY_NOT_REACHED();
        }
        }
    };

    auto expand_columns_to_fill_available_width = [&](SizeType column_type) {
        CSSPixels remaining_available_width = available_width;
        CSSPixels total_preferred_width_increment = 0;
        for (auto& column : m_columns) {
            remaining_available_width -= column.used_width;
            if (column.type == column_type) {
                total_preferred_width_increment += column_preferred_width(column) - column.min_size;
            }
        }

        if (total_preferred_width_increment == 0)
            return;

        for (auto& column : m_columns) {
            if (column.type == column_type) {
                CSSPixels preferred_width_increment = column_preferred_width(column) - column.min_size;
                column.used_width += preferred_width_increment * remaining_available_width / total_preferred_width_increment;
            }
        }
    };

    auto shrink_columns_to_fit_available_width = [&](SizeType column_type) {
        for (auto& column : m_columns) {
            if (column.type == column_type)
                column.used_width = column.min_size;
        }

        expand_columns_to_fill_available_width(column_type);
    };

    for (auto& column : m_columns) {
        column.used_width = column.min_size;
    }

    for (auto& column : m_columns) {
        if (column.type == SizeType::Percent) {
            column.used_width = max(column.min_size, column.percentage_width / 100 * available_width);
        }
    }

    if (columns_total_used_width() > available_width) {
        shrink_columns_to_fit_available_width(SizeType::Percent);
        return;
    }

    for (auto& column : m_columns) {
        if (column.type == SizeType::Pixel) {
            column.used_width = column.max_size;
        }
    }

    if (columns_total_used_width() > available_width) {
        shrink_columns_to_fit_available_width(SizeType::Pixel);
        return;
    }

    if (columns_total_used_width() < available_width) {
        expand_columns_to_fill_available_width(SizeType::Auto);
    }

    if (columns_total_used_width() < available_width) {
        expand_columns_to_fill_available_width(SizeType::Pixel);
    }

    if (columns_total_used_width() < available_width) {
        expand_columns_to_fill_available_width(SizeType::Percent);
    }

    // Implements steps 1 and 2 of https://www.w3.org/TR/css-tables-3/#distributing-width-to-columns
    // FIXME: Implement steps 3-5 as well, which distribute excess width to constrained columns.
    if (columns_total_used_width() < available_width) {
        // NOTE: if all columns got their max width and there is still width to distribute left
        // it should be assigned to columns proportionally to their max width
        CSSPixels grid_max = 0.0f;
        size_t unconstrained_column_count = 0;
        for (auto& column : m_columns) {
            if (column.is_constrained) {
                continue;
            }
            grid_max += column.max_size;
            ++unconstrained_column_count;
        }

        auto width_to_distribute = available_width - columns_total_used_width();
        if (grid_max == 0) {
            // If total max width of columns is zero then divide distributable width equally among them
            auto column_width = width_to_distribute / unconstrained_column_count;
            for (auto& column : m_columns) {
                if (column.is_constrained)
                    continue;
                column.used_width = column_width;
            }
        } else {
            // Distribute width to columns proportionally to their max width
            for (auto& column : m_columns) {
                if (column.is_constrained)
                    continue;
                column.used_width += width_to_distribute * column.max_size / grid_max;
            }
        }
    }
}

void TableFormattingContext::compute_table_height(LayoutMode layout_mode)
{
    // First pass of row height calculation:
    for (auto& row : m_rows) {
        auto row_computed_height = row.box->computed_values().height();
        if (row_computed_height.is_length()) {
            auto height_of_containing_block = m_state.get(*row.box->containing_block()).content_height();
            auto row_used_height = row_computed_height.to_px(row.box, height_of_containing_block);
            row.base_height = max(row.base_height, row_used_height);
        }
    }

    // First pass of cells layout:
    for (auto& cell : m_cells) {
        auto& row = m_rows[cell.row_index];
        auto& cell_state = m_state.get_mutable(cell.box);

        CSSPixels span_width = 0;
        for (size_t i = 0; i < cell.column_span; ++i)
            span_width += m_columns[cell.column_index + i].used_width;

        auto width_of_containing_block = m_state.get(*cell.box->containing_block()).content_width();
        auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
        auto height_of_containing_block = m_state.get(*cell.box->containing_block()).content_height();
        auto height_of_containing_block_as_length = CSS::Length::make_px(height_of_containing_block);

        cell_state.padding_top = cell.box->computed_values().padding().top().to_px(cell.box, width_of_containing_block);
        cell_state.padding_bottom = cell.box->computed_values().padding().bottom().to_px(cell.box, width_of_containing_block);
        cell_state.padding_left = cell.box->computed_values().padding().left().to_px(cell.box, width_of_containing_block);
        cell_state.padding_right = cell.box->computed_values().padding().right().to_px(cell.box, width_of_containing_block);

        if (cell.box->computed_values().border_collapse() == CSS::BorderCollapse::Separate) {
            cell_state.border_top = cell.box->computed_values().border_top().width;
            cell_state.border_bottom = cell.box->computed_values().border_bottom().width;
            cell_state.border_left = cell.box->computed_values().border_left().width;
            cell_state.border_right = cell.box->computed_values().border_right().width;
        }

        auto cell_computed_height = cell.box->computed_values().height();
        if (cell_computed_height.is_length()) {
            auto cell_used_height = cell_computed_height.to_px(cell.box, height_of_containing_block);
            cell_state.set_content_height(cell_used_height - cell_state.border_box_top() - cell_state.border_box_bottom());

            row.base_height = max(row.base_height, cell_used_height);
        }

        // Compute cell width as specified by https://www.w3.org/TR/css-tables-3/#bounding-box-assignment:
        // The position of any table-cell, table-track, or table-track-group box within the table is defined as the rectangle whose width/height is the sum of:
        // - the widths/heights of all spanned visible columns/rows
        // - the horizontal/vertical border-spacing times the amount of spanned visible columns/rows minus one
        // FIXME: Account for visibility.
        cell_state.set_content_width(span_width - cell_state.border_box_left() - cell_state.border_box_right() + (cell.column_span - 1) * border_spacing_horizontal());
        if (auto independent_formatting_context = layout_inside(cell.box, layout_mode, cell_state.available_inner_space_or_constraints_from(*m_available_space))) {
            cell_state.set_content_height(independent_formatting_context->automatic_content_height());
            independent_formatting_context->parent_context_did_dimension_child_root_box();
        }

        cell.baseline = box_baseline(cell.box);

        // Implements https://www.w3.org/TR/css-tables-3/#computing-the-table-height

        // The minimum height of a row is the maximum of:
        // - the computed height (if definite, percentages being considered 0px) of its corresponding table-row (if nay)
        // - the computed height of each cell spanning the current row exclusively (if definite, percentages being treated as 0px), and
        // - the minimum height (ROWMIN) required by the cells spanning the row.
        // Note that we've already applied the first rule at the top of the method.
        if (cell.row_span == 1) {
            row.base_height = max(row.base_height, cell_state.border_box_height());
        }
        row.base_height = max(row.base_height, m_rows[cell.row_index].min_size);
        row.baseline = max(row.baseline, cell.baseline);
    }

    CSSPixels sum_rows_height = 0;
    for (auto& row : m_rows) {
        sum_rows_height += row.base_height;
    }

    m_table_height = sum_rows_height;

    if (!table_box().computed_values().height().is_auto()) {
        // If the table has a height property with a value other than auto, it is treated as a minimum height for the
        // table grid, and will eventually be distributed to the height of the rows if their collective minimum height
        // ends up smaller than this number.
        CSSPixels height_of_table_containing_block = m_state.get(*table_wrapper().containing_block()).content_height();
        auto specified_table_height = table_box().computed_values().height().to_px(table_box(), height_of_table_containing_block);
        auto const& table_state = m_state.get(table_box());
        m_table_height = max(m_table_height, specified_table_height - table_state.border_box_top() - table_state.border_box_bottom());
    }

    for (auto& row : m_rows) {
        // Reference size is the largest of
        // - its initial base height and
        // - its new base height (the one evaluated during the second layout pass, where percentages used in
        //   rowgroups/rows/cells' specified heights were resolved according to the table height, instead of
        //   being ignored as 0px).

        // Assign reference size to base size. Later reference size might change to largee value during
        // second pass of rows layout.
        row.reference_height = row.base_height;
    }

    // Second pass of rows height calculation:
    // At this point percentage row height can be resolved because final table height is calculated.
    for (auto& row : m_rows) {
        auto row_computed_height = row.box->computed_values().height();
        if (row_computed_height.is_percentage()) {
            auto row_used_height = row_computed_height.to_px(row.box, m_table_height);
            row.reference_height = max(row.reference_height, row_used_height);
        } else {
            continue;
        }
    }

    // Second pass cells layout:
    // At this point percantage cell height can be resolved because final table heigh is calculated.
    for (auto& cell : m_cells) {
        auto& row = m_rows[cell.row_index];
        auto& cell_state = m_state.get_mutable(cell.box);

        CSSPixels span_width = 0;
        for (size_t i = 0; i < cell.column_span; ++i)
            span_width += m_columns[cell.column_index + i].used_width;

        auto cell_computed_height = cell.box->computed_values().height();
        if (cell_computed_height.is_percentage()) {
            auto cell_used_height = cell_computed_height.to_px(cell.box, m_table_height);
            cell_state.set_content_height(cell_used_height - cell_state.border_box_top() - cell_state.border_box_bottom());

            row.reference_height = max(row.reference_height, cell_used_height);
        } else {
            continue;
        }

        cell_state.set_content_width(span_width - cell_state.border_box_left() - cell_state.border_box_right() + (cell.column_span - 1) * border_spacing_horizontal());
        if (auto independent_formatting_context = layout_inside(cell.box, layout_mode, cell_state.available_inner_space_or_constraints_from(*m_available_space))) {
            independent_formatting_context->parent_context_did_dimension_child_root_box();
        }

        cell.baseline = box_baseline(cell.box);

        row.reference_height = max(row.reference_height, cell_state.border_box_height());
        row.baseline = max(row.baseline, cell.baseline);
    }
}

void TableFormattingContext::distribute_height_to_rows()
{
    CSSPixels sum_reference_height = 0;
    for (auto& row : m_rows) {
        sum_reference_height += row.reference_height;
    }

    if (sum_reference_height == 0)
        return;

    Vector<Row&> rows_with_auto_height;
    for (auto& row : m_rows) {
        if (row.box->computed_values().height().is_auto()) {
            rows_with_auto_height.append(row);
        }
    }

    if (m_table_height <= sum_reference_height) {
        // If the table height is equal or smaller than sum of reference sizes, the final height assigned to each row
        // will be the weighted mean of the base and the reference size that yields the correct total height.

        for (auto& row : m_rows) {
            auto weight = row.reference_height / sum_reference_height;
            auto final_height = m_table_height * weight;
            row.final_height = final_height;
        }
    } else if (rows_with_auto_height.size() > 0) {
        // Else, if the table owns any “auto-height” row (a row whose size is only determined by its content size and
        // none of the specified heights), each non-auto-height row receives its reference height and auto-height rows
        // receive their reference size plus some increment which is equal to the height missing to amount to the
        // specified table height divided by the amount of such rows.

        for (auto& row : m_rows) {
            row.final_height = row.reference_height;
        }

        auto auto_height_rows_increment = (m_table_height - sum_reference_height) / rows_with_auto_height.size();
        for (auto& row : rows_with_auto_height) {
            row.final_height += auto_height_rows_increment;
        }
    } else {
        // Else, all rows receive their reference size plus some increment which is equal to the height missing to
        // amount to the specified table height divided by the amount of rows.

        auto increment = (m_table_height - sum_reference_height) / m_rows.size();
        for (auto& row : m_rows) {
            row.final_height = row.reference_height + increment;
        }
    }

    // Add undistributable space due to border spacing: https://www.w3.org/TR/css-tables-3/#computing-undistributable-space.
    m_table_height += (m_rows.size() + 1) * border_spacing_vertical();
}

void TableFormattingContext::position_row_boxes()
{
    auto const& table_state = m_state.get(table_box());

    CSSPixels row_top_offset = table_state.offset.y() + table_state.padding_top + border_spacing_vertical();
    CSSPixels row_left_offset = table_state.border_left + table_state.padding_left + border_spacing_horizontal();
    for (size_t y = 0; y < m_rows.size(); y++) {
        auto& row = m_rows[y];
        auto& row_state = m_state.get_mutable(row.box);
        CSSPixels row_width = 0.0f;
        for (auto& column : m_columns) {
            row_width += column.used_width;
        }

        row_state.set_content_height(row.final_height);
        row_state.set_content_width(row_width);
        row_state.set_content_x(row_left_offset);
        row_state.set_content_y(row_top_offset);
        row_top_offset += row_state.content_height() + border_spacing_vertical();
    }

    CSSPixels row_group_top_offset = table_state.border_top + table_state.padding_top;
    CSSPixels row_group_left_offset = table_state.border_left + table_state.padding_left;
    for_each_child_box_matching(table_box(), is_table_row_group, [&](auto& row_group_box) {
        CSSPixels row_group_height = 0.0f;
        CSSPixels row_group_width = 0.0f;

        auto& row_group_box_state = m_state.get_mutable(row_group_box);
        row_group_box_state.set_content_x(row_group_left_offset);
        row_group_box_state.set_content_y(row_group_top_offset);

        for_each_child_box_matching(row_group_box, is_table_row, [&](auto& row) {
            auto const& row_state = m_state.get(row);
            row_group_height += row_state.border_box_height();
            row_group_width = max(row_group_width, row_state.border_box_width());
        });

        row_group_box_state.set_content_height(row_group_height);
        row_group_box_state.set_content_width(row_group_width);

        row_group_top_offset += row_group_height;
    });

    auto total_content_height = max(row_top_offset, row_group_top_offset) - table_state.offset.y() - table_state.padding_top;
    m_table_height = max(total_content_height, m_table_height);
}

void TableFormattingContext::position_cell_boxes()
{
    CSSPixels left_column_offset = 0;
    for (auto& column : m_columns) {
        column.left_offset = left_column_offset;
        left_column_offset += column.used_width;
    }

    for (auto& cell : m_cells) {
        auto& cell_state = m_state.get_mutable(cell.box);
        auto& row_state = m_state.get(m_rows[cell.row_index].box);
        CSSPixels const cell_border_box_height = cell_state.content_height() + cell_state.border_box_top() + cell_state.border_box_bottom();
        CSSPixels const row_content_height = compute_row_content_height(cell);
        auto const& vertical_align = cell.box->computed_values().vertical_align();
        // The following image shows various alignment lines of a row:
        // https://www.w3.org/TR/css-tables-3/images/cell-align-explainer.png
        if (vertical_align.has<CSS::VerticalAlign>()) {
            auto height_diff = row_content_height - cell_border_box_height;
            switch (vertical_align.get<CSS::VerticalAlign>()) {
            case CSS::VerticalAlign::Middle: {
                cell_state.padding_top += height_diff / 2;
                cell_state.padding_bottom += height_diff / 2;
                break;
            }
            case CSS::VerticalAlign::Top: {
                cell_state.padding_bottom += height_diff;
                break;
            }
            case CSS::VerticalAlign::Bottom: {
                cell_state.padding_top += height_diff;
                break;
            }
            case CSS::VerticalAlign::Baseline: {
                cell_state.padding_top += m_rows[cell.row_index].baseline - cell.baseline;
                cell_state.padding_bottom += height_diff;
                break;
            }
            default:
                VERIFY_NOT_REACHED();
            }
        }

        // Compute cell position as specified by https://www.w3.org/TR/css-tables-3/#bounding-box-assignment:
        // left/top location is the sum of:
        // - for top: the height reserved for top captions (including margins), if any
        // - the padding-left/padding-top and border-left-width/border-top-width of the table
        // FIXME: Account for visibility.
        cell_state.offset = row_state.offset.translated(
            cell_state.border_box_left() + m_columns[cell.column_index].left_offset + cell.column_index * border_spacing_horizontal(),
            cell_state.border_box_top());
    }
}

static const CSS::BorderData& winning_border_style(const CSS::BorderData& a, const CSS::BorderData& b)
{
    // Implements steps 1, 2 and 3 of border conflict resolution algorithm.
    static HashMap<CSS::LineStyle, unsigned> const line_style_score = {
        { CSS::LineStyle::Inset, 0 },
        { CSS::LineStyle::Groove, 1 },
        { CSS::LineStyle::Outset, 2 },
        { CSS::LineStyle::Ridge, 3 },
        { CSS::LineStyle::Dotted, 4 },
        { CSS::LineStyle::Dashed, 5 },
        { CSS::LineStyle::Solid, 6 },
        { CSS::LineStyle::Double, 7 },
    };
    if (a.line_style == CSS::LineStyle::Hidden) {
        return a;
    }
    if (b.line_style == CSS::LineStyle::Hidden) {
        return b;
    }
    if (a.line_style == CSS::LineStyle::None) {
        return b;
    }
    if (b.line_style == CSS::LineStyle::None) {
        return a;
    }
    if (a.width > b.width) {
        return a;
    } else if (a.width < b.width) {
        return b;
    }
    if (*line_style_score.get(a.line_style) > *line_style_score.get(b.line_style)) {
        return a;
    } else if (*line_style_score.get(a.line_style) < *line_style_score.get(b.line_style)) {
        return b;
    }
    return a;
}

const CSS::BorderData& TableFormattingContext::border_data_conflicting_edge(TableFormattingContext::ConflictingEdge const& conflicting_edge)
{
    auto const& style = conflicting_edge.element->computed_values();
    switch (conflicting_edge.side) {
    case ConflictingSide::Top: {
        return style.border_top();
    }
    case ConflictingSide::Bottom: {
        return style.border_bottom();
    }
    case ConflictingSide::Left: {
        return style.border_left();
    }
    case ConflictingSide::Right: {
        return style.border_right();
    }
    default: {
        VERIFY_NOT_REACHED();
    }
    }
}

void TableFormattingContext::border_conflict_resolution()
{
    // Partially implements border conflict resolution, as described in
    // https://www.w3.org/TR/CSS22/tables.html#border-conflict-resolution
    BorderConflictFinder finder(this);
    for (auto& cell : m_cells) {
        if (cell.box->computed_values().border_collapse() == CSS::BorderCollapse::Separate) {
            continue;
        }
        // Execute steps 1, 2 and 3 of the algorithm for each edge.
        Painting::BordersData override_borders_data;
        auto const& cell_style = cell.box->computed_values();
        auto& cell_state = m_state.get_mutable(cell.box);
        auto winning_border_left = cell_style.border_left();
        for (auto const conflicting_edge : finder.conflicting_edges(cell, ConflictingSide::Left)) {
            winning_border_left = winning_border_style(winning_border_left, border_data_conflicting_edge(conflicting_edge));
        }
        override_borders_data.left = winning_border_left;
        cell_state.border_left = winning_border_left.width;
        auto winning_border_right = cell_style.border_right();
        for (auto const conflicting_edge : finder.conflicting_edges(cell, ConflictingSide::Right)) {
            winning_border_right = winning_border_style(winning_border_right, border_data_conflicting_edge(conflicting_edge));
        }
        override_borders_data.right = winning_border_right;
        cell_state.border_right = winning_border_right.width;
        auto winning_border_top = cell_style.border_top();
        for (auto const conflicting_edge : finder.conflicting_edges(cell, ConflictingSide::Top)) {
            winning_border_top = winning_border_style(winning_border_top, border_data_conflicting_edge(conflicting_edge));
        }
        override_borders_data.top = winning_border_top;
        cell_state.border_top = winning_border_top.width;
        auto winning_border_bottom = cell_style.border_bottom();
        for (auto const conflicting_edge : finder.conflicting_edges(cell, ConflictingSide::Bottom)) {
            winning_border_bottom = winning_border_style(winning_border_bottom, border_data_conflicting_edge(conflicting_edge));
        }
        override_borders_data.bottom = winning_border_bottom;
        cell_state.border_bottom = override_borders_data.bottom.width;
        // FIXME: 4. If border styles differ only in color, then a style set on a cell wins over one on a row, which wins over a
        //           row group, column, column group and, lastly, table. When two elements of the same type conflict, then the one
        //           further to the left (if the table's 'direction' is 'ltr'; right, if it is 'rtl') and further to the top wins.
        cell_state.set_override_borders_data(override_borders_data);
    }
}

CSSPixels TableFormattingContext::compute_row_content_height(Cell const& cell) const
{
    auto& row_state = m_state.get(m_rows[cell.row_index].box);
    if (cell.row_span == 1) {
        return row_state.content_height();
    }
    // The height of a cell is the sum of all spanned rows, as described in
    // https://www.w3.org/TR/css-tables-3/#bounding-box-assignment

    // When the row span is greater than 1, the borders of inner rows within the span have to be
    // included in the content height of the spanning cell. First top and final bottom borders are
    // excluded to be consistent with the handling of row span 1 case above, which uses the content
    // height (no top and bottom borders) of the row.
    CSSPixels span_height = 0;
    for (size_t i = 0; i < cell.row_span; ++i) {
        auto const& row_state = m_state.get(m_rows[cell.row_index + i].box);
        if (i == 0) {
            span_height += row_state.content_height() + row_state.border_box_bottom();
        } else if (i == cell.row_span - 1) {
            span_height += row_state.border_box_top() + row_state.content_height();
        } else {
            span_height += row_state.border_box_height();
        }
    }

    // Compute cell height as specified by https://www.w3.org/TR/css-tables-3/#bounding-box-assignment:
    // width/height is the sum of:
    // - the widths/heights of all spanned visible columns/rows
    // - the horizontal/vertical border-spacing times the amount of spanned visible columns/rows minus one
    // FIXME: Account for visibility.
    span_height += (cell.row_span - 1) * border_spacing_vertical();
    return span_height;
}

TableFormattingContext::BorderConflictFinder::BorderConflictFinder(TableFormattingContext const* context)
    : m_context(context)
{
    collect_conflicting_col_elements();
}

void TableFormattingContext::BorderConflictFinder::collect_conflicting_col_elements()
{
    m_col_elements_by_index.resize(m_context->m_columns.size());
    for (auto* child = m_context->table_box().first_child(); child; child = child->next_sibling()) {
        if (!child->display().is_table_column_group()) {
            continue;
        }
        size_t column_index = 0;
        for (auto* child_of_column_group = child->first_child(); child_of_column_group; child_of_column_group = child_of_column_group->next_sibling()) {
            VERIFY(child_of_column_group->display().is_table_column());
            auto const& col_node = static_cast<HTML::HTMLTableColElement const&>(*child_of_column_group->dom_node());
            unsigned span = col_node.attribute(HTML::AttributeNames::span).to_uint().value_or(1);
            for (size_t i = column_index; i < column_index + span; ++i) {
                m_col_elements_by_index[i] = child_of_column_group;
            }
            column_index += span;
        }
    }
}

Vector<TableFormattingContext::ConflictingEdge> TableFormattingContext::BorderConflictFinder::conflicting_edges(
    Cell const& cell, TableFormattingContext::ConflictingSide edge) const
{
    // FIXME: Conflicting elements can be cells, rows, row groups, columns, column groups, and the table itself,
    //        but we only consider 'col' elements in a 'colgroup' and the table itself for now.
    Vector<ConflictingEdge> result = {};
    if (m_col_elements_by_index[cell.column_index] && edge == ConflictingSide::Left) {
        result.append({ m_col_elements_by_index[cell.column_index], ConflictingSide::Left });
    }
    if (cell.column_index >= cell.column_span && m_col_elements_by_index[cell.column_index - cell.column_span] && edge == ConflictingSide::Left) {
        result.append({ m_col_elements_by_index[cell.column_index - cell.column_span], ConflictingSide::Right });
    }
    if (m_col_elements_by_index[cell.column_index] && edge == ConflictingSide::Right) {
        result.append({ m_col_elements_by_index[cell.column_index], ConflictingSide::Right });
    }
    if (cell.column_index + cell.column_span < m_col_elements_by_index.size() && m_col_elements_by_index[cell.column_index + cell.column_span] && edge == ConflictingSide::Right) {
        result.append({ m_col_elements_by_index[cell.column_index + cell.column_span], ConflictingSide::Left });
    }
    if (cell.row_index == 0 && edge == ConflictingSide::Top) {
        if (m_col_elements_by_index[cell.column_index]) {
            result.append({ m_col_elements_by_index[cell.column_index], ConflictingSide::Top });
        }
        result.append({ &m_context->table_box(), ConflictingSide::Top });
    }
    if (cell.row_index == m_context->m_rows.size() - 1 && edge == ConflictingSide::Bottom) {
        if (m_col_elements_by_index[cell.column_index]) {
            result.append({ m_col_elements_by_index[cell.column_index], ConflictingSide::Bottom });
        }
        result.append({ &m_context->table_box(), ConflictingSide::Bottom });
    }
    if (cell.column_index == 0 && edge == ConflictingSide::Left) {
        result.append({ &m_context->table_box(), ConflictingSide::Left });
    }
    if (cell.column_index == m_context->m_columns.size() - 1 && edge == ConflictingSide::Right) {
        result.append({ &m_context->table_box(), ConflictingSide::Right });
    }
    return result;
}

void TableFormattingContext::run(Box const& box, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    m_available_space = available_space;

    auto total_captions_height = run_caption_layout(layout_mode, CSS::CaptionSide::Top);

    // Determine the number of rows/columns the table requires.
    calculate_row_column_grid(box);

    border_conflict_resolution();

    // Compute the minimum width of each column.
    compute_cell_measures(available_space);
    compute_table_measures<Column>();

    // https://www.w3.org/TR/css-tables-3/#row-layout
    // Since during row layout the specified heights of cells in the row were ignored and cells that were spanning more than one rows
    // have not been sized correctly, their height will need to be eventually distributed to the set of rows they spanned. This is done
    // by running the same algorithm as the column measurement, with the span=1 value being initialized (for min-content) with the largest
    // of the resulting height of the previous row layout, the height specified on the corresponding table-row (if any), and the largest
    // height specified on cells that span this row only (the algorithm starts by considering cells of span 2 on top of that assignment).
    compute_table_measures<Row>();

    // Compute the width of the table.
    compute_table_width();

    if (available_space.width.is_intrinsic_sizing_constraint() && !available_space.height.is_intrinsic_sizing_constraint()) {
        return;
    }

    // Distribute the width of the table among columns.
    distribute_width_to_columns();

    compute_table_height(layout_mode);

    distribute_height_to_rows();

    position_row_boxes();
    position_cell_boxes();

    m_state.get_mutable(table_box()).set_content_height(m_table_height);

    total_captions_height += run_caption_layout(layout_mode, CSS::CaptionSide::Bottom);

    // Table captions are positioned between the table margins and its borders (outside the grid box borders) as described in
    // https://www.w3.org/TR/css-tables-3/#bounding-box-assignment
    // A visual representation of this model can be found at https://www.w3.org/TR/css-tables-3/images/table_container.png
    m_state.get_mutable(table_box()).margin_bottom += total_captions_height;

    m_automatic_content_height = m_table_height;
}

CSSPixels TableFormattingContext::automatic_content_width() const
{
    return greatest_child_width(context_box());
}

CSSPixels TableFormattingContext::automatic_content_height() const
{
    return m_automatic_content_height;
}

template<>
size_t TableFormattingContext::cell_span<TableFormattingContext::Row>(TableFormattingContext::Cell const& cell)
{
    return cell.row_span;
}

template<>
size_t TableFormattingContext::cell_span<TableFormattingContext::Column>(TableFormattingContext::Cell const& cell)
{
    return cell.column_span;
}

template<>
size_t TableFormattingContext::cell_index<TableFormattingContext::Row>(TableFormattingContext::Cell const& cell)
{
    return cell.row_index;
}

template<>
size_t TableFormattingContext::cell_index<TableFormattingContext::Column>(TableFormattingContext::Cell const& cell)
{
    return cell.column_index;
}

template<>
CSSPixels TableFormattingContext::cell_min_size<TableFormattingContext::Row>(TableFormattingContext::Cell const& cell)
{
    return cell.min_height;
}

template<>
CSSPixels TableFormattingContext::cell_min_size<TableFormattingContext::Column>(TableFormattingContext::Cell const& cell)
{
    return cell.min_width;
}

template<>
CSSPixels TableFormattingContext::cell_max_size<TableFormattingContext::Row>(TableFormattingContext::Cell const& cell)
{
    return cell.max_height;
}

template<>
CSSPixels TableFormattingContext::cell_max_size<TableFormattingContext::Column>(TableFormattingContext::Cell const& cell)
{
    return cell.max_width;
}

template<>
CSSPixels TableFormattingContext::border_spacing<TableFormattingContext::Row>()
{
    return border_spacing_vertical();
}

template<>
CSSPixels TableFormattingContext::border_spacing<TableFormattingContext::Column>()
{
    return border_spacing_horizontal();
}

CSSPixels TableFormattingContext::border_spacing_horizontal() const
{
    auto const& computed_values = table_box().computed_values();
    // When a table is laid out in collapsed-borders mode, the border-spacing of the table-root is ignored (as if it was set to 0px):
    // https://www.w3.org/TR/css-tables-3/#collapsed-style-overrides
    if (computed_values.border_collapse() == CSS::BorderCollapse::Collapse)
        return 0;
    return computed_values.border_spacing_horizontal().to_px(table_box());
}

CSSPixels TableFormattingContext::border_spacing_vertical() const
{
    auto const& computed_values = table_box().computed_values();
    // When a table is laid out in collapsed-borders mode, the border-spacing of the table-root is ignored (as if it was set to 0px):
    // https://www.w3.org/TR/css-tables-3/#collapsed-style-overrides
    if (computed_values.border_collapse() == CSS::BorderCollapse::Collapse)
        return 0;
    return computed_values.border_spacing_vertical().to_px(table_box());
}

template<>
Vector<TableFormattingContext::Row>& TableFormattingContext::table_rows_or_columns()
{
    return m_rows;
}

template<>
Vector<TableFormattingContext::Column>& TableFormattingContext::table_rows_or_columns()
{
    return m_columns;
}

}
