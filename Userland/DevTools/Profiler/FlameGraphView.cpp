/*
 * Copyright (c) 2021, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FlameGraphView.h"
#include "Profile.h"
#include <AK/Function.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Palette.h>

namespace Profiler {

constexpr int bar_rounding = 2;
constexpr int bar_margin = 2;
constexpr int bar_padding = 8;
constexpr int bar_height = 20;
constexpr int text_threshold = 30;

Vector<Gfx::Color> s_colors;

static Vector<Gfx::Color> const& get_colors()
{
    if (s_colors.is_empty()) {
        // Start with a nice orange, then make shades of it
        Gfx::Color midpoint(255, 94, 19);
        s_colors.extend(midpoint.shades(3, 0.5f));
        s_colors.append(midpoint);
        s_colors.extend(midpoint.tints(3, 0.5f));
    }

    return s_colors;
}

FlameGraphView::FlameGraphView(GUI::Model& model, int text_column, int width_column)
    : m_model(model)
    , m_text_column(text_column)
    , m_width_column(width_column)
{
    set_fill_with_background_color(true);
    set_background_role(Gfx::ColorRole::Base);
    set_scrollbars_enabled(true);
    set_frame_style(Gfx::FrameStyle::NoFrame);
    set_should_hide_unnecessary_scrollbars(false);
    horizontal_scrollbar().set_visible(false);

    m_model.register_client(*this);

    m_colors = get_colors();
    layout_bars();
    scroll_to_bottom();
}

GUI::ModelIndex FlameGraphView::hovered_index() const
{
    if (!m_hovered_bar)
        return GUI::ModelIndex();
    return m_hovered_bar->index;
}

void FlameGraphView::model_did_update(unsigned)
{
    m_selected_indexes.clear();
    layout_bars();
    update();
}

void FlameGraphView::mousemove_event(GUI::MouseEvent& event)
{
    StackBar* hovered_bar = nullptr;

    for (size_t i = 0; i < m_bars.size(); ++i) {
        auto& bar = m_bars[i];
        if (to_widget_rect(bar.rect).contains(event.x(), event.y())) {
            hovered_bar = &bar;
            break;
        }
    }

    if (m_hovered_bar == hovered_bar)
        return;

    m_hovered_bar = hovered_bar;

    if (on_hover_change)
        on_hover_change();

    String label;
    if (m_hovered_bar != nullptr && m_hovered_bar->index.is_valid()) {
        label = bar_label(*m_hovered_bar);
    }
    set_tooltip(label);
    show_or_hide_tooltip();

    update();
}

void FlameGraphView::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;

    if (!m_hovered_bar)
        return;

    m_selected_indexes.clear();
    GUI::ModelIndex selected_index = m_hovered_bar->index;
    while (selected_index.is_valid()) {
        m_selected_indexes.append(selected_index);
        selected_index = selected_index.parent();
    }

    layout_bars();
    update();
}

void FlameGraphView::resize_event(GUI::ResizeEvent& event)
{
    auto old_scroll = vertical_scrollbar().value();

    AbstractScrollableWidget::resize_event(event);

    // Adjust scroll to keep the bottom of the graph fixed
    auto available_height_delta = m_old_available_size.height() - available_size().height();

    vertical_scrollbar().set_value(old_scroll + available_height_delta);

    layout_bars();

    m_old_available_size = available_size();
}

void FlameGraphView::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto content_clip_rect = to_content_rect(event.rect());

    for (auto const& bar : m_bars) {
        if (!content_clip_rect.intersects_vertically(bar.rect))
            continue;

        auto label = bar_label(bar);

        auto color = m_colors[label.hash() % m_colors.size()];

        if (&bar == m_hovered_bar)
            color = color.lightened(1.2f);

        if (bar.selected)
            color = color.with_alpha(128);

        auto rect = to_widget_rect(bar.rect);

        // Do rounded corners if the node will draw with enough width
        if (rect.width() > (bar_rounding * 3))
            painter.fill_rect_with_rounded_corners(rect.shrunken(0, bar_margin), color, bar_rounding);
        else
            painter.fill_rect(rect.shrunken(0, bar_margin), color);

        if (rect.width() > text_threshold) {
            painter.draw_text(
                rect.shrunken(bar_padding, 0),
                label,
                painter.font(),
                Gfx::TextAlignment::CenterLeft,
                Gfx::Color::Black,
                Gfx::TextElision::Right);
        }
    }
}

String FlameGraphView::bar_label(StackBar const& bar) const
{
    auto label_index = bar.index.sibling_at_column(m_text_column);
    String label = "All"_string;
    if (label_index.is_valid()) {
        label = MUST(String::from_byte_string(m_model.data(label_index).to_byte_string()));
    }
    return label;
}

void FlameGraphView::layout_bars()
{
    m_bars.clear();
    m_hovered_bar = nullptr;

    // Explicit copy here so the layout can mutate
    Vector<GUI::ModelIndex> selected = m_selected_indexes;
    GUI::ModelIndex null_index;
    layout_children(null_index, 0, 0, available_size().width(), selected);

    // Translate bars from (-height..0) to (0..height) now that we know the height,
    // use available height as minimum to keep the graph at the bottom when it's small
    int height = available_size().height();

    for (auto& bar : m_bars)
        height = max(height, -bar.rect.top());
    for (auto& bar : m_bars)
        bar.rect.translate_by(0, height);

    // Update scrollbars if height changed
    if (height != content_size().height()) {
        auto old_content_height = content_size().height();
        auto old_scroll = vertical_scrollbar().value();

        set_content_size(Gfx::IntSize(available_size().width(), height));

        // Adjust scroll to keep the bottom of the graph fixed, so it doesn't jump
        // around when double-clicking
        auto content_height_delta = old_content_height - content_size().height();

        vertical_scrollbar().set_value(old_scroll - content_height_delta);
    }
}

void FlameGraphView::layout_children(GUI::ModelIndex& index, int depth, int left, int right, Vector<GUI::ModelIndex>& selected_nodes)
{
    auto available_width = right - left;
    if (available_width < 1)
        return;

    auto y = -(bar_height * depth) - bar_height;

    u32 node_event_count = 0;
    if (!index.is_valid()) {
        // We're at the root, so calculate the event count across all roots
        for (auto i = 0; i < m_model.row_count(index); ++i) {
            auto const& root = *static_cast<ProfileNode const*>(m_model.index(i).internal_data());
            node_event_count += root.event_count();
        }
        m_bars.append({ {}, { left, y, available_width, bar_height }, false });
    } else {
        auto const* node = static_cast<ProfileNode const*>(index.internal_data());

        bool selected = !selected_nodes.is_empty();
        if (selected) {
            VERIFY(selected_nodes.take_last() == index);
        }

        node_event_count = node->event_count();

        Gfx::IntRect node_rect { left, y, available_width, bar_height };
        m_bars.append({ index, node_rect, selected });
    }

    float width_per_sample = static_cast<float>(available_width) / node_event_count;
    float new_left = static_cast<float>(left);

    for (auto i = 0; i < m_model.row_count(index); ++i) {
        auto child_index = m_model.index(i, 0, index);
        if (!child_index.is_valid())
            continue;

        if (!selected_nodes.is_empty()) {
            if (selected_nodes.last() != child_index)
                continue;

            layout_children(child_index, depth + 1, left, right, selected_nodes);
            return;
        }

        auto const* child = static_cast<ProfileNode const*>(child_index.internal_data());
        float child_width = width_per_sample * child->event_count();
        layout_children(child_index, depth + 1, static_cast<int>(new_left), static_cast<int>(new_left + child_width), selected_nodes);
        new_left += child_width;
    }
}

}
