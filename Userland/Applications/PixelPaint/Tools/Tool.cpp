/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>

namespace PixelPaint {

void Tool::setup(ImageEditor& editor)
{
    m_editor = editor;
}

void Tool::set_action(GUI::Action* action)
{
    m_action = action;
}

bool Tool::on_keydown(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_LeftBracket:
        if (m_primary_slider) {
            m_primary_slider->decrease_slider_by(1);
            return true;
        }
        break;
    case KeyCode::Key_RightBracket:
        if (m_primary_slider) {
            m_primary_slider->increase_slider_by(1);
            return true;
        }
        break;
    case KeyCode::Key_LeftBrace:
        if (m_secondary_slider) {
            m_secondary_slider->decrease_slider_by(1);
            return true;
        }
        break;
    case KeyCode::Key_RightBrace:
        if (m_secondary_slider) {
            m_secondary_slider->increase_slider_by(1);
            return true;
        }
        break;
    default:
        break;
    }

    return false;
}

Gfx::IntPoint Tool::editor_layer_location(Layer const& layer) const
{
    return (Gfx::FloatPoint { layer.location() } * m_editor->scale()).to_rounded<int>();
}

Gfx::IntPoint Tool::editor_stroke_position(Gfx::IntPoint pixel_coords, int stroke_thickness) const
{
    auto position = m_editor->content_to_frame_position(pixel_coords);
    auto offset = (stroke_thickness % 2 == 0) ? 0 : m_editor->scale() / 2;
    position = position.translated(offset, offset);
    return position.to_type<int>();
}

Gfx::IntPoint Tool::constrain_line_angle(Gfx::IntPoint start_pos, Gfx::IntPoint end_pos, float angle_increment)
{
    float current_angle = AK::atan2<float>(end_pos.y() - start_pos.y(), end_pos.x() - start_pos.x()) + float { M_PI * 2 };

    float constrained_angle = ((int)((current_angle + angle_increment / 2) / angle_increment)) * angle_increment;

    auto diff = end_pos - start_pos;
    float line_length = AK::hypot<float>(diff.x(), diff.y());

    return { start_pos.x() + (int)(AK::cos(constrained_angle) * line_length),
        start_pos.y() + (int)(AK::sin(constrained_angle) * line_length) };
}

template<>
void Tool::set_pixel_with_possible_mask<Gfx::StorageFormat::BGRA8888>(int x, int y, Gfx::Color color, Gfx::Bitmap& bitmap)
{
    if (!m_editor || !m_editor->active_layer())
        return;

    switch (m_editor->active_layer()->edit_mode()) {
    case Layer::EditMode::Content:
        if (m_editor->active_layer()->mask_type() == Layer::MaskType::EditingMask)
            bitmap.set_pixel<Gfx::StorageFormat::BGRA8888>(x, y, m_editor->active_layer()->modify_pixel_with_editing_mask(x, y, color, bitmap.get_pixel(x, y)));
        else
            bitmap.set_pixel(x, y, color);
        break;
    case Layer::EditMode::Mask:
        bitmap.set_pixel<Gfx::StorageFormat::BGRA8888>(x, y, color);
        break;
    }
}

void Tool::set_pixel_with_possible_mask(int x, int y, Gfx::Color color, Gfx::Bitmap& bitmap)
{
    if (!m_editor || !m_editor->active_layer())
        return;

    switch (m_editor->active_layer()->edit_mode()) {
    case Layer::EditMode::Content:
        if (m_editor->active_layer()->mask_type() == Layer::MaskType::EditingMask)
            bitmap.set_pixel(x, y, m_editor->active_layer()->modify_pixel_with_editing_mask(x, y, color, bitmap.get_pixel(x, y)));
        else
            bitmap.set_pixel(x, y, color);
        break;
    case Layer::EditMode::Mask:
        bitmap.set_pixel(x, y, color);
        break;
    }
}

}
