/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TreeMapWidget.h"
#include "ProgressWindow.h"
#include "Tree.h"
#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/NumberFormat.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Statusbar.h>
#include <LibGfx/Font/Font.h>
#include <WindowServer/WindowManager.h>

namespace SpaceAnalyzer {

static constexpr Array colors = {
    Color(253, 231, 37),
    Color(148, 216, 64),
    Color(60, 188, 117),
    Color(31, 150, 139),
    Color(45, 112, 142),
    Color(63, 71, 136),
    Color(85, 121, 104),
};

static float get_normalized_aspect_ratio(float a, float b)
{
    if (a < b) {
        return a / b;
    } else {
        return b / a;
    }
}

static bool node_is_leaf(TreeNode const& node)
{
    return node.num_children() == 0;
}

bool TreeMapWidget::rect_can_contain_label(Gfx::IntRect const& rect) const
{
    return rect.height() >= font().presentation_size() && rect.width() > 20;
}

void TreeMapWidget::paint_cell_frame(GUI::Painter& painter, TreeNode const& node, Gfx::IntRect const& cell_rect, Gfx::IntRect const& inner_rect, int depth, HasLabel has_label) const
{
    if (cell_rect.width() <= 2 || cell_rect.height() <= 2) {
        painter.fill_rect(cell_rect, Color::Black);
        return;
    }
    Gfx::IntRect remainder = cell_rect;

    Color color = colors[depth % (sizeof(colors) / sizeof(colors[0]))];
    if (m_selected_node_cache == &node) {
        color = color.darkened(0.8f);
    }

    // Draw borders.
    painter.fill_rect(remainder.take_from_right(1), Color::Black);
    painter.fill_rect(remainder.take_from_bottom(1), Color::Black);
    // Draw highlights.
    painter.fill_rect(remainder.take_from_right(1), color.darkened());
    painter.fill_rect(remainder.take_from_bottom(1), color.darkened());
    painter.fill_rect(remainder.take_from_top(1), color.lightened());
    painter.fill_rect(remainder.take_from_left(1), color.lightened());

    // Paint the background.
    if (inner_rect.is_empty()) {
        painter.fill_rect(remainder, color);
    } else {
        // Draw black edges above and to the left of the inner_rect.
        Gfx::IntRect border_rect = inner_rect.inflated(2, 2);
        Gfx::IntRect hammer_rect = border_rect;
        hammer_rect.set_width(hammer_rect.width() - 1);
        hammer_rect.set_height(hammer_rect.height() - 1);
        painter.fill_rect(border_rect.take_from_top(1), Color::Black);
        painter.fill_rect(border_rect.take_from_left(1), Color::Black);
        for (auto& shard : remainder.shatter(hammer_rect)) {
            painter.fill_rect(shard, color);
        }
    }

    // Paint text.
    if (has_label == HasLabel::Yes) {
        Gfx::IntRect text_rect = remainder;
        text_rect.shrink(4, 4);
        painter.clear_clip_rect();
        painter.add_clip_rect(text_rect);
        if (node_is_leaf(node)) {
            painter.draw_text(text_rect, node.name(), font(), Gfx::TextAlignment::TopLeft, Color::Black);
            text_rect.take_from_top(font().presentation_size() + 1);
            painter.draw_text(text_rect, human_readable_size(node.area()), font(), Gfx::TextAlignment::TopLeft, Color::Black);
        } else {
            painter.draw_text(text_rect, ByteString::formatted("{} - {}", node.name(), human_readable_size(node.area())), font(), Gfx::TextAlignment::TopLeft, Color::Black);
        }
        painter.clear_clip_rect();
    }
}

template<typename Function>
void TreeMapWidget::lay_out_children(TreeNode const& node, Gfx::IntRect const& rect, int depth, Function callback)
{
    if (node.num_children() == 0) {
        return;
    }

    // Check if the children are sorted yet, if not do that now.
    for (size_t k = 0; k < node.num_children() - 1; k++) {
        if (node.child_at(k).area() < node.child_at(k + 1).area()) {
            node.sort_children_by_area();
            break;
        }
    }

    i64 total_area = node.area();
    Gfx::IntRect canvas = rect;
    bool remaining_nodes_are_too_small = false;
    for (size_t i = 0; !remaining_nodes_are_too_small && i < node.num_children(); i++) {
        i64 const i_node_area = node.child_at(i).area();
        if (i_node_area == 0)
            break;

        size_t const long_side_size = max(canvas.width(), canvas.height());
        size_t const short_side_size = min(canvas.width(), canvas.height());

        size_t row_or_column_size = long_side_size * i_node_area / total_area;
        i64 node_area_sum = i_node_area;
        size_t k = i + 1;

        // Try to add nodes to this row or column so long as the worst aspect ratio of
        // the new set of nodes is better than the worst aspect ratio of the current set.
        {
            float best_worst_aspect_ratio_so_far = get_normalized_aspect_ratio(row_or_column_size, short_side_size);
            for (; k < node.num_children(); k++) {
                // Do a preliminary calculation of the worst aspect ratio of the nodes at index i and k
                // if that aspect ratio is better than the 'best_worst_aspect_ratio_so_far' we keep it,
                // otherwise it is discarded.
                i64 k_node_area = node.child_at(k).area();
                if (k_node_area == 0) {
                    break;
                }
                i64 new_node_area_sum = node_area_sum + k_node_area;
                size_t new_row_or_column_size = long_side_size * new_node_area_sum / total_area;
                size_t i_node_size = short_side_size * i_node_area / new_node_area_sum;
                size_t k_node_size = short_side_size * k_node_area / new_node_area_sum;
                float i_node_aspect_ratio = get_normalized_aspect_ratio(new_row_or_column_size, i_node_size);
                float k_node_aspect_ratio = get_normalized_aspect_ratio(new_row_or_column_size, k_node_size);
                float new_worst_aspect_ratio = min(i_node_aspect_ratio, k_node_aspect_ratio);
                if (new_worst_aspect_ratio < best_worst_aspect_ratio_so_far) {
                    break;
                }
                best_worst_aspect_ratio_so_far = new_worst_aspect_ratio;
                node_area_sum = new_node_area_sum;
                row_or_column_size = new_row_or_column_size;
            }
        }

        // Paint the elements from 'i' up to and including 'k-1'.
        {
            size_t const fixed_side_size = row_or_column_size;
            i64 placement_area = node_area_sum;
            size_t main_dim = short_side_size;

            // Lay out nodes in a row or column.
            Orientation orientation = canvas.width() > canvas.height() ? Orientation::Horizontal : Orientation::Vertical;
            Gfx::IntRect layout_rect = canvas;
            layout_rect.set_primary_size_for_orientation(orientation, fixed_side_size);
            for (size_t q = i; q < k; q++) {
                auto& child = node.child_at(q);
                size_t node_size = main_dim * child.area() / placement_area;
                Gfx::IntRect cell_rect = layout_rect;
                cell_rect.set_secondary_size_for_orientation(orientation, node_size);
                Gfx::IntRect inner_rect;
                HasLabel has_label = HasLabel::No;
                if (child.num_children() != 0 && rect.height() >= 8 && rect.width() >= 8) {
                    inner_rect = cell_rect;
                    inner_rect.shrink(4, 4); // border and shading
                    if (rect_can_contain_label(inner_rect)) {
                        int const margin = 5;
                        has_label = HasLabel::Yes;
                        inner_rect.set_y(inner_rect.y() + font().presentation_size() + margin);
                        inner_rect.set_height(inner_rect.height() - (font().presentation_size() + margin * 2));
                        inner_rect.set_x(inner_rect.x() + margin);
                        inner_rect.set_width(inner_rect.width() - margin * 2);
                    }
                } else if (rect_can_contain_label(cell_rect)) {
                    has_label = HasLabel::Yes;
                }
                callback(child, q, cell_rect, inner_rect, depth, has_label, IsRemainder::No);
                if (cell_rect.width() * cell_rect.height() < 16) {
                    remaining_nodes_are_too_small = true;
                } else if (!inner_rect.is_empty()) {
                    lay_out_children(child, inner_rect, depth + 1, callback);
                }
                layout_rect.set_secondary_offset_for_orientation(orientation, layout_rect.secondary_offset_for_orientation(orientation) + node_size);
                main_dim -= node_size;
                placement_area -= child.area();
            }
            canvas.set_primary_offset_for_orientation(orientation, canvas.primary_offset_for_orientation(orientation) + fixed_side_size);
            canvas.set_primary_size_for_orientation(orientation, canvas.primary_size_for_orientation(orientation) - fixed_side_size);
        }

        // Consume nodes that were added to this row or column.
        i = k - 1;
        total_area -= node_area_sum;
    }

    // If not the entire canvas was filled with nodes, fill the remaining area with a dither pattern.
    if (!canvas.is_empty()) {
        callback(node, 0, canvas, Gfx::IntRect(), depth, HasLabel::No, IsRemainder::Yes);
    }
}

TreeNode const* TreeMapWidget::path_node(size_t n) const
{
    if (!m_tree.ptr())
        return nullptr;
    TreeNode const* iter = &m_tree->root();
    size_t path_index = 0;
    while (iter && path_index < m_path_segments.size() && path_index < n) {
        auto child_name = m_path_segments[path_index];
        auto maybe_child = iter->child_with_name(child_name);
        if (!maybe_child.has_value())
            return nullptr;
        iter = &maybe_child.release_value();
        path_index++;
    }
    return iter;
}

void TreeMapWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    m_selected_node_cache = path_node(m_path_segments.size());

