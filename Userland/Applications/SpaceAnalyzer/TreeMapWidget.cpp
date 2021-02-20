/*
 * Copyright (c) 2021, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "TreeMapWidget.h"
#include <AK/NumberFormat.h>
#include <LibGUI/Painter.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Font.h>
#include <WindowServer/WindowManager.h>

namespace SpaceAnalyzer {

REGISTER_WIDGET(SpaceAnalyzer, TreeMapWidget)

TreeMapWidget::TreeMapWidget()
    : m_viewpoint(0)
{
}

TreeMapWidget::~TreeMapWidget()
{
}

static const Color colors[] = {
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

static bool node_is_leaf(const TreeMapNode& node)
{
    return node.num_children() == 0;
}

bool TreeMapWidget::rect_can_contain_label(const Gfx::IntRect& rect) const
{
    return rect.height() > font().presentation_size() && rect.width() > 20;
}

bool TreeMapWidget::rect_can_contain_children(const Gfx::IntRect& rect) const
{
    return rect.height() > 10 && rect.width() > 10;
}

Gfx::IntRect TreeMapWidget::inner_rect_for_frame(const Gfx::IntRect& rect) const
{
    const int margin = 5;
    Gfx::IntRect tmp_rect = rect;
    tmp_rect.shrink(2, 2); // border
    tmp_rect.shrink(2, 2); // shading
    if (rect_can_contain_label(tmp_rect)) {
        tmp_rect.set_y(tmp_rect.y() + font().presentation_size() + margin);
        tmp_rect.set_height(tmp_rect.height() - (font().presentation_size() + margin * 2));
        tmp_rect.set_x(tmp_rect.x() + margin);
        tmp_rect.set_width(tmp_rect.width() - margin * 2);
    }
    return tmp_rect;
}

void TreeMapWidget::paint_cell_frame(GUI::Painter& painter, const TreeMapNode& node, const Gfx::IntRect& cell_rect, int depth, bool fill_frame) const
{
    const Gfx::IntRect border_rect = cell_rect.shrunken(2, 2);
    const Gfx::IntRect outer_rect = border_rect.shrunken(2, 2);
    const Gfx::IntRect inner_rect = inner_rect_for_frame(cell_rect);

    painter.clear_clip_rect();
    painter.add_clip_rect(cell_rect);
    Color color = colors[depth % (sizeof(colors) / sizeof(colors[0]))];
    if (m_selected_node_cache == &node) {
        color = color.darkened(0.8f);
    }

    // Draw borders.
    painter.draw_rect(cell_rect, Color::Black, false);
    painter.draw_line(border_rect.bottom_left(), border_rect.top_left(), color.lightened());
    painter.draw_line(border_rect.top_left(), border_rect.top_right(), color.lightened());
    painter.draw_line(border_rect.top_right(), border_rect.bottom_right(), color.darkened());
    painter.draw_line(border_rect.bottom_left(), border_rect.bottom_right(), color.darkened());

    // Paint the background.
    if (fill_frame) {
        painter.fill_rect(outer_rect, color);
    } else {
        for (auto& shard : outer_rect.shatter(inner_rect)) {
            painter.fill_rect(shard, color);
        }
    }

    // Paint text.
    if (rect_can_contain_label(outer_rect)) {
        Gfx::IntRect text_rect = outer_rect;
        text_rect.move_by(2, 2);
        painter.draw_text(text_rect, node.name(), font(), Gfx::TextAlignment::TopLeft, Color::Black);
        if (node_is_leaf(node)) {
            text_rect.move_by(0, font().presentation_size() + 1);
            painter.draw_text(text_rect, human_readable_size(node.area()), font(), Gfx::TextAlignment::TopLeft, Color::Black);
        }
    }
}

template<typename Function>
void TreeMapWidget::lay_out_children(const TreeMapNode& node, const Gfx::IntRect& rect, int depth, Function callback)
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

    int total_area = node.area();
    Gfx::IntRect canvas = rect;
    bool remaining_nodes_are_too_small = false;
    for (size_t i = 0; !remaining_nodes_are_too_small && i < node.num_children(); i++) {
        const int i_node_area = node.child_at(i).area();
        if (i_node_area == 0)
            break;

        const int long_side_size = max(canvas.width(), canvas.height());
        const int short_side_size = min(canvas.width(), canvas.height());

        int row_or_column_size = (long long int)long_side_size * i_node_area / total_area;
        int node_area_sum = i_node_area;
        size_t k = i + 1;

        // Try to add nodes to this row or column so long as the worst aspect ratio of
        // the new set of nodes is better than the worst aspect ratio of the current set.
        {
            float best_worst_aspect_ratio_so_far = get_normalized_aspect_ratio(row_or_column_size, short_side_size);
            for (; k < node.num_children(); k++) {
                // Do a preliminary calculation of the worst aspect ratio of the nodes at index i and k
                // if that aspect ratio is better than the 'best_worst_aspect_ratio_so_far' we keep it,
                // otherwise it is discarded.
                int k_node_area = node.child_at(k).area();
                if (k_node_area == 0) {
                    break;
                }
                int new_node_area_sum = node_area_sum + k_node_area;
                int new_row_or_column_size = (long long int)long_side_size * new_node_area_sum / total_area;
                int i_node_size = (long long int)short_side_size * i_node_area / new_node_area_sum;
                int k_node_size = (long long int)short_side_size * k_node_area / new_node_area_sum;
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
            const int fixed_side_size = row_or_column_size;
            int placement_area = node_area_sum;
            int main_dim = short_side_size;

            // Lay out nodes in a row or column.
            Orientation orientation = canvas.width() > canvas.height() ? Orientation::Horizontal : Orientation::Vertical;
            Gfx::IntRect layout_rect = canvas;
            layout_rect.set_primary_size_for_orientation(orientation, fixed_side_size);
            for (size_t q = i; q < k; q++) {
                auto& child = node.child_at(q);
                int node_size = (long long int)main_dim * child.area() / placement_area;
                Gfx::IntRect cell_rect = layout_rect;
                cell_rect.set_secondary_size_for_orientation(orientation, node_size);
                Gfx::IntRect inner_rect = inner_rect_for_frame(cell_rect);
                bool is_visual_leaf = child.num_children() == 0 || !rect_can_contain_children(inner_rect);
                callback(child, q, cell_rect, depth, is_visual_leaf ? IsVisualLeaf::Yes : IsVisualLeaf::No, IsRemainder::No);
                if (cell_rect.width() * cell_rect.height() < 16) {
                    remaining_nodes_are_too_small = true;
                } else {
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
        callback(node, 0, canvas, depth, IsVisualLeaf::No, IsRemainder::Yes);
    }
}

const TreeMapNode* TreeMapWidget::path_node(size_t n) const
{
    if (!m_tree.ptr())
        return nullptr;
    const TreeMapNode* iter = &m_tree->root();
    size_t path_index = 0;
    while (iter && path_index < m_path.size() && path_index < n) {
        size_t child_index = m_path[path_index];
        if (child_index >= iter->num_children()) {
            return nullptr;
        }
        iter = &iter->child_at(child_index);
        path_index++;
    }
    return iter;
}

void TreeMapWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    m_selected_node_cache = path_node(m_path.size());

    const TreeMapNode* node = path_node(m_viewpoint);
    if (!node) {
        painter.fill_rect(frame_inner_rect(), Color::MidGray);
    } else if (node_is_leaf(*node)) {
        paint_cell_frame(painter, *node, frame_inner_rect(), m_viewpoint - 1, true);
    } else {
        lay_out_children(*node, frame_inner_rect(), m_viewpoint, [&](const TreeMapNode& node, int, const Gfx::IntRect& rect, int depth, IsVisualLeaf visual_leaf, IsRemainder remainder) {
            if (remainder == IsRemainder::No) {
                bool fill = visual_leaf == IsVisualLeaf::Yes ? true : false;
                paint_cell_frame(painter, node, rect, depth, fill);
            } else {
                Color color = colors[depth % (sizeof(colors) / sizeof(colors[0]))];
                painter.clear_clip_rect();
                painter.add_clip_rect(rect);
                painter.draw_rect(rect, Color::Black);
                painter.fill_rect_with_dither_pattern(rect.shrunken(2, 2), color, Color::Black);
            }
        });
    }
}

Vector<int> TreeMapWidget::path_to_position(const Gfx::IntPoint& position)
{
    const TreeMapNode* node = path_node(m_viewpoint);
    if (!node) {
        return {};
    }
    Vector<int> path;
    lay_out_children(*node, frame_inner_rect(), m_viewpoint, [&](const TreeMapNode&, int index, const Gfx::IntRect& rect, int, IsVisualLeaf, IsRemainder is_remainder) {
        if (is_remainder == IsRemainder::No && rect.contains(position)) {
            path.append(index);
        }
    });
    return path;
}

void TreeMapWidget::mousedown_event(GUI::MouseEvent& event)
{
    const TreeMapNode* node = path_node(m_viewpoint);
    if (node && !node_is_leaf(*node)) {
        Vector<int> path = path_to_position(event.position());
        if (!path.is_empty()) {
            m_path.shrink(m_viewpoint);
            m_path.append(path);
            if (on_path_change) {
                on_path_change();
            }
            update();
        }
    }
}

void TreeMapWidget::doubleclick_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Left)
        return;
    const TreeMapNode* node = path_node(m_viewpoint);
    if (node && !node_is_leaf(*node)) {
        Vector<int> path = path_to_position(event.position());
        m_path.shrink(m_viewpoint);
        m_path.append(path);
        m_viewpoint = m_path.size();
        if (on_path_change) {
            on_path_change();
        }
        update();
    }
}

void TreeMapWidget::mousewheel_event(GUI::MouseEvent& event)
{
    int delta = event.wheel_delta();
    // FIXME: The wheel_delta is premultiplied in the window server, we actually want a raw value here.
    int step_size = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetScrollStepSize>()->step_size();
    if (delta > 0) {
        size_t step_back = delta / step_size;
        if (step_back > m_viewpoint)
            step_back = m_viewpoint;
        set_viewpoint(m_viewpoint - step_back);
    } else {
        size_t step_up = (-delta) / step_size;
        set_viewpoint(m_viewpoint + step_up);
    }
}

void TreeMapWidget::context_menu_event(GUI::ContextMenuEvent& context_menu_event)
{
    if (on_context_menu_request)
        on_context_menu_request(context_menu_event);
}

void TreeMapWidget::set_tree(RefPtr<TreeMap> tree)
{
    m_tree = tree;
    m_path.clear();
    m_viewpoint = 0;
    if (on_path_change) {
        on_path_change();
    }
    update();
}

void TreeMapWidget::set_viewpoint(size_t viewpoint)
{
    if (viewpoint > m_path.size())
        viewpoint = m_path.size();
    m_viewpoint = viewpoint;
    if (on_path_change) {
        on_path_change();
    }
    update();
}

size_t TreeMapWidget::path_size() const
{
    return m_path.size() + 1;
}

size_t TreeMapWidget::viewpoint() const
{
    return m_viewpoint;
}

}
