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

    auto process_row = [&](auto& row) {
        if (y_height == y_current)
            y_height++;

        x_current = 0;
        while (x_current < x_width && grid.contains(GridPosition { x_current, y_current }))
            x_current++;

        for (auto* child = row.first_child(); child; child = child->next_sibling()) {
            if (child->display().is_table_cell()) {
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
}

void TableFormattingContext::compute_cell_measures(AvailableSpace const& available_space)
{
    auto const& containing_block = m_state.get(*table_wrapper().containing_block());

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
        auto width = computed_values.width().to_px(cell.box, containing_block.content_width());
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
        CSSPixels max_width = computed_values.width().is_auto() ? max_content_width : width;
        if (!computed_values.max_height().is_none())
            max_height = min(max_height, computed_values.max_height().to_px(cell.box, containing_block.content_height()));
        if (!computed_values.max_width().is_none())
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
        }

        auto cell_intrinsic_height_offsets = padding_top + padding_bottom + border_top + border_bottom;
        cell.min_height = min_height + cell_intrinsic_height_offsets;
        cell.max_height = max(max(height, min_height), max_height) + cell_intrinsic_height_offsets;

        auto cell_intrinsic_width_offsets = padding_left + padding_right + border_left + border_right;
        cell.min_width = min_width + cell_intrinsic_width_offsets;
        cell.max_width = max(max(width, min_width), max_width) + cell_intrinsic_width_offsets;
    }
}

template<class RowOrColumn>
void TableFormattingContext::compute_table_measures()
{
    auto& rows_or_columns = table_rows_or_columns<RowOrColumn>();
    for (auto& cell : m_cells) {
        if (cell_span<RowOrColumn>(cell) == 1) {
            auto rc_index = cell_index<RowOrColumn>(cell);
            rows_or_columns[rc_index].min_size = max(rows_or_columns[rc_index].min_size, cell_min_size<RowOrColumn>(cell));
            rows_or_columns[rc_index].max_size = max(rows_or_columns[rc_index].max_size, cell_max_size<RowOrColumn>(cell));
        }
    }

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

                // The contribution of the cell is the sum of:
                // the min-content size of the column based on cells of span up to N-1
                auto cell_min_contribution = rows_or_columns[cell_start_rc_index].min_size;
                // and the product of:
                // - the ratio of the max-content size based on cells of span up to N-1 of the column to the baseline max-content size
                // - the outer min-content size of the cell minus the baseline max-content size and baseline border spacing, or 0 if this is negative
                cell_min_contribution += (rows_or_columns[cell_start_rc_index].max_size / baseline_max_content_size) * max(CSSPixels(0), cell_min_size<RowOrColumn>(cell) - baseline_max_content_size);

                // The contribution of the cell is the sum of:
                // the max-content size of the column based on cells of span up to N-1
                auto cell_max_contribution = rows_or_columns[cell_start_rc_index].max_size;
                // and the product of:
                // - the ratio of the max-content size based on cells of span up to N-1 of the column to the baseline max-content size
                // - the outer max-content size of the cell minus the baseline max-content size and the baseline border spacing, or 0 if this is negative
                cell_max_contribution += (rows_or_columns[cell_start_rc_index].max_size / baseline_max_content_size) * max(CSSPixels(0), cell_max_size<RowOrColumn>(cell) - baseline_max_content_size);

                // Spread contribution to all rows / columns, since we've weighted the gap to the desired spanned size by the the
                // ratio of the max-content size based on cells of span up to N-1 of the row / column to the baseline max-content width.
                for (auto rc_index = cell_start_rc_index; rc_index < cell_end_rc_index; rc_index++) {
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

    // The row/column-grid width minimum (GRIDMIN) width is the sum of the min-content width
    // of all the columns plus cell spacing or borders.
    CSSPixels grid_min = 0.0f;
    for (auto& column : m_columns) {
        grid_min += column.min_size;
    }

    // The row/column-grid width maximum (GRIDMAX) width is the sum of the max-content width
    // of all the columns plus cell spacing or borders.
    CSSPixels grid_max = 0.0f;
    for (auto& column : m_columns) {
        grid_max += column.max_size;
    }

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
    } else {
        // If the table-root’s width property has a computed value (resolving to
        // resolved-table-width) other than auto, the used width is the greater
        // of resolved-table-width, and the used min-width of the table.
        CSSPixels resolved_table_width = computed_values.width().to_px(table_box(), width_of_table_wrapper_containing_block);
        used_width = max(resolved_table_width, used_min_width);
        if (!computed_values.max_width().is_none())
            used_width = min(used_width, computed_values.max_width().to_px(table_box(), width_of_table_wrapper_containing_block));
    }

    // The assignable table width is the used width of the table minus the total horizontal border spacing (if any).
    // This is the width that we will be able to allocate to the columns.
    table_box_state.set_content_width(used_width - table_box_state.border_left - table_box_state.border_right);
}

