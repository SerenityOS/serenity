/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageEditor.h"
#include "Image.h"
#include "Layer.h"
#include "MoveTool.h"
#include "Tool.h"
#include <LibGUI/Command.h>
#include <LibGUI/Painter.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

ImageEditor::ImageEditor(NonnullRefPtr<Image> image)
    : m_image(move(image))
    , m_undo_stack(make<GUI::UndoStack>())
    , m_selection(*this)
{
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    m_undo_stack = make<GUI::UndoStack>();
    m_undo_stack->push(make<ImageUndoCommand>(*m_image));
    m_image->add_client(*this);
}

ImageEditor::~ImageEditor()
{
    m_image->remove_client(*this);
}

void ImageEditor::did_complete_action()
{
    m_undo_stack->push(make<ImageUndoCommand>(*m_image));
}

bool ImageEditor::undo()
{
    if (m_undo_stack->can_undo()) {
        m_undo_stack->undo();
        layers_did_change();
        return true;
    }
    return false;
}

bool ImageEditor::redo()
{
    if (m_undo_stack->can_redo()) {
        m_undo_stack->redo();
        layers_did_change();
        return true;
    }
    return false;
}

void ImageEditor::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    {
        Gfx::DisjointRectSet background_rects;
        background_rects.add(frame_inner_rect());
        background_rects.shatter(m_editor_image_rect);
        for (auto& rect : background_rects.rects())
            painter.fill_rect(rect, palette().color(Gfx::ColorRole::Tray));
    }

    Gfx::StylePainter::paint_transparency_grid(painter, m_editor_image_rect, palette());

    painter.draw_rect(m_editor_image_rect.inflated(2, 2), Color::Black);
    m_image->paint_into(painter, m_editor_image_rect);

    if (m_active_layer) {
        painter.draw_rect(enclosing_int_rect(image_rect_to_editor_rect(m_active_layer->relative_rect())).inflated(2, 2), Color::Black);
    }

    if (m_show_guides) {
        for (auto& guide : m_guides) {
            if (guide.orientation() == Guide::Orientation::Horizontal) {
                int y_coordinate = (int)image_position_to_editor_position({ 0.0f, guide.offset() }).y();
                painter.draw_line({ 0, y_coordinate }, { rect().width(), y_coordinate }, Color::Cyan, 1, Gfx::Painter::LineStyle::Dashed, Color::LightGray);
            } else if (guide.orientation() == Guide::Orientation::Vertical) {
                int x_coordinate = (int)image_position_to_editor_position({ guide.offset(), 0.0f }).x();
                painter.draw_line({ x_coordinate, 0 }, { x_coordinate, rect().height() }, Color::Cyan, 1, Gfx::Painter::LineStyle::Dashed, Color::LightGray);
            }
        }
    }

    if (!m_selection.is_empty())
        m_selection.paint(painter);
}

Gfx::FloatRect ImageEditor::layer_rect_to_editor_rect(Layer const& layer, Gfx::IntRect const& layer_rect) const
{
    return image_rect_to_editor_rect(layer_rect.translated(layer.location()));
}

Gfx::FloatRect ImageEditor::image_rect_to_editor_rect(Gfx::IntRect const& image_rect) const
{
    Gfx::FloatRect editor_rect;
    editor_rect.set_location(image_position_to_editor_position(image_rect.location()));
    editor_rect.set_width((float)image_rect.width() * m_scale);
    editor_rect.set_height((float)image_rect.height() * m_scale);
    return editor_rect;
}

Gfx::FloatRect ImageEditor::editor_rect_to_image_rect(Gfx::IntRect const& editor_rect) const
{
    Gfx::FloatRect image_rect;
    image_rect.set_location(editor_position_to_image_position(editor_rect.location()));
    image_rect.set_width((float)editor_rect.width() / m_scale);
    image_rect.set_height((float)editor_rect.height() / m_scale);
    return image_rect;
}

Gfx::FloatPoint ImageEditor::layer_position_to_editor_position(Layer const& layer, Gfx::IntPoint const& layer_position) const
{
    return image_position_to_editor_position(layer_position.translated(layer.location()));
}

Gfx::FloatPoint ImageEditor::image_position_to_editor_position(Gfx::IntPoint const& image_position) const
{
    Gfx::FloatPoint editor_position;
    editor_position.set_x(m_editor_image_rect.x() + ((float)image_position.x() * m_scale));
    editor_position.set_y(m_editor_image_rect.y() + ((float)image_position.y() * m_scale));
    return editor_position;
}

Gfx::FloatPoint ImageEditor::editor_position_to_image_position(Gfx::IntPoint const& editor_position) const
{
    Gfx::FloatPoint image_position;
    image_position.set_x(((float)editor_position.x() - m_editor_image_rect.x()) / m_scale);
    image_position.set_y(((float)editor_position.y() - m_editor_image_rect.y()) / m_scale);
    return image_position;
}

