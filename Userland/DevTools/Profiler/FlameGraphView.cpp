/*
 * Copyright (c) 2021, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FlameGraphView.h"
#include "DevTools/Profiler/Profile.h"
#include "LibGfx/Forward.h"
#include <AK/Function.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/FontDatabase.h>
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

    m_model.register_client(*this);

    m_colors = get_colors();
    layout_bars();
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
        if (bar.rect.contains(event.x(), event.y())) {
            hovered_bar = &bar;
            break;
        }
    }

    if (m_hovered_bar == hovered_bar)
        return;

    m_hovered_bar = hovered_bar;

    if (on_hover_change)
        on_hover_change();

    String label = "";
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

void FlameGraphView::resize_event(GUI::ResizeEvent&)
{
    layout_bars();
}

void FlameGraphView::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    for (auto const& bar : m_bars) {
        auto label = bar_label(bar);

        auto color = m_colors[label.hash() % m_colors.size()];

        if (&bar == m_hovered_bar)
            color = color.lightened(1.2f);

        if (bar.selected)
            color = color.with_alpha(128);

        // Do rounded corners if the node will draw with enough width
        if (bar.rect.width() > (bar_rounding * 3))
            painter.fill_rect_with_rounded_corners(bar.rect.shrunken(0, bar_margin), color, bar_rounding);
        else
            painter.fill_rect(bar.rect.shrunken(0, bar_margin), color);

        if (bar.rect.width() > text_threshold) {
            painter.draw_text(
                bar.rect.shrunken(bar_padding, 0),
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
    String label = "All";
    if (label_index.is_valid()) {
        label = m_model.data(label_index).to_string();
    }
    return label;
}

void FlameGraphView::layout_bars()
{
    m_bars.clear();

    // Explicit copy here so the layout can mutate
    Vector<GUI::ModelIndex> selected = m_selected_indexes;
    GUI::ModelIndex null_index;
    layout_children(null_index, 0, 0, this->width(), selected);
}

void FlameGraphView::layout_children(GUI::ModelIndex& index, int depth, int left, int right, Vector<GUI::ModelIndex>& selected_nodes)
{
    auto available_width = right - left;
    if (available_width < 1)
        return;

    auto y = this->height() - (bar_height * depth) - bar_height;
    if (y < 0)
        return;

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