void TableFormattingContext::distribute_width_to_columns()
{
    // Implements https://www.w3.org/TR/css-tables-3/#width-distribution-algorithm

    CSSPixels available_width = m_state.get(table_box()).content_width();

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

    if (columns_total_used_width() < available_width) {
        // NOTE: if all columns got their max width and there is still width to distribute left
        // it should be assigned to columns proportionally to their max width
        CSSPixels grid_max = 0.0f;
        for (auto& column : m_columns) {
            grid_max += column.max_size;
        }

        auto width_to_distribute = available_width - columns_total_used_width();
        if (grid_max == 0) {
            // If total max width of columns is zero then divide distributable width equally among them
            auto column_width = width_to_distribute / m_columns.size();
            for (auto& column : m_columns) {
                column.used_width = column_width;
            }
        } else {
            // Distribute width to columns proportionally to their max width
            for (auto& column : m_columns) {
                column.used_width += width_to_distribute * column.max_size / grid_max;
            }
        }
    }
}

void TableFormattingContext::determine_intrisic_size_of_table_container(AvailableSpace const& available_space)
{
    auto& table_box_state = m_state.get_mutable(table_box());

    if (available_space.width.is_min_content()) {
        // The min-content width of a table is the width required to fit all of its columns min-content widths and its undistributable spaces.
        CSSPixels grid_min = 0.0f;
        for (auto& column : m_columns)
            grid_min += column.min_size;
        table_box_state.set_content_width(grid_min);
    }

    if (available_space.width.is_max_content()) {
        // The max-content width of a table is the width required to fit all of its columns max-content widths and its undistributable spaces.
        CSSPixels grid_max = 0.0f;
        for (auto& column : m_columns)
            grid_max += column.max_size;
        table_box_state.set_content_width(grid_max);
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

        cell_state.set_content_width((span_width - cell_state.border_box_left() - cell_state.border_box_right()));
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

    // The height of a table is the sum of the row heights plus any cell spacing or borders.
    m_table_height = sum_rows_height;

    if (!table_box().computed_values().height().is_auto()) {
        // If the table has a height property with a value other than auto, it is treated as a minimum height for the
        // table grid, and will eventually be distributed to the height of the rows if their collective minimum height
        // ends up smaller than this number.
        CSSPixels height_of_table_containing_block = m_state.get(*table_wrapper().containing_block()).content_height();
        auto specified_table_height = table_box().computed_values().height().to_px(table_box(), height_of_table_containing_block);
        if (m_table_height < specified_table_height) {
            m_table_height = specified_table_height;
        }
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

        cell_state.set_content_width((span_width - cell_state.border_box_left() - cell_state.border_box_right()));
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
}

void TableFormattingContext::position_row_boxes(CSSPixels& total_content_height)
{
    auto const& table_state = m_state.get(table_box());

    CSSPixels row_top_offset = table_state.offset.y() + table_state.padding_top;
    CSSPixels row_left_offset = table_state.border_left + table_state.padding_left;
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
        row_top_offset += row_state.content_height();
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

    total_content_height = max(row_top_offset, row_group_top_offset) - table_state.offset.y() - table_state.padding_top;
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
        if (vertical_align.has<CSS::VerticalAlign>()) {
            switch (vertical_align.get<CSS::VerticalAlign>()) {
            case CSS::VerticalAlign::Middle: {
                cell_state.padding_top += (row_content_height - cell_border_box_height) / 2;
                cell_state.padding_bottom += (row_content_height - cell_border_box_height) / 2;
                break;
            }
            // FIXME: implement actual 'top' and 'bottom' support instead of fall back to 'baseline'
            case CSS::VerticalAlign::Top:
            case CSS::VerticalAlign::Bottom:
            case CSS::VerticalAlign::Baseline: {
                cell_state.padding_top += m_rows[cell.row_index].baseline - cell.baseline;
                cell_state.padding_bottom += row_content_height - cell_border_box_height;
                break;
            }
            default:
                VERIFY_NOT_REACHED();
            }
        }

        cell_state.offset = row_state.offset.translated(cell_state.border_box_left() + m_columns[cell.column_index].left_offset, cell_state.border_box_top());
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
        for (auto const conflicting_element : finder.conflicting_elements(cell, ConflictingEdge::Left)) {
            auto const& other_style = conflicting_element->computed_values();
            override_borders_data.left = winning_border_style(cell_style.border_left(), other_style.border_left());
            cell_state.border_left = override_borders_data.left.width;
        }
        for (auto const conflicting_element : finder.conflicting_elements(cell, ConflictingEdge::Right)) {
            auto const& other_style = conflicting_element->computed_values();
            override_borders_data.right = winning_border_style(cell_style.border_right(), other_style.border_right());
            cell_state.border_right = override_borders_data.right.width;
        }
        for (auto const conflicting_element : finder.conflicting_elements(cell, ConflictingEdge::Top)) {
            auto const& other_style = conflicting_element->computed_values();
            override_borders_data.top = winning_border_style(cell_style.border_top(), other_style.border_top());
            cell_state.border_top = override_borders_data.top.width;
        }
        for (auto const conflicting_element : finder.conflicting_elements(cell, ConflictingEdge::Bottom)) {
            auto const& other_style = conflicting_element->computed_values();
            override_borders_data.bottom = winning_border_style(cell_style.border_bottom(), other_style.border_bottom());
            cell_state.border_bottom = override_borders_data.bottom.width;
        }
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

Vector<Node const*> TableFormattingContext::BorderConflictFinder::conflicting_elements(Cell const& cell, TableFormattingContext::ConflictingEdge edge) const
{
    // FIXME: Conflicting elements can be cells, rows, row groups, columns, column groups, and the table itself,
    //        but we only consider 'col' elements in a 'colgroup', the table and the cell itself for now.
    Vector<Node const*> result = { cell.box };
    auto is_top = cell.row_index == 0;
    auto is_left = cell.column_index == 0;
    auto is_right = cell.column_index == m_context->m_columns.size() - 1;
    auto is_bottom = cell.row_index == m_context->m_rows.size() - 1;
    auto conflicts_top_or_bottom = (is_top && edge == ConflictingEdge::Top) || (is_bottom && edge == ConflictingEdge::Bottom);
    if (conflicts_top_or_bottom || (is_left && edge == ConflictingEdge::Left) || (is_right && edge == ConflictingEdge::Right)) {
        result.append(&m_context->table_box());
    }
    if (m_col_elements_by_index[cell.column_index] && (conflicts_top_or_bottom || edge == ConflictingEdge::Left || edge == ConflictingEdge::Right)) {
        result.append(m_col_elements_by_index[cell.column_index]);
    }
    return result;
}

void TableFormattingContext::run(Box const& box, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    m_available_space = available_space;

    auto total_captions_height = run_caption_layout(layout_mode, CSS::CaptionSide::Top);

    CSSPixels total_content_height = 0;

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

    if (available_space.width.is_intrinsic_sizing_constraint() && !available_space.height.is_intrinsic_sizing_constraint()) {
        determine_intrisic_size_of_table_container(available_space);
        return;
    }

    // Compute the width of the table.
    compute_table_width();

    // Distribute the width of the table among columns.
    distribute_width_to_columns();

    compute_table_height(layout_mode);

    distribute_height_to_rows();

    position_row_boxes(total_content_height);
    position_cell_boxes();

    m_state.get_mutable(table_box()).set_content_height(total_content_height);

    total_captions_height += run_caption_layout(layout_mode, CSS::CaptionSide::Bottom);

    // Table captions are positioned between the table margins and its borders (outside the grid box borders) as described in
    // https://www.w3.org/TR/css-tables-3/#bounding-box-assignment
    // A visual representation of this model can be found at https://www.w3.org/TR/css-tables-3/images/table_container.png
    m_state.get_mutable(table_box()).margin_bottom += total_captions_height;

    // FIXME: This is a total hack, we should respect the 'height' property.
    m_automatic_content_height = total_content_height;
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