    TreeNode const* node = path_node(m_viewpoint);
    if (!node) {
        painter.fill_rect(frame_inner_rect(), Color::MidGray);
    } else if (node_is_leaf(*node)) {
        paint_cell_frame(painter, *node, frame_inner_rect(), Gfx::IntRect(), m_viewpoint - 1, HasLabel::Yes);
    } else {
        lay_out_children(*node, frame_inner_rect(), m_viewpoint, [&](TreeNode const& node, int, Gfx::IntRect const& rect, Gfx::IntRect const& inner_rect, int depth, HasLabel has_label, IsRemainder remainder) {
            if (remainder == IsRemainder::No) {
                paint_cell_frame(painter, node, rect, inner_rect, depth, has_label);
            } else {
                Color color = colors[depth % (sizeof(colors) / sizeof(colors[0]))];
                Gfx::IntRect dither_rect = rect;
                painter.fill_rect(dither_rect.take_from_right(1), Color::Black);
                painter.fill_rect(dither_rect.take_from_bottom(1), Color::Black);
                painter.fill_rect_with_dither_pattern(dither_rect, color, Color::Black);
            }
        });
    }
}

Vector<ByteString> TreeMapWidget::path_to_position(Gfx::IntPoint position)
{
    TreeNode const* node = path_node(m_viewpoint);
    if (!node) {
        return {};
    }
    Vector<ByteString> path;
    lay_out_children(*node, frame_inner_rect(), m_viewpoint, [&](TreeNode const& node, int, Gfx::IntRect const& rect, Gfx::IntRect const&, int, HasLabel, IsRemainder is_remainder) {
        if (is_remainder == IsRemainder::No && rect.contains(position)) {
            path.append(node.name());
        }
    });
    return path;
}

