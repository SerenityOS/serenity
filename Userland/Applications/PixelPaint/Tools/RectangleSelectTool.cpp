/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectangleSelectTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>

namespace PixelPaint {

void RectangleSelectTool::on_mousedown(Layer*, MouseEvent& event)
{
    auto& image_event = event.image_event();
    if (image_event.button() != GUI::MouseButton::Primary)
        return;

    m_selecting = true;
    m_editor->image().selection().begin_interactive_selection();

    m_selection_start = image_event.position();
    m_selection_end = image_event.position();
    m_editor->update();
}

void RectangleSelectTool::on_mousemove(Layer*, MouseEvent& event)
{
    auto& image_event = event.image_event();
    if (!m_selecting)
        return;

    if (m_moving_mode != MovingMode::None) {
        auto delta = m_selection_end - image_event.position();
        if (m_moving_mode == MovingMode::MovingOrigin)
            m_selection_start -= delta;
        else if (m_moving_mode == MovingMode::AroundCenter)
            m_selection_start += delta;
    }

    m_selection_end = image_event.position();
    m_editor->update();
}

void RectangleSelectTool::on_mouseup(Layer*, MouseEvent& event)
{
    auto& image_event = event.image_event();
    if (!m_selecting || image_event.button() != GUI::MouseButton::Primary)
        return;

    m_selecting = false;
    m_editor->image().selection().end_interactive_selection();

    m_editor->update();

    auto rect_in_image = selection_rect();

    auto mask = Mask::full(rect_in_image);

    auto feathering = ((mask.bounding_rect().size().to_type<float>() * .5f) * m_edge_feathering).to_type<int>();

    // Multiply the alpha instead of setting it to ensure corners are feathered correctly
    auto multiply_alpha = [&mask](int x, int y, float alpha) {
        Gfx::IntPoint point { x, y };
        point += mask.bounding_rect().top_left();

        float old_alpha = mask.getf(point);
        mask.setf(point, old_alpha * alpha);
    };

    // Horizontal feathering
    for (int offset = 0; offset < feathering.width(); offset++) {
        // Add 1 to offset before dividing to ensure the first pixel won't always be transparent
        float alpha = (float)(offset + 1) / (float)feathering.width();

        for (int y = 0; y < mask.bounding_rect().height(); y++) {
            multiply_alpha(offset, y, alpha);
            multiply_alpha(mask.bounding_rect().width() - offset - 1, y, alpha);
        }
    }

    // Vertical feathering
    for (int offset = 0; offset < feathering.height(); offset++) {
        // Add 1 to offset before dividing to ensure the first pixel won't always be transparent
        float alpha = (float)(offset + 1) / (float)feathering.height();

        for (int x = 0; x < mask.bounding_rect().width(); x++) {
            multiply_alpha(x, offset, alpha);
            multiply_alpha(x, mask.bounding_rect().height() - offset - 1, alpha);
        }
    }

    m_editor->image().selection().merge(mask, m_merge_mode);

    m_editor->did_complete_action(tool_name());
}

bool RectangleSelectTool::on_keydown(GUI::KeyEvent& key_event)
{
    if (key_event.key() == KeyCode::Key_Space) {
        m_moving_mode = MovingMode::MovingOrigin;
        return true;
    }
    if (key_event.key() == KeyCode::Key_LeftControl) {
        m_moving_mode = MovingMode::AroundCenter;
        return true;
    }

    if (key_event.key() == KeyCode::Key_Escape) {
        if (m_selecting)
            m_selecting = false;
        else
            m_editor->image().selection().clear();
        return true;
    }

    return Tool::on_keydown(key_event);
}

void RectangleSelectTool::on_keyup(GUI::KeyEvent& key_event)
{
    if (key_event.key() == KeyCode::Key_Space && m_moving_mode == MovingMode::MovingOrigin)
        m_moving_mode = MovingMode::None;
    else if (key_event.key() == KeyCode::Key_LeftControl && m_moving_mode == MovingMode::AroundCenter)
        m_moving_mode = MovingMode::None;
}

void RectangleSelectTool::on_second_paint(Layer const*, GUI::PaintEvent& event)
{
    if (!m_selecting)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());

    auto rect_in_image = selection_rect();
    if (rect_in_image.is_empty())
        return;

    auto rect_in_editor = m_editor->content_to_frame_rect(rect_in_image);

    m_editor->draw_marching_ants(painter, rect_in_editor.to_rounded<int>());
}

NonnullRefPtr<GUI::Widget> RectangleSelectTool::get_properties_widget()
{
    if (m_properties_widget) {
        return *m_properties_widget.ptr();
    }

    auto properties_widget = GUI::Widget::construct();
    properties_widget->set_layout<GUI::VerticalBoxLayout>();

    auto& feather_container = properties_widget->add<GUI::Widget>();
    feather_container.set_fixed_height(20);
    feather_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& feather_label = feather_container.add<GUI::Label>();
    feather_label.set_text("Feather:"_string);
    feather_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    feather_label.set_fixed_size(80, 20);

    int const feather_slider_max = 100;
    auto& feather_slider = feather_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
    feather_slider.set_range(0, feather_slider_max);
    feather_slider.set_value((int)floorf(m_edge_feathering * (float)feather_slider_max));

    feather_slider.on_change = [this](int value) {
        m_edge_feathering = (float)value / (float)feather_slider_max;
    };
    set_primary_slider(&feather_slider);

    auto& mode_container = properties_widget->add<GUI::Widget>();
    mode_container.set_fixed_height(20);
    mode_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& mode_label = mode_container.add<GUI::Label>();
    mode_label.set_text("Mode:"_string);
    mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    mode_label.set_fixed_size(80, 20);

    for (int i = 0; i < (int)Selection::MergeMode::__Count; i++) {
        switch ((Selection::MergeMode)i) {
        case Selection::MergeMode::Set:
            m_merge_mode_names.append("Set");
            break;
        case Selection::MergeMode::Add:
            m_merge_mode_names.append("Add");
            break;
        case Selection::MergeMode::Subtract:
            m_merge_mode_names.append("Subtract");
            break;
        case Selection::MergeMode::Intersect:
            m_merge_mode_names.append("Intersect");
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    auto& mode_combo = mode_container.add<GUI::ComboBox>();
    mode_combo.set_only_allow_values_from_model(true);
    mode_combo.set_model(*GUI::ItemListModel<ByteString>::create(m_merge_mode_names));
    mode_combo.set_selected_index((int)m_merge_mode);
    mode_combo.on_change = [this](auto&&, GUI::ModelIndex const& index) {
        VERIFY(index.row() >= 0);
        VERIFY(index.row() < (int)Selection::MergeMode::__Count);

        m_merge_mode = (Selection::MergeMode)index.row();
    };

    m_properties_widget = properties_widget;
    return *m_properties_widget;
}

Gfx::IntPoint RectangleSelectTool::point_position_to_preferred_cell(Gfx::FloatPoint position) const
{
    return position.to_rounded<int>();
}

Gfx::IntRect RectangleSelectTool::selection_rect() const
{
    auto image_rect = m_editor->image().rect();
    auto unconstrained_selection_rect = Gfx::IntRect::from_two_points(m_selection_start, m_selection_end);
    if (!unconstrained_selection_rect.intersects(image_rect))
        return {};

    return unconstrained_selection_rect.intersected(image_rect);
}

}
