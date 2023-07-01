/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/QuickSort.h>
#include <AK/Traits.h>
#include <LibWeb/Layout/TableFormattingContext.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/TableBordersPainting.h>

struct CellCoordinates {
    size_t row_index;
    size_t column_index;

    bool operator==(CellCoordinates const& other) const
    {
        return row_index == other.row_index && column_index == other.column_index;
    }
};

namespace AK {
template<>
struct Traits<CellCoordinates> : public GenericTraits<CellCoordinates> {
    static unsigned hash(CellCoordinates const& key) { return pair_int_hash(key.row_index, key.column_index); }
};
}

namespace Web::Painting {

static void collect_cell_boxes_with_collapsed_borders(Vector<PaintableBox const*>& cell_boxes, Layout::Node const& box)
{
    box.for_each_child([&](auto& child) {
        if (child.display().is_table_cell() && child.computed_values().border_collapse() == CSS::BorderCollapse::Collapse) {
            VERIFY(is<Layout::Box>(child) && child.paintable());
            cell_boxes.append(static_cast<Layout::Box const&>(child).paintable_box());
        } else {
            collect_cell_boxes_with_collapsed_borders(cell_boxes, child);
        }
    });
}

enum class EdgeDirection {
    Horizontal,
    Vertical,
};

struct BorderEdgePaintingInfo {
    DevicePixelRect rect;
    CSS::BorderData border_data;
    EdgeDirection direction;
};

static BorderEdgePaintingInfo make_right_cell_edge(PaintContext& context, CSSPixelRect const& right_cell_rect, DevicePixelRect const& cell_rect, BordersData const& borders_data)
{
    auto device_right_cell_rect = context.rounded_device_rect(right_cell_rect);
    DevicePixelRect right_border_rect = {
        device_right_cell_rect.x() - context.enclosing_device_pixels(borders_data.right.width / 2),
        cell_rect.y() - context.enclosing_device_pixels(borders_data.top.width / 2),
        context.enclosing_device_pixels(borders_data.right.width),
        max(cell_rect.height(), device_right_cell_rect.height()) + context.enclosing_device_pixels(borders_data.top.width / 2) + context.enclosing_device_pixels(borders_data.bottom.width / 2),
    };
    return BorderEdgePaintingInfo {
        .rect = right_border_rect,
        .border_data = borders_data.right,
        .direction = EdgeDirection::Vertical,
    };
}

static BorderEdgePaintingInfo make_down_cell_edge(PaintContext& context, CSSPixelRect const& down_cell_rect, DevicePixelRect const& cell_rect, BordersData const& borders_data)
{
    auto device_down_cell_rect = context.rounded_device_rect(down_cell_rect);
    DevicePixelRect down_border_rect = {
        cell_rect.x() - context.enclosing_device_pixels(borders_data.left.width / 2),
        device_down_cell_rect.y() - context.enclosing_device_pixels(borders_data.bottom.width / 2),
        max(cell_rect.width(), device_down_cell_rect.width()) + context.enclosing_device_pixels(borders_data.left.width / 2) + context.enclosing_device_pixels(borders_data.right.width / 2),
        context.enclosing_device_pixels(borders_data.bottom.width),
    };
    return BorderEdgePaintingInfo {
        .rect = down_border_rect,
        .border_data = borders_data.bottom,
        .direction = EdgeDirection::Horizontal,
    };
}

static BorderEdgePaintingInfo make_first_row_top_cell_edge(PaintContext& context, DevicePixelRect const& cell_rect, BordersData const& borders_data)
{
    DevicePixelRect top_border_rect = {
        cell_rect.x() - context.enclosing_device_pixels(borders_data.left.width / 2),
        cell_rect.y() - context.enclosing_device_pixels(borders_data.top.width / 2),
        cell_rect.width(),
        context.enclosing_device_pixels(borders_data.top.width),
    };
    return BorderEdgePaintingInfo {
        .rect = top_border_rect,
        .border_data = borders_data.top,
        .direction = EdgeDirection::Horizontal,
    };
}

static BorderEdgePaintingInfo make_last_row_bottom_cell_edge(PaintContext& context, DevicePixelRect const& cell_rect, BordersData const& borders_data)
{
    DevicePixelRect bottom_border_rect = {
        cell_rect.x() - context.enclosing_device_pixels(borders_data.left.width / 2),
        cell_rect.y() + cell_rect.height() - context.enclosing_device_pixels(borders_data.bottom.width / 2),
        cell_rect.width() + context.enclosing_device_pixels(borders_data.left.width / 2) + context.enclosing_device_pixels(borders_data.right.width / 2),
        context.enclosing_device_pixels(borders_data.bottom.width),
    };
    return BorderEdgePaintingInfo {
        .rect = bottom_border_rect,
        .border_data = borders_data.bottom,
        .direction = EdgeDirection::Horizontal,
    };
}

static BorderEdgePaintingInfo make_first_column_left_cell_edge(PaintContext& context, DevicePixelRect const& cell_rect, BordersData const& borders_data)
{
    DevicePixelRect left_border_rect = {
        cell_rect.x() - context.enclosing_device_pixels(borders_data.left.width / 2),
        cell_rect.y() - context.enclosing_device_pixels(borders_data.top.width / 2),
        context.enclosing_device_pixels(borders_data.left.width),
        cell_rect.height(),
    };
    return BorderEdgePaintingInfo {
        .rect = left_border_rect,
        .border_data = borders_data.left,
        .direction = EdgeDirection::Vertical,
    };
}

static BorderEdgePaintingInfo make_last_column_right_cell_edge(PaintContext& context, DevicePixelRect const& cell_rect, BordersData const& borders_data)
{
    DevicePixelRect right_border_rect = {
        cell_rect.x() + cell_rect.width() - context.enclosing_device_pixels(borders_data.right.width / 2),
        cell_rect.y() - context.enclosing_device_pixels(borders_data.top.width / 2),
        context.enclosing_device_pixels(borders_data.right.width),
        cell_rect.height() + context.enclosing_device_pixels(borders_data.top.width / 2) + context.enclosing_device_pixels(borders_data.bottom.width / 2),
    };
    return BorderEdgePaintingInfo {
        .rect = right_border_rect,
        .border_data = borders_data.right,
        .direction = EdgeDirection::Vertical,
    };
}

static void paint_collected_edges(PaintContext& context, Vector<BorderEdgePaintingInfo>& border_edge_painting_info_list)
{
    // This sorting step isn't part of the specification, but it matches the behavior of other browsers at border intersections, which aren't
    // part of border conflict resolution in the specification but it's still desirable to handle them in a way which is consistent with it.
    quick_sort(border_edge_painting_info_list, [](auto const& a, auto const& b) {
        return Layout::TableFormattingContext::border_is_less_specific(a.border_data, b.border_data);
    });

    for (auto const& border_edge_painting_info : border_edge_painting_info_list) {
        auto const& border_data = border_edge_painting_info.border_data;
        CSSPixels width = border_data.width;
        if (width <= 0)
            continue;
        auto color = border_data.color;
        auto border_style = border_data.line_style;
        auto p1 = border_edge_painting_info.rect.top_left();
        auto p2 = border_edge_painting_info.direction == EdgeDirection::Horizontal
            ? border_edge_painting_info.rect.top_right()
            : border_edge_painting_info.rect.bottom_left();

        if (border_style == CSS::LineStyle::Dotted) {
            Gfx::AntiAliasingPainter aa_painter { context.painter() };
            aa_painter.draw_line(p1.to_type<int>(), p2.to_type<int>(), color, width.to_double(), Gfx::Painter::LineStyle::Dotted);
        } else if (border_style == CSS::LineStyle::Dashed) {
            context.painter().draw_line(p1.to_type<int>(), p2.to_type<int>(), color, width.to_double(), Gfx::Painter::LineStyle::Dashed);
        } else {
            // FIXME: Support the remaining line styles instead of rendering them as solid.
            context.painter().fill_rect(Gfx::IntRect(border_edge_painting_info.rect.location(), border_edge_painting_info.rect.size()), color);
        }
    }
}

void paint_table_collapsed_borders(PaintContext& context, Layout::Node const& box)
{
    // Partial implementation of painting according to the collapsing border model:
    // https://www.w3.org/TR/CSS22/tables.html#collapsing-borders
    Vector<PaintableBox const*> cell_boxes;
    collect_cell_boxes_with_collapsed_borders(cell_boxes, box);
    Vector<BorderEdgePaintingInfo> border_edge_painting_info_list;
    HashMap<CellCoordinates, PaintableBox const*> cell_coordinates_to_box;
    size_t row_count = 0;
    size_t column_count = 0;
    for (auto const cell_box : cell_boxes) {
        cell_coordinates_to_box.set(CellCoordinates {
                                        .row_index = cell_box->table_cell_coordinates()->row_index,
                                        .column_index = cell_box->table_cell_coordinates()->column_index },
            cell_box);
        row_count = max(row_count, cell_box->table_cell_coordinates()->row_index + cell_box->table_cell_coordinates()->row_span);
        column_count = max(column_count, cell_box->table_cell_coordinates()->column_index + cell_box->table_cell_coordinates()->column_span);
    }
    for (auto const cell_box : cell_boxes) {
        auto borders_data = cell_box->override_borders_data().has_value() ? cell_box->override_borders_data().value() : BordersData {
            .top = cell_box->box_model().border.top == 0 ? CSS::BorderData() : cell_box->computed_values().border_top(),
            .right = cell_box->box_model().border.right == 0 ? CSS::BorderData() : cell_box->computed_values().border_right(),
            .bottom = cell_box->box_model().border.bottom == 0 ? CSS::BorderData() : cell_box->computed_values().border_bottom(),
            .left = cell_box->box_model().border.left == 0 ? CSS::BorderData() : cell_box->computed_values().border_left(),
        };
        auto cell_rect = context.rounded_device_rect(cell_box->absolute_border_box_rect());
        auto maybe_right_cell = cell_coordinates_to_box.get(CellCoordinates {
            .row_index = cell_box->table_cell_coordinates()->row_index,
            .column_index = cell_box->table_cell_coordinates()->column_index + cell_box->table_cell_coordinates()->column_span });
        auto maybe_down_cell = cell_coordinates_to_box.get(CellCoordinates {
            .row_index = cell_box->table_cell_coordinates()->row_index + cell_box->table_cell_coordinates()->row_span,
            .column_index = cell_box->table_cell_coordinates()->column_index });
        if (maybe_right_cell.has_value())
            border_edge_painting_info_list.append(make_right_cell_edge(context, maybe_right_cell.value()->absolute_border_box_rect(), cell_rect, borders_data));
        if (maybe_down_cell.has_value())
            border_edge_painting_info_list.append(make_down_cell_edge(context, maybe_down_cell.value()->absolute_border_box_rect(), cell_rect, borders_data));
        if (cell_box->table_cell_coordinates()->row_index == 0)
            border_edge_painting_info_list.append(make_first_row_top_cell_edge(context, cell_rect, borders_data));
        if (cell_box->table_cell_coordinates()->row_index + cell_box->table_cell_coordinates()->row_span == row_count)
            border_edge_painting_info_list.append(make_last_row_bottom_cell_edge(context, cell_rect, borders_data));
        if (cell_box->table_cell_coordinates()->column_index == 0)
            border_edge_painting_info_list.append(make_first_column_left_cell_edge(context, cell_rect, borders_data));
        if (cell_box->table_cell_coordinates()->column_index + cell_box->table_cell_coordinates()->column_span == column_count)
            border_edge_painting_info_list.append(make_last_column_right_cell_edge(context, cell_rect, borders_data));
    }

    paint_collected_edges(context, border_edge_painting_info_list);

    for (auto const cell_box : cell_boxes) {
        auto const& border_radii_data = cell_box->normalized_border_radii_data();
        auto top_left = border_radii_data.top_left.as_corner(context);
        auto top_right = border_radii_data.top_right.as_corner(context);
        auto bottom_right = border_radii_data.bottom_right.as_corner(context);
        auto bottom_left = border_radii_data.bottom_left.as_corner(context);
        if (!top_left && !top_right && !bottom_left && !bottom_right) {
            continue;
        } else {
            auto borders_data = cell_box->override_borders_data().has_value() ? cell_box->override_borders_data().value() : BordersData {
                .top = cell_box->box_model().border.top == 0 ? CSS::BorderData() : cell_box->computed_values().border_top(),
                .right = cell_box->box_model().border.right == 0 ? CSS::BorderData() : cell_box->computed_values().border_right(),
                .bottom = cell_box->box_model().border.bottom == 0 ? CSS::BorderData() : cell_box->computed_values().border_bottom(),
                .left = cell_box->box_model().border.left == 0 ? CSS::BorderData() : cell_box->computed_values().border_left(),
            };
            paint_all_borders(context, cell_box->absolute_border_box_rect(), cell_box->normalized_border_radii_data(), borders_data);
        }
    }
}

}