void TreeMapWidget::mousemove_event(GUI::MouseEvent& event)
{
    auto* node = path_node(m_viewpoint);
    if (!node) {
        set_tooltip({});
        return;
    }

    auto* hovered_node = node;
    lay_out_children(*node, frame_inner_rect(), m_viewpoint, [&](TreeNode const&, int index, Gfx::IntRect const& rect, Gfx::IntRect const&, int, HasLabel, IsRemainder is_remainder) {
        if (is_remainder == IsRemainder::No && rect.contains(event.position())) {
            hovered_node = &hovered_node->child_at(index);
        }
    });

    set_tooltip(MUST(String::formatted("{}\n{}", hovered_node->name(), human_readable_size(hovered_node->area()))));
}

void TreeMapWidget::mousedown_event(GUI::MouseEvent& event)
{
    TreeNode const* node = path_node(m_viewpoint);
    if (node && !node_is_leaf(*node)) {
        auto path = path_to_position(event.position());
        if (!path.is_empty()) {
            m_path_segments.shrink(m_viewpoint);
            m_path_segments.extend(path);
            update();
        }
    }
}

void TreeMapWidget::doubleclick_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;
    TreeNode const* node = path_node(m_viewpoint);
    if (node && !node_is_leaf(*node)) {
        auto path = path_to_position(event.position());
        m_path_segments.shrink(m_viewpoint);
        m_path_segments.extend(path);
        m_viewpoint = m_path_segments.size();
        if (on_path_change) {
            on_path_change();
        }
        update();
    }
}

