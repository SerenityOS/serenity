/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, David Isaksson <davidisaksson93@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageEditor.h"
#include "Image.h"
#include "Layer.h"
#include "Tools/MoveTool.h"
#include "Tools/Tool.h"
#include <LibConfig/Client.h>
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

    m_pixel_grid_threshold = (float)Config::read_i32("PixelPaint", "PixelGrid", "Threshold", 15);
    m_show_pixel_grid = Config::read_bool("PixelPaint", "PixelGrid", "Show", true);

    m_show_rulers = Config::read_bool("PixelPaint", "Rulers", "Show", true);
    m_show_guides = Config::read_bool("PixelPaint", "Guides", "Show", true);
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
    if (!m_undo_stack->can_undo())
        return false;

    /* Without this you need to undo twice to actually start undoing stuff.
     * This is due to the fact that the top of the UndoStack contains the snapshot of the currently
     * shown image but what we actually want to restore is the snapshot right below it.
     * Doing "undo->undo->redo" restores the 2nd topmost snapshot on the stack while lowering the
     * stack pointer only by 1. This is important because we want the UndoStack's pointer to always point
     * at the currently shown snapshot, otherwise doing 'undo->undo->draw something else' would delete
     * one of the snapshots.
     * This works because UndoStack::undo first decrements the stack pointer and then restores the snapshot,
     * while UndoStack::redo first restores the snapshot and then increments the stack pointer.
     */
    m_undo_stack->undo();
    m_undo_stack->undo();
    m_undo_stack->redo();
    layers_did_change();
    return true;
}