void ImageEditor::second_paint_event(GUI::PaintEvent& event)
{
    if (m_active_tool)
        m_active_tool->on_second_paint(m_active_layer, event);
}

GUI::MouseEvent ImageEditor::event_with_pan_and_scale_applied(GUI::MouseEvent const& event) const
{
    auto image_position = editor_position_to_image_position(event.position());
    return {
        static_cast<GUI::Event::Type>(event.type()),
        Gfx::IntPoint(image_position.x(), image_position.y()),
        event.buttons(),
        event.button(),
        event.modifiers(),
        event.wheel_delta()
    };
}

GUI::MouseEvent ImageEditor::event_adjusted_for_layer(GUI::MouseEvent const& event, Layer const& layer) const
{
    auto image_position = editor_position_to_image_position(event.position());
    image_position.translate_by(-layer.location().x(), -layer.location().y());
    return {
        static_cast<GUI::Event::Type>(event.type()),
        Gfx::IntPoint(image_position.x(), image_position.y()),
        event.buttons(),
        event.button(),
        event.modifiers(),
        event.wheel_delta()
    };
}

void ImageEditor::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Middle) {
        m_click_position = event.position();
        m_saved_pan_origin = m_pan_origin;
        set_override_cursor(Gfx::StandardCursor::Drag);
        return;
    }

    if (!m_active_tool)
        return;

    if (is<MoveTool>(*m_active_tool)) {
        if (auto* other_layer = layer_at_editor_position(event.position())) {
            set_active_layer(other_layer);
        }
    }

    auto layer_event = m_active_layer ? event_adjusted_for_layer(event, *m_active_layer) : event;
    auto image_event = event_with_pan_and_scale_applied(event);
    Tool::MouseEvent tool_event(Tool::MouseEvent::Action::MouseDown, layer_event, image_event, event);
    m_active_tool->on_mousedown(m_active_layer.ptr(), tool_event);
}

void ImageEditor::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() & GUI::MouseButton::Middle) {
        auto delta = event.position() - m_click_position;
        m_pan_origin = m_saved_pan_origin.translated(
            -delta.x() / m_scale,
            -delta.y() / m_scale);

        relayout();
        return;
    }

    if (!m_active_tool)
        return;

    auto layer_event = m_active_layer ? event_adjusted_for_layer(event, *m_active_layer) : event;
    auto image_event = event_with_pan_and_scale_applied(event);
    Tool::MouseEvent tool_event(Tool::MouseEvent::Action::MouseDown, layer_event, image_event, event);
    m_active_tool->on_mousemove(m_active_layer.ptr(), tool_event);

    if (on_image_mouse_position_change) {
        on_image_mouse_position_change(image_event.position());
    }
}

void ImageEditor::mouseup_event(GUI::MouseEvent& event)
{
    set_override_cursor(m_active_cursor);

    if (!m_active_tool)
        return;
    auto layer_event = m_active_layer ? event_adjusted_for_layer(event, *m_active_layer) : event;
    auto image_event = event_with_pan_and_scale_applied(event);
    Tool::MouseEvent tool_event(Tool::MouseEvent::Action::MouseDown, layer_event, image_event, event);
    m_active_tool->on_mouseup(m_active_layer.ptr(), tool_event);
}

void ImageEditor::mousewheel_event(GUI::MouseEvent& event)
{
    auto scale_delta = -event.wheel_delta() * 0.1f;
    scale_centered_on_position(event.position(), scale_delta);
}

void ImageEditor::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (!m_active_tool)
        return;
    m_active_tool->on_context_menu(m_active_layer, event);
}

void ImageEditor::resize_event(GUI::ResizeEvent& event)
{
    relayout();
    GUI::Frame::resize_event(event);
}

void ImageEditor::keydown_event(GUI::KeyEvent& event)
{
    if (m_active_tool)
        m_active_tool->on_keydown(event);
}

void ImageEditor::keyup_event(GUI::KeyEvent& event)
{
    if (m_active_tool)
        m_active_tool->on_keyup(event);
}

void ImageEditor::enter_event(Core::Event&)
{
    set_override_cursor(m_active_cursor);
}

void ImageEditor::leave_event(Core::Event&)
{
    set_override_cursor(Gfx::StandardCursor::None);

    if (on_leave)
        on_leave();
}

void ImageEditor::set_active_layer(Layer* layer)
{
    if (m_active_layer == layer)
        return;
    m_active_layer = layer;

    if (m_active_layer) {
        VERIFY(&m_active_layer->image() == m_image.ptr());
        size_t index = 0;
        for (; index < m_image->layer_count(); ++index) {
            if (&m_image->layer(index) == layer)
                break;
        }
        if (on_active_layer_change)
            on_active_layer_change(layer);
    } else {
        if (on_active_layer_change)
            on_active_layer_change({});
    }

    layers_did_change();
}