void TreeMapWidget::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == KeyCode::Key_Left)
        set_viewpoint(m_viewpoint == 0 ? m_path_segments.size() : m_viewpoint - 1);
    else if (event.key() == KeyCode::Key_Right)
        set_viewpoint(m_viewpoint == m_path_segments.size() ? 0 : m_viewpoint + 1);
    else
        event.ignore();
}

void TreeMapWidget::mousewheel_event(GUI::MouseEvent& event)
{
    int delta = event.wheel_raw_delta_y();
    if (delta > 0) {
        size_t step_back = delta;
        if (step_back > m_viewpoint)
            step_back = m_viewpoint;
        set_viewpoint(m_viewpoint - step_back);
    } else {
        size_t step_up = -delta;
        set_viewpoint(m_viewpoint + step_up);
    }
}

void TreeMapWidget::context_menu_event(GUI::ContextMenuEvent& context_menu_event)
{
    if (on_context_menu_request)
        on_context_menu_request(context_menu_event);
}

void TreeMapWidget::recalculate_path_for_new_tree()
{
    TreeNode const* current = &m_tree->root();
    size_t new_path_length = 0;
    for (auto& segment : m_path_segments) {
        auto maybe_child = current->child_with_name(segment);
        if (!maybe_child.has_value())
            break;
        new_path_length++;
        current = &maybe_child.release_value();
    }
    m_path_segments.shrink(new_path_length);
    if (new_path_length < m_viewpoint)
        m_viewpoint = new_path_length - 1;
}

static ErrorOr<void> fill_mounts(Vector<MountInfo>& output)
{
    // Output info about currently mounted filesystems.
    auto file = TRY(Core::File::open("/sys/kernel/df"sv, Core::File::OpenMode::Read));

    auto content = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(content));

    TRY(json.as_array().try_for_each([&output](JsonValue const& value) -> ErrorOr<void> {
        auto& filesystem_object = value.as_object();
        MountInfo mount_info;
        mount_info.mount_point = filesystem_object.get_byte_string("mount_point"sv).value_or({});
        mount_info.source = filesystem_object.get_byte_string("source"sv).value_or("none");
        TRY(output.try_append(mount_info));
        return {};
    }));

    return {};
}

ErrorOr<void> TreeMapWidget::analyze(GUI::Statusbar& statusbar)
{
    statusbar.set_text({});
    auto progress_window = TRY(ProgressWindow::try_create("Space Analyzer"sv));
    progress_window->show();

    // Build an in-memory tree mirroring the filesystem and for each node
    // calculate the sum of the file size for all its descendants.
    auto tree = TRY(Tree::create(""));
    Vector<MountInfo> mounts;
    TRY(fill_mounts(mounts));
    auto errors = tree->root().populate_filesize_tree(mounts, [&](size_t processed_file_count) {
        progress_window->update_progress_label(processed_file_count);
    });

    progress_window->close();

    // Display an error summary in the statusbar.
    if (!errors.is_empty()) {
        StringBuilder builder;
        bool first = true;
        builder.append("Some directories were not analyzed: "sv);
        for (auto& key : errors.keys()) {
            if (!first) {
                builder.append(", "sv);
            }
            auto const* error = strerror(key);
            builder.append({ error, strlen(error) });
            builder.append(" ("sv);
            int value = errors.get(key).value();
            builder.append(ByteString::number(value));
            if (value == 1) {
                builder.append(" time"sv);
            } else {
                builder.append(" times"sv);
            }
            builder.append(')');
            first = false;
        }
        statusbar.set_text(TRY(builder.to_string()));
    } else {
        statusbar.set_text("No errors"_string);
    }

    m_tree = move(tree);
    recalculate_path_for_new_tree();

    if (on_path_change) {
        on_path_change();
    }
    update();

    return {};
}

void TreeMapWidget::set_viewpoint(size_t viewpoint)
{
    if (m_viewpoint == viewpoint)
        return;
    if (viewpoint > m_path_segments.size())
        viewpoint = m_path_segments.size();
    m_viewpoint = viewpoint;
    if (on_path_change) {
        on_path_change();
    }
    update();
}

size_t TreeMapWidget::path_size() const
{
    return m_path_segments.size() + 1;
}

size_t TreeMapWidget::viewpoint() const
{
    return m_viewpoint;
}

}
