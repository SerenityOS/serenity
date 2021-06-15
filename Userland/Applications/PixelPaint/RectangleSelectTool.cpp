/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectangleSelectTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Painter.h>

namespace PixelPaint {

RectangleSelectTool::RectangleSelectTool()
{
}

RectangleSelectTool::~RectangleSelectTool()
{
}

void RectangleSelectTool::on_mousedown(Layer&, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (image_event.button() != GUI::MouseButton::Left)
        return;

    m_selecting = true;
    m_editor->selection().begin_interactive_selection();

    m_selection_start = image_event.position();
    m_selection_end = image_event.position();
    m_editor->update();
}

void RectangleSelectTool::on_mousemove(Layer&, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (!m_selecting)
        return;

    m_selection_end = image_event.position();
    m_editor->update();
}

void RectangleSelectTool::on_mouseup(Layer& layer, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (!m_selecting || image_event.button() != GUI::MouseButton::Left)
        return;

    m_selecting = false;
    m_editor->selection().end_interactive_selection();

    m_editor->update();

    auto rect_in_image = Gfx::IntRect::from_two_points(m_selection_start, m_selection_end);
    if (m_active_layer_only) {
        auto layer_rect = layer.rect();
        auto layer_location = layer.location();
        layer_rect.translate_by(layer_location.x(), layer_location.y());
        rect_in_image.intersect(layer_rect);
    }
    m_editor->selection().set(rect_in_image);
}

void RectangleSelectTool::on_second_paint(Layer const&, GUI::PaintEvent& event)
{
    if (!m_selecting)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());

    auto rect_in_image = Gfx::IntRect::from_two_points(m_selection_start, m_selection_end);
    auto rect_in_editor = m_editor->image_rect_to_editor_rect(rect_in_image);

    m_editor->selection().draw_marching_ants(painter, rect_in_editor.to_type<int>());
}

GUI::Widget* RectangleSelectTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& active_layer_only_checkbox = m_properties_widget->add<GUI::CheckBox>("Only Active Layer");
        active_layer_only_checkbox.set_checked(true);
        active_layer_only_checkbox.set_fixed_height(20);
        active_layer_only_checkbox.on_checked = [this](bool checked) {
            m_active_layer_only = checked;
            m_editor->selection().clear();
        };
    }

    return m_properties_widget.ptr();
}

}