bool ImageEditor::redo()
{
    if (!m_undo_stack->can_redo())
        return false;

    m_undo_stack->redo();
    layers_did_change();
    return true;
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

    if (m_active_layer && m_show_active_layer_boundary) {
        painter.draw_rect(enclosing_int_rect(image_rect_to_editor_rect(m_active_layer->relative_rect())).inflated(2, 2), Color::Black);
    }

    if (m_show_pixel_grid && m_scale > m_pixel_grid_threshold) {
        auto event_image_rect = enclosing_int_rect(editor_rect_to_image_rect(event.rect())).inflated(1, 1);
        auto image_rect = m_image->rect().inflated(1, 1).intersected(event_image_rect);

        for (auto i = image_rect.left(); i < image_rect.right(); i++) {
            auto start_point = image_position_to_editor_position({ i, image_rect.top() }).to_type<int>();
            auto end_point = image_position_to_editor_position({ i, image_rect.bottom() }).to_type<int>();
            painter.draw_line(start_point, end_point, Color::LightGray);
        }

        for (auto i = image_rect.top(); i < image_rect.bottom(); i++) {
            auto start_point = image_position_to_editor_position({ image_rect.left(), i }).to_type<int>();
            auto end_point = image_position_to_editor_position({ image_rect.right(), i }).to_type<int>();
            painter.draw_line(start_point, end_point, Color::LightGray);
        }
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

    if (m_show_rulers) {
        const auto ruler_bg_color = palette().color(Gfx::ColorRole::InactiveSelection);
        const auto ruler_fg_color = palette().color(Gfx::ColorRole::Ruler);
        const auto ruler_text_color = palette().color(Gfx::ColorRole::InactiveSelectionText);
        const auto mouse_indicator_color = Color::White;

        // Ruler background
        painter.fill_rect({ { 0, 0 }, { m_ruler_thickness, rect().height() } }, ruler_bg_color);
        painter.fill_rect({ { 0, 0 }, { rect().width(), m_ruler_thickness } }, ruler_bg_color);

        const auto ruler_step = calculate_ruler_step_size();
        const auto editor_origin_to_image = editor_position_to_image_position({ 0, 0 });
        const auto editor_max_to_image = editor_position_to_image_position({ width(), height() });

        // Horizontal ruler
        painter.draw_line({ 0, m_ruler_thickness }, { rect().width(), m_ruler_thickness }, ruler_fg_color);
        const auto x_start = floor(editor_origin_to_image.x()) - ((int)floor(editor_origin_to_image.x()) % ruler_step) - ruler_step;
        for (int x = x_start; x < editor_max_to_image.x(); x += ruler_step) {
            const int num_sub_divisions = min(ruler_step, 10);
            for (int x_sub = 0; x_sub < num_sub_divisions; ++x_sub) {
                const int x_pos = x + (int)(ruler_step * x_sub / num_sub_divisions);
                const int editor_x_sub = image_position_to_editor_position({ x_pos, 0 }).x();
                const int line_length = (x_sub % 2 == 0) ? m_ruler_thickness / 3 : m_ruler_thickness / 6;
                painter.draw_line({ editor_x_sub, m_ruler_thickness - line_length }, { editor_x_sub, m_ruler_thickness }, ruler_fg_color);
            }

            const int editor_x = image_position_to_editor_position({ x, 0 }).x();
            painter.draw_line({ editor_x, 0 }, { editor_x, m_ruler_thickness }, ruler_fg_color);
            painter.draw_text({ { editor_x + 2, 0 }, { m_ruler_thickness, m_ruler_thickness - 2 } }, String::formatted("{}", x), painter.font(), Gfx::TextAlignment::CenterLeft, ruler_text_color);
        }

        // Vertical ruler
        painter.draw_line({ m_ruler_thickness, 0 }, { m_ruler_thickness, rect().height() }, ruler_fg_color);
        const auto y_start = floor(editor_origin_to_image.y()) - ((int)floor(editor_origin_to_image.y()) % ruler_step) - ruler_step;
        for (int y = y_start; y < editor_max_to_image.y(); y += ruler_step) {
            const int num_sub_divisions = min(ruler_step, 10);
            for (int y_sub = 0; y_sub < num_sub_divisions; ++y_sub) {
                const int y_pos = y + (int)(ruler_step * y_sub / num_sub_divisions);
                const int editor_y_sub = image_position_to_editor_position({ 0, y_pos }).y();
                const int line_length = (y_sub % 2 == 0) ? m_ruler_thickness / 3 : m_ruler_thickness / 6;
                painter.draw_line({ m_ruler_thickness - line_length, editor_y_sub }, { m_ruler_thickness, editor_y_sub }, ruler_fg_color);
            }

            const int editor_y = image_position_to_editor_position({ 0, y }).y();
            painter.draw_line({ 0, editor_y }, { m_ruler_thickness, editor_y }, ruler_fg_color);
            painter.draw_text({ { 0, editor_y - m_ruler_thickness }, { m_ruler_thickness, m_ruler_thickness } }, String::formatted("{}", y), painter.font(), Gfx::TextAlignment::BottomRight, ruler_text_color);
        }

        // Mouse position indicator
        const Gfx::IntPoint indicator_x({ m_mouse_position.x(), m_ruler_thickness });
        const Gfx::IntPoint indicator_y({ m_ruler_thickness, m_mouse_position.y() });
        painter.draw_triangle(indicator_x, indicator_x + Gfx::IntPoint(-m_mouse_indicator_triangle_size, -m_mouse_indicator_triangle_size), indicator_x + Gfx::IntPoint(m_mouse_indicator_triangle_size, -m_mouse_indicator_triangle_size), mouse_indicator_color);
        painter.draw_triangle(indicator_y, indicator_y + Gfx::IntPoint(-m_mouse_indicator_triangle_size, -m_mouse_indicator_triangle_size), indicator_y + Gfx::IntPoint(-m_mouse_indicator_triangle_size, m_mouse_indicator_triangle_size), mouse_indicator_color);

        // Top left square
        painter.fill_rect({ { 0, 0 }, { m_ruler_thickness, m_ruler_thickness } }, ruler_bg_color);
    }
}

int ImageEditor::calculate_ruler_step_size() const
{
    const auto step_target = 80 / m_scale;
    const auto max_factor = 5;
    for (int factor = 0; factor < max_factor; ++factor) {
        if (step_target <= 1 * (float)pow(10, factor))
            return 1 * pow(10, factor);
        if (step_target <= 2 * (float)pow(10, factor))
            return 2 * pow(10, factor);
        if (step_target <= 5 * (float)pow(10, factor))
            return 5 * pow(10, factor);
    }
    return 1 * pow(10, max_factor);
}

Gfx::IntRect ImageEditor::mouse_indicator_rect_x() const
{
    const Gfx::IntPoint top_left({ m_ruler_thickness, m_ruler_thickness - m_mouse_indicator_triangle_size });
    const Gfx::IntSize size({ width() + 1, m_mouse_indicator_triangle_size + 1 });
    return Gfx::IntRect(top_left, size);
}

Gfx::IntRect ImageEditor::mouse_indicator_rect_y() const
{
    const Gfx::IntPoint top_left({ m_ruler_thickness - m_mouse_indicator_triangle_size, m_ruler_thickness });
    const Gfx::IntSize size({ m_mouse_indicator_triangle_size + 1, height() + 1 });
    return Gfx::IntRect(top_left, size);
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
    m_mouse_position = event.position();
    if (m_show_rulers) {
        update(mouse_indicator_rect_x());
        update(mouse_indicator_rect_y());
    }

    if (event.buttons() & GUI::MouseButton::Middle) {
        auto delta = event.position() - m_click_position;
        m_pan_origin = m_saved_pan_origin.translated(
            -delta.x(),
            -delta.y());

        relayout();
        return;
    }

    auto image_event = event_with_pan_and_scale_applied(event);
    if (on_image_mouse_position_change) {
        on_image_mouse_position_change(image_event.position());
    }

    if (!m_active_tool)
        return;

    auto layer_event = m_active_layer ? event_adjusted_for_layer(event, *m_active_layer) : event;
    Tool::MouseEvent tool_event(Tool::MouseEvent::Action::MouseDown, layer_event, image_event, event);
    m_active_tool->on_mousemove(m_active_layer.ptr(), tool_event);
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

void ImageEditor::update_tool_cursor()
{
    if (m_active_tool) {
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

void ImageEditor::set_ruler_visibility(bool show_rulers)
{
    if (m_show_rulers == show_rulers)
        return;

    m_show_rulers = show_rulers;

    if (on_set_ruler_visibility)
        on_set_ruler_visibility(m_show_rulers);

    update();
}

void ImageEditor::set_pixel_grid_visibility(bool show_pixel_grid)
{
    if (m_show_pixel_grid == show_pixel_grid)
        return;
    m_show_pixel_grid = show_pixel_grid;
    update();
}

void ImageEditor::layers_did_change()
{
    update();
}

Color ImageEditor::color_for(GUI::MouseButton button) const
{
    if (button == GUI::MouseButton::Primary)
        return m_primary_color;
    if (button == GUI::MouseButton::Secondary)
        return m_secondary_color;
    VERIFY_NOT_REACHED();
}

Color ImageEditor::color_for(GUI::MouseEvent const& event) const
{
    if (event.buttons() & GUI::MouseButton::Primary)
        return m_primary_color;
    if (event.buttons() & GUI::MouseButton::Secondary)
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

void ImageEditor::set_absolute_scale(float scale, bool do_relayout)
{
    if (scale < 0.1f)
        scale = 0.1f;
    if (scale > 100.0f)
        scale = 100.0f;
    if (scale == m_scale)
        return;
    m_scale = scale;
    if (on_scale_changed)
        on_scale_changed(m_scale);
    if (do_relayout)
        relayout();
}

void ImageEditor::clamped_scale_by(float scale_delta, bool do_relayout)
{
    set_absolute_scale(m_scale * AK::exp2(scale_delta), do_relayout);
}

void ImageEditor::scale_centered_on_position(Gfx::IntPoint const& position, float scale_delta)
{
    auto old_scale = m_scale;
    auto image_coord_of_position = editor_position_to_image_position(position);

    auto image_size = m_image->size();
    Gfx::FloatPoint offset_from_center_in_image_coords = {
        image_coord_of_position.x() - image_size.width() / 2.0f,
        image_coord_of_position.y() - image_size.height() / 2.0f
    };
    Gfx::FloatPoint offset_from_center_in_editor_coords = {
        position.x() - width() / 2.0f,
        position.y() - height() / 2.0f
    };

    clamped_scale_by(scale_delta, false);

    m_pan_origin = {
        offset_from_center_in_image_coords.x() * m_scale - offset_from_center_in_editor_coords.x(),
        offset_from_center_in_image_coords.y() * m_scale - offset_from_center_in_editor_coords.y()
    };

    if (old_scale != m_scale)
        relayout();
}

void ImageEditor::scale_by(float scale_delta)
{
    if (scale_delta == 0)
        return;
    clamped_scale_by(scale_delta, true);
}

void ImageEditor::set_pan_origin(Gfx::FloatPoint const& pan_origin)
{
    if (m_pan_origin == pan_origin)
        return;

    m_pan_origin = pan_origin;
    relayout();
}

void ImageEditor::fit_image_to_view(FitType type)
{
    auto viewport_rect = rect();
    m_pan_origin = Gfx::FloatPoint(0, 0);

    if (m_show_rulers) {
        viewport_rect = {
            viewport_rect.x() + m_ruler_thickness,
            viewport_rect.y() + m_ruler_thickness,
            viewport_rect.width() - m_ruler_thickness,
            viewport_rect.height() - m_ruler_thickness
        };
    }

    const float border_ratio = 0.95f;
    auto image_size = image().size();
    auto height_ratio = floorf(border_ratio * viewport_rect.height()) / (float)image_size.height();
    auto width_ratio = floorf(border_ratio * viewport_rect.width()) / (float)image_size.width();
    switch (type) {
    case FitType::Width:
        set_absolute_scale(width_ratio, false);
        break;
    case FitType::Height:
        set_absolute_scale(height_ratio, false);
        break;
    case FitType::Image:
        set_absolute_scale(min(height_ratio, width_ratio), false);
        break;
    }

    float offset = m_show_rulers ? -m_ruler_thickness / (m_scale * 2.0f) : 0.0f;
    m_pan_origin = Gfx::FloatPoint(offset, offset);

    relayout();
}

void ImageEditor::reset_scale_and_position()
{
    set_absolute_scale(1.0f, false);

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
    new_location.set_x((width() / 2) - (new_size.width() / 2) - (m_pan_origin.x()));
    new_location.set_y((height() / 2) - (new_size.height() / 2) - (m_pan_origin.y()));
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

Result<void, String> ImageEditor::save_project_to_fd_and_close(int fd) const
{
    StringBuilder builder;
    JsonObjectSerializer json(builder);
    m_image->serialize_as_json(json);
    auto json_guides = json.add_array("guides");
    for (const auto& guide : m_guides) {
        auto json_guide = json_guides.add_object();
        json_guide.add("offset"sv, (double)guide.offset());
        if (guide.orientation() == Guide::Orientation::Vertical)
            json_guide.add("orientation", "vertical");
        else if (guide.orientation() == Guide::Orientation::Horizontal)
            json_guide.add("orientation", "horizontal");
        json_guide.finish();
    }
    json_guides.finish();
    json.finish();

    auto file = Core::File::construct();
    file->open(fd, Core::OpenMode::WriteOnly | Core::OpenMode::Truncate, Core::File::ShouldCloseFileDescriptor::Yes);
    if (file->has_error())
        return String { file->error_string() };

    if (!file->write(builder.string_view()))
        return String { file->error_string() };
    return {};
}

void ImageEditor::set_show_active_layer_boundary(bool show)
{
    if (m_show_active_layer_boundary == show)
        return;

    m_show_active_layer_boundary = show;
    update();
}

}