void ImageEditor::set_active_tool(Tool* tool)
{
    if (m_active_tool == tool)
        return;

    if (m_active_tool)
        m_active_tool->clear();

    m_active_tool = tool;

    if (m_active_tool) {
        m_active_tool->setup(*this);
        m_active_tool->on_tool_activation();
        m_active_cursor = m_active_tool->cursor();
        set_override_cursor(m_active_cursor);
    }
}

void ImageEditor::set_guide_visibility(bool show_guides)
{
    if (m_show_guides == show_guides)
        return;

    m_show_guides = show_guides;

    if (on_set_guide_visibility)
        on_set_guide_visibility(m_show_guides);

    update();
}

void ImageEditor::layers_did_change()
{
    update();
}

Color ImageEditor::color_for(GUI::MouseButton button) const
{
    if (button == GUI::MouseButton::Left)
        return m_primary_color;
    if (button == GUI::MouseButton::Right)
        return m_secondary_color;
    VERIFY_NOT_REACHED();
}

Color ImageEditor::color_for(GUI::MouseEvent const& event) const
{
    if (event.buttons() & GUI::MouseButton::Left)
        return m_primary_color;
    if (event.buttons() & GUI::MouseButton::Right)
        return m_secondary_color;
    VERIFY_NOT_REACHED();
}

void ImageEditor::set_primary_color(Color color)
{
    if (m_primary_color == color)
        return;
    m_primary_color = color;
    if (on_primary_color_change)
        on_primary_color_change(color);
}

void ImageEditor::set_secondary_color(Color color)
{
    if (m_secondary_color == color)
        return;
    m_secondary_color = color;
    if (on_secondary_color_change)
        on_secondary_color_change(color);
}

Layer* ImageEditor::layer_at_editor_position(Gfx::IntPoint const& editor_position)
{
    auto image_position = editor_position_to_image_position(editor_position);
    for (ssize_t i = m_image->layer_count() - 1; i >= 0; --i) {
        auto& layer = m_image->layer(i);
        if (!layer.is_visible())
            continue;
        if (layer.relative_rect().contains(Gfx::IntPoint(image_position.x(), image_position.y())))
            return const_cast<Layer*>(&layer);
    }
    return nullptr;
}

void ImageEditor::clamped_scale(float scale_delta)
{
    m_scale *= YAK::exp2(scale_delta);
    if (m_scale < 0.1f)
        m_scale = 0.1f;
    if (m_scale > 100.0f)
        m_scale = 100.0f;
}

void ImageEditor::scale_centered_on_position(Gfx::IntPoint const& position, float scale_delta)
{
    auto old_scale = m_scale;
    clamped_scale(scale_delta);

    Gfx::FloatPoint focus_point {
        m_pan_origin.x() - (position.x() - width() / 2.0f) / old_scale,
        m_pan_origin.y() - (position.y() - height() / 2.0f) / old_scale
    };

    m_pan_origin = Gfx::FloatPoint(
        focus_point.x() - m_scale / old_scale * (focus_point.x() - m_pan_origin.x()),
        focus_point.y() - m_scale / old_scale * (focus_point.y() - m_pan_origin.y()));

    if (old_scale != m_scale)
        relayout();
}

void ImageEditor::scale_by(float scale_delta)
{
    if (scale_delta != 0) {
        clamped_scale(scale_delta);
        relayout();
    }
}

void ImageEditor::reset_scale_and_position()
{
    if (m_scale != 1.0f)
        m_scale = 1.0f;

    m_pan_origin = Gfx::FloatPoint(0, 0);
    relayout();
}

void ImageEditor::relayout()
{
    Gfx::IntSize new_size;
    new_size.set_width(image().size().width() * m_scale);
    new_size.set_height(image().size().height() * m_scale);
    m_editor_image_rect.set_size(new_size);

    Gfx::IntPoint new_location;
    new_location.set_x((width() / 2) - (new_size.width() / 2) - (m_pan_origin.x() * m_scale));
    new_location.set_y((height() / 2) - (new_size.height() / 2) - (m_pan_origin.y() * m_scale));
    m_editor_image_rect.set_location(new_location);

    update();
}

void ImageEditor::image_did_change(Gfx::IntRect const& modified_image_rect)
{
    update(m_editor_image_rect.intersected(enclosing_int_rect(image_rect_to_editor_rect(modified_image_rect))));
}

void ImageEditor::image_did_change_rect(Gfx::IntRect const& new_image_rect)
{
    m_editor_image_rect = enclosing_int_rect(image_rect_to_editor_rect(new_image_rect));
    update(m_editor_image_rect);
}

void ImageEditor::image_did_change_title(String const& path)
{
    if (on_image_title_change)
        on_image_title_change(path);
}

void ImageEditor::image_select_layer(Layer* layer)
{
    set_active_layer(layer);
}
}
