/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, David Isaksson <davidisaksson93@gmail.com>
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageEditor.h"
#include "Image.h"
#include "Layer.h"
#include "Tools/MoveTool.h"
#include "Tools/Tool.h"
#include <AK/IntegralMath.h>
#include <AK/LexicalPath.h>
#include <LibConfig/Client.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Command.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

constexpr int marching_ant_length = 4;

ImageEditor::ImageEditor(NonnullRefPtr<Image> image)
    : m_image(move(image))
    , m_title("Untitled"_string)
    , m_gui_event_loop(Core::EventLoop::current())
{
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    m_undo_stack.push(make<ImageUndoCommand>(*m_image, ByteString()));
    m_image->add_client(*this);
    m_image->selection().add_client(*this);
    set_original_rect(m_image->rect());
    set_scale_bounds(0.1f, 100.0f);

    m_pixel_grid_threshold = (float)Config::read_i32("PixelPaint"sv, "PixelGrid"sv, "Threshold"sv, 15);
    m_show_pixel_grid = Config::read_bool("PixelPaint"sv, "PixelGrid"sv, "Show"sv, true);

    m_show_rulers = Config::read_bool("PixelPaint"sv, "Rulers"sv, "Show"sv, true);
    m_show_guides = Config::read_bool("PixelPaint"sv, "Guides"sv, "Show"sv, true);

    m_marching_ants_timer = Core::Timer::create_repeating(80, [this] {
        ++m_marching_ants_offset;
        m_marching_ants_offset %= (marching_ant_length * 2);
        if (!m_image->selection().is_empty() || m_image->selection().in_interactive_selection())
            update();
    });
    m_marching_ants_timer->start();
}

ImageEditor::~ImageEditor()
{
    m_image->selection().remove_client(*this);
    m_image->remove_client(*this);
}

void ImageEditor::did_complete_action(ByteString action_text)
{
    set_modified(move(action_text));
}

bool ImageEditor::is_modified()
{
    return undo_stack().is_current_modified();
}

bool ImageEditor::undo()
{
    if (!m_undo_stack.can_undo())
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
    m_undo_stack.undo();
    m_undo_stack.undo();
    m_undo_stack.redo();
    layers_did_change();
    return true;
}

bool ImageEditor::redo()
{
    if (!m_undo_stack.can_redo())
        return false;

    m_undo_stack.redo();
    layers_did_change();
    return true;
}

void ImageEditor::set_title(String title)
{
    m_title = move(title);
    if (on_title_change)
        on_title_change(m_title);
}

void ImageEditor::set_path(ByteString path)
{
    m_path = move(path);
    set_title(String::from_byte_string(LexicalPath::title(m_path)).release_value_but_fixme_should_propagate_errors());
}

void ImageEditor::set_modified(ByteString action_text)
{
    m_undo_stack.push(make<ImageUndoCommand>(*m_image, move(action_text)));
    update_modified();
}

void ImageEditor::set_unmodified()
{
    m_undo_stack.set_current_unmodified();
    update_modified();
}

void ImageEditor::update_modified()
{
    if (on_modified_change)
        on_modified_change(is_modified());
}

Gfx::IntRect ImageEditor::subtract_rulers_from_rect(Gfx::IntRect const& rect) const
{
    Gfx::IntRect clipped_rect {};
    clipped_rect.set_top(max(rect.y(), m_ruler_thickness + 1));
    clipped_rect.set_left(max(rect.x(), m_ruler_thickness + 1));
    clipped_rect.set_bottom(rect.bottom());
    clipped_rect.set_right(rect.right());
    return clipped_rect;
}

void ImageEditor::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    {
        Gfx::DisjointIntRectSet background_rects;
        background_rects.add(frame_inner_rect());
        background_rects.shatter(content_rect());
        for (auto& rect : background_rects.rects())
            painter.fill_rect(rect, palette().color(Gfx::ColorRole::Tray));
    }

    Gfx::StylePainter::paint_transparency_grid(painter, content_rect(), palette());

    painter.draw_rect(content_rect().inflated(2, 2), Color::Black);
    m_image->paint_into(painter, content_rect(), scale());

    if (m_active_layer && m_show_active_layer_boundary)
        painter.draw_rect(content_to_frame_rect(m_active_layer->relative_rect()).to_type<int>().inflated(2, 2), Color::Black);

    if (m_show_pixel_grid && scale() > m_pixel_grid_threshold) {
        auto event_image_rect = enclosing_int_rect(frame_to_content_rect(event.rect())).inflated(1, 1);
        auto image_rect = m_image->rect().inflated(1, 1).intersected(event_image_rect);

        for (auto i = image_rect.left(); i < image_rect.right() - 1; i++) {
            auto start_point = content_to_frame_position({ i, image_rect.top() }).to_type<int>();
            auto end_point = content_to_frame_position({ i, image_rect.bottom() - 1 }).to_type<int>();
            painter.draw_line(start_point, end_point, Color::LightGray);
        }

        for (auto i = image_rect.top(); i < image_rect.bottom() - 1; i++) {
            auto start_point = content_to_frame_position({ image_rect.left(), i }).to_type<int>();
            auto end_point = content_to_frame_position({ image_rect.right() - 1, i }).to_type<int>();
            painter.draw_line(start_point, end_point, Color::LightGray);
        }
    }

    if (m_show_guides) {
        for (auto& guide : m_guides) {
            if (guide->orientation() == Guide::Orientation::Horizontal) {
                int y_coordinate = (int)content_to_frame_position({ 0.0f, guide->offset() }).y();
                painter.draw_line({ 0, y_coordinate }, { rect().width(), y_coordinate }, Color::Cyan, 1, Gfx::LineStyle::Dashed, Color::LightGray);
            } else if (guide->orientation() == Guide::Orientation::Vertical) {
                int x_coordinate = (int)content_to_frame_position({ guide->offset(), 0.0f }).x();
                painter.draw_line({ x_coordinate, 0 }, { x_coordinate, rect().height() }, Color::Cyan, 1, Gfx::LineStyle::Dashed, Color::LightGray);
            }
        }
    }

    paint_selection(painter);

    if (m_show_rulers) {
        auto const ruler_bg_color = palette().color(Gfx::ColorRole::InactiveSelection);
        auto const ruler_fg_color = palette().color(Gfx::ColorRole::Ruler);
        auto const ruler_text_color = palette().color(Gfx::ColorRole::InactiveSelectionText);
        auto const mouse_indicator_color = Color::White;

        // Ruler background
        painter.fill_rect({ { 0, 0 }, { m_ruler_thickness, rect().height() } }, ruler_bg_color);
        painter.fill_rect({ { 0, 0 }, { rect().width(), m_ruler_thickness } }, ruler_bg_color);

        auto const ruler_step = calculate_ruler_step_size();
        auto const editor_origin_to_image = frame_to_content_position({ 0, 0 });
        auto const editor_max_to_image = frame_to_content_position({ width(), height() });

        // Horizontal ruler
        painter.draw_line({ 0, m_ruler_thickness }, { rect().width(), m_ruler_thickness }, ruler_fg_color);
        auto const x_start = floor(editor_origin_to_image.x()) - ((int)floor(editor_origin_to_image.x()) % ruler_step) - ruler_step;
        for (int x = x_start; x < editor_max_to_image.x(); x += ruler_step) {
            int const num_sub_divisions = min(ruler_step, 10);
            for (int x_sub = 0; x_sub < num_sub_divisions; ++x_sub) {
                int const x_pos = x + (int)(ruler_step * x_sub / num_sub_divisions);
                int const editor_x_sub = content_to_frame_position({ x_pos, 0 }).x();
                int const line_length = (x_sub % 2 == 0) ? m_ruler_thickness / 3 : m_ruler_thickness / 6;
                painter.draw_line({ editor_x_sub, m_ruler_thickness - line_length }, { editor_x_sub, m_ruler_thickness }, ruler_fg_color);
            }

            int const editor_x = content_to_frame_position({ x, 0 }).x();
            painter.draw_line({ editor_x, 0 }, { editor_x, m_ruler_thickness }, ruler_fg_color);
            painter.draw_text(Gfx::IntRect { { editor_x + 2, 0 }, { m_ruler_thickness, m_ruler_thickness - 2 } }, ByteString::formatted("{}", x), painter.font(), Gfx::TextAlignment::CenterLeft, ruler_text_color);
        }

        // Vertical ruler
        painter.draw_line({ m_ruler_thickness, 0 }, { m_ruler_thickness, rect().height() }, ruler_fg_color);
        auto const y_start = floor(editor_origin_to_image.y()) - ((int)floor(editor_origin_to_image.y()) % ruler_step) - ruler_step;
        for (int y = y_start; y < editor_max_to_image.y(); y += ruler_step) {
            int const num_sub_divisions = min(ruler_step, 10);
            for (int y_sub = 0; y_sub < num_sub_divisions; ++y_sub) {
                int const y_pos = y + (int)(ruler_step * y_sub / num_sub_divisions);
                int const editor_y_sub = content_to_frame_position({ 0, y_pos }).y();
                int const line_length = (y_sub % 2 == 0) ? m_ruler_thickness / 3 : m_ruler_thickness / 6;
                painter.draw_line({ m_ruler_thickness - line_length, editor_y_sub }, { m_ruler_thickness, editor_y_sub }, ruler_fg_color);
            }

            int const editor_y = content_to_frame_position({ 0, y }).y();
            painter.draw_line({ 0, editor_y }, { m_ruler_thickness, editor_y }, ruler_fg_color);
            painter.draw_text(Gfx::IntRect { { 0, editor_y - m_ruler_thickness }, { m_ruler_thickness, m_ruler_thickness } }, ByteString::formatted("{}", y), painter.font(), Gfx::TextAlignment::BottomRight, ruler_text_color);
        }

        // Mouse position indicator
        Gfx::IntPoint const indicator_x({ m_mouse_position.x(), m_ruler_thickness });
        Gfx::IntPoint const indicator_y({ m_ruler_thickness, m_mouse_position.y() });
        painter.draw_triangle(indicator_x, indicator_x + Gfx::IntPoint(-m_mouse_indicator_triangle_size, -m_mouse_indicator_triangle_size), indicator_x + Gfx::IntPoint(m_mouse_indicator_triangle_size, -m_mouse_indicator_triangle_size), mouse_indicator_color);
        painter.draw_triangle(indicator_y, indicator_y + Gfx::IntPoint(-m_mouse_indicator_triangle_size, -m_mouse_indicator_triangle_size), indicator_y + Gfx::IntPoint(-m_mouse_indicator_triangle_size, m_mouse_indicator_triangle_size), mouse_indicator_color);

        // Top left square
        painter.fill_rect({ { 0, 0 }, { m_ruler_thickness, m_ruler_thickness } }, ruler_bg_color);
    }
}

int ImageEditor::calculate_ruler_step_size() const
{
    auto const step_target = 80 / scale();
    auto const max_factor = 5;
    for (int factor = 0; factor < max_factor; ++factor) {
        int ten_to_factor = AK::pow<int>(10, factor);
        if (step_target <= 1 * ten_to_factor)
            return 1 * ten_to_factor;
        if (step_target <= 2 * ten_to_factor)
            return 2 * ten_to_factor;
        if (step_target <= 5 * ten_to_factor)
            return 5 * ten_to_factor;
    }
    return AK::pow<int>(10, max_factor);
}

Gfx::IntRect ImageEditor::mouse_indicator_rect_x() const
{
    Gfx::IntPoint const top_left({ m_ruler_thickness, m_ruler_thickness - m_mouse_indicator_triangle_size });
    Gfx::IntSize const size({ width() + 1, m_mouse_indicator_triangle_size + 1 });
    return Gfx::IntRect(top_left, size);
}

Gfx::IntRect ImageEditor::mouse_indicator_rect_y() const
{
    Gfx::IntPoint const top_left({ m_ruler_thickness - m_mouse_indicator_triangle_size, m_ruler_thickness });
    Gfx::IntSize const size({ m_mouse_indicator_triangle_size + 1, height() + 1 });
    return Gfx::IntRect(top_left, size);
}

void ImageEditor::second_paint_event(GUI::PaintEvent& event)
{
    if (m_active_layer && m_active_layer->mask_type() != Layer::MaskType::None)
        m_active_layer->on_second_paint(*this);

    if (m_active_tool) {
        if (m_show_rulers) {
            auto clipped_event = GUI::PaintEvent(subtract_rulers_from_rect(event.rect()), event.window_size());
            m_active_tool->on_second_paint(m_active_layer, clipped_event);
        } else {
            m_active_tool->on_second_paint(m_active_layer, event);
        }
    }
}

GUI::MouseEvent ImageEditor::event_with_pan_and_scale_applied(GUI::MouseEvent const& event) const
{
    auto image_position = frame_to_content_position(event.position());
    auto tool_adjusted_image_position = m_active_tool->point_position_to_preferred_cell(image_position);

    return {
        static_cast<GUI::Event::Type>(event.type()),
        tool_adjusted_image_position,
        event.buttons(),
        event.button(),
        event.modifiers(),
        event.wheel_delta_x(),
        event.wheel_delta_y(),
        event.wheel_raw_delta_x(),
        event.wheel_raw_delta_y(),
    };
}

GUI::MouseEvent ImageEditor::event_adjusted_for_layer(GUI::MouseEvent const& event, Layer const& layer) const
{
    auto image_position = frame_to_content_position(event.position());
    image_position.translate_by(-layer.location().x(), -layer.location().y());
    auto tool_adjusted_image_position = m_active_tool->point_position_to_preferred_cell(image_position);

    return {
        static_cast<GUI::Event::Type>(event.type()),
        tool_adjusted_image_position,
        event.buttons(),
        event.button(),
        event.modifiers(),
        event.wheel_delta_x(),
        event.wheel_delta_y(),
        event.wheel_raw_delta_x(),
        event.wheel_raw_delta_y(),
    };
}

Optional<Color> ImageEditor::color_from_position(Gfx::IntPoint position, bool sample_all_layers)
{
    Color color;
    auto* layer = active_layer();
    if (sample_all_layers) {
        color = image().color_at(position);
    } else {
        if (!layer || !layer->rect().contains(position))
            return {};
        color = layer->currently_edited_bitmap().get_pixel(position);
    }
    return color;
}

void ImageEditor::set_status_info_to_color_at_mouse_position(Gfx::IntPoint position, bool sample_all_layers)
{
    auto const color = color_from_position(position, sample_all_layers);
    if (!color.has_value())
        return;

    set_appended_status_info(ByteString::formatted("R:{}, G:{}, B:{}, A:{} [{}]", color->red(), color->green(), color->blue(), color->alpha(), color->to_byte_string()));
}

void ImageEditor::set_editor_color_to_color_at_mouse_position(GUI::MouseEvent const& event, bool sample_all_layers = false)
{
    auto const color = color_from_position(event.position(), sample_all_layers);

    if (!color.has_value())
        return;

    // We picked a transparent pixel, do nothing.
    if (!color->alpha())
        return;

    if (event.buttons() & GUI::MouseButton::Primary)
        set_primary_color(*color);
    if (event.buttons() & GUI::MouseButton::Secondary)
        set_secondary_color(*color);
}

void ImageEditor::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Middle) {
        start_panning(event.position());
        set_override_cursor(Gfx::StandardCursor::Drag);
        return;
    }

    if (!m_active_tool)
        return;

    if (auto* tool = dynamic_cast<MoveTool*>(m_active_tool); tool && tool->layer_selection_mode() == MoveTool::LayerSelectionMode::ForegroundLayer) {
        if (auto* foreground_layer = layer_at_editor_position(event.position()); foreground_layer && !tool->cursor_is_within_resize_anchor())
            set_active_layer(foreground_layer);
    }

    auto layer_event = m_active_layer ? event_adjusted_for_layer(event, *m_active_layer) : event;
    if (event.alt() && !m_active_tool->is_overriding_alt()) {
        set_editor_color_to_color_at_mouse_position(layer_event);
        return; // Pick Color instead of acivating active tool when holding alt.
    }

    auto image_event = event_with_pan_and_scale_applied(event);
    Tool::MouseEvent tool_event(Tool::MouseEvent::Action::MouseDown, layer_event, image_event, event);
    m_active_tool->on_mousedown(m_active_layer.ptr(), tool_event);
}

void ImageEditor::doubleclick_event(GUI::MouseEvent& event)
{
    if (!m_active_tool || (event.alt() && !m_active_tool->is_overriding_alt()))
        return;
    auto layer_event = m_active_layer ? event_adjusted_for_layer(event, *m_active_layer) : event;
    auto image_event = event_with_pan_and_scale_applied(event);
    Tool::MouseEvent tool_event(Tool::MouseEvent::Action::DoubleClick, layer_event, image_event, event);
    m_active_tool->on_doubleclick(m_active_layer.ptr(), tool_event);
}

void ImageEditor::mousemove_event(GUI::MouseEvent& event)
{
    m_mouse_position = event.position();
    if (m_show_rulers) {
        update(mouse_indicator_rect_x());
        update(mouse_indicator_rect_y());
    }

    if (is_panning()) {
        GUI::AbstractZoomPanWidget::mousemove_event(event);
        return;
    }

    if (active_tool() == nullptr)
        return;

    auto image_event = event_with_pan_and_scale_applied(event);
    if (on_image_mouse_position_change) {
        on_image_mouse_position_change(image_event.position());
    }

    auto layer_event = m_active_layer ? event_adjusted_for_layer(event, *m_active_layer) : event;
    if (m_active_tool && event.alt() && !m_active_tool->is_overriding_alt()) {
        set_override_cursor(Gfx::StandardCursor::Eyedropper);
        set_editor_color_to_color_at_mouse_position(layer_event);
        return;
    }

    Tool::MouseEvent tool_event(Tool::MouseEvent::Action::MouseDown, layer_event, image_event, event);
    m_active_tool->on_mousemove(m_active_layer.ptr(), tool_event);
}

void ImageEditor::mouseup_event(GUI::MouseEvent& event)
{
    if (!(m_active_tool && event.alt() && !m_active_tool->is_overriding_alt()))
        set_override_cursor(m_active_cursor);

    if (event.button() == GUI::MouseButton::Middle) {
        stop_panning();
        return;
    }

    if (!m_active_tool || (event.alt() && !m_active_tool->is_overriding_alt()))
        return;
    auto layer_event = m_active_layer ? event_adjusted_for_layer(event, *m_active_layer) : event;
    auto image_event = event_with_pan_and_scale_applied(event);
    Tool::MouseEvent tool_event(Tool::MouseEvent::Action::MouseDown, layer_event, image_event, event);
    m_active_tool->on_mouseup(m_active_layer.ptr(), tool_event);
}

void ImageEditor::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (!m_active_tool)
        return;
    m_active_tool->on_context_menu(m_active_layer, event);
}

void ImageEditor::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == Key_Delete && !m_image->selection().is_empty() && active_layer()) {
        active_layer()->erase_selection(m_image->selection());
        did_complete_action("Erase Selection"sv);
        return;
    }

    if (!m_active_tool)
        return;

    if (!m_active_tool->is_overriding_alt() && event.key() == Key_LeftAlt)
        set_override_cursor(Gfx::StandardCursor::Eyedropper);

    if (m_active_tool->on_keydown(event))
        return;

    if (event.key() == Key_Escape && !m_image->selection().is_empty()) {
        m_image->selection().clear();
        did_complete_action("Clear Selection"sv);
        return;
    }

    event.ignore();
}

void ImageEditor::keyup_event(GUI::KeyEvent& event)
{
    if (!m_active_tool)
        return;

    if (!m_active_tool->is_overriding_alt() && event.key() == Key_LeftAlt)
        update_tool_cursor();

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
    if (m_show_active_layer_boundary)
        update();
}

ErrorOr<void> ImageEditor::add_new_layer_from_selection()
{
    auto current_layer_selection = image().selection();
    if (current_layer_selection.is_empty())
        return Error::from_string_literal("There is no active selection to create layer from.");

    // save offsets of selection so we know where to place the new layer
    auto selection_offset = current_layer_selection.bounding_rect().location();

    auto selection_bitmap = active_layer()->copy_bitmap(current_layer_selection);
    if (selection_bitmap.is_null())
        return Error::from_string_literal("Unable to create bitmap from selection.");

    auto layer_or_error = PixelPaint::Layer::create_with_bitmap(image(), selection_bitmap.release_nonnull(), "New Layer"sv);
    if (layer_or_error.is_error())
        return Error::from_string_literal("Unable to create layer from selection.");

    auto new_layer = layer_or_error.release_value();
    new_layer->set_location(selection_offset);
    image().add_layer(new_layer);
    layers_did_change();
    return {};
}

void ImageEditor::set_active_tool(Tool* tool)
{
    if (m_active_tool == tool) {
        if (m_active_tool)
            m_active_tool->setup(*this);
        return;
    }

    if (m_active_tool) {
        m_active_tool->on_tool_deactivation();
        m_active_tool->clear();
    }

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

void ImageEditor::clear_guides()
{
    m_guides.clear();
    update();
}

void ImageEditor::layers_did_change()
{
    update_modified();
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

Layer* ImageEditor::layer_at_editor_position(Gfx::IntPoint editor_position)
{
    auto image_position = frame_to_content_position(editor_position);
    for (ssize_t i = m_image->layer_count() - 1; i >= 0; --i) {
        auto& layer = m_image->layer(i);
        if (!layer.is_visible())
            continue;
        if (layer.relative_rect().contains(Gfx::IntPoint(image_position.x(), image_position.y())))
            return const_cast<Layer*>(&layer);
    }
    return nullptr;
}

void ImageEditor::fit_image_to_view(FitType type)
{
    auto viewport_rect = rect();

    if (m_show_rulers) {
        viewport_rect = {
            viewport_rect.x() + m_ruler_thickness,
            viewport_rect.y() + m_ruler_thickness,
            viewport_rect.width() - m_ruler_thickness,
            viewport_rect.height() - m_ruler_thickness
        };
    }

    fit_content_to_rect(viewport_rect, type);
}

void ImageEditor::image_did_change(Gfx::IntRect const& modified_image_rect)
{
    update(content_rect().intersected(enclosing_int_rect(content_to_frame_rect(modified_image_rect))));
}

void ImageEditor::image_did_change_rect(Gfx::IntRect const& new_image_rect)
{
    set_original_rect(new_image_rect);
    set_content_rect(new_image_rect);
    relayout();
}

void ImageEditor::image_select_layer(Layer* layer)
{
    set_active_layer(layer);
}

bool ImageEditor::request_close()
{
    if (!undo_stack().is_current_modified())
        return true;

    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), path(), undo_stack().last_unmodified_timestamp());

    if (result == GUI::MessageBox::ExecResult::Yes) {
        save_project();
        return true;
    }

    if (result == GUI::MessageBox::ExecResult::No)
        return true;

    return false;
}

void ImageEditor::save_project()
{
    if (path().is_empty() || m_loaded_from_image) {
        save_project_as();
        return;
    }
    auto response = FileSystemAccessClient::Client::the().request_file(window(), path(), Core::File::OpenMode::Truncate | Core::File::OpenMode::Write);
    if (response.is_error())
        return;
    auto result = save_project_to_file(response.value().release_stream());
    if (result.is_error()) {
        GUI::MessageBox::show_error(window(), MUST(String::formatted("Could not save {}: {}", path(), result.release_error())));
        return;
    }
    set_unmodified();
    if (on_file_saved)
        on_file_saved(path());
}

void ImageEditor::save_project_as()
{
    auto response = FileSystemAccessClient::Client::the().save_file(window(), m_title.to_byte_string(), "pp");
    if (response.is_error())
        return;
    auto file = response.release_value();
    auto result = save_project_to_file(file.release_stream());
    if (result.is_error()) {
        GUI::MessageBox::show_error(window(), MUST(String::formatted("Could not save {}: {}", file.filename(), result.release_error())));
        return;
    }
    set_path(file.filename());
    set_loaded_from_image(false);
    set_unmodified();
    if (on_file_saved)
        on_file_saved(path());
}

ErrorOr<void> ImageEditor::save_project_to_file(NonnullOwnPtr<Core::File> file) const
{
    StringBuilder builder;
    auto json = TRY(JsonObjectSerializer<>::try_create(builder));
    TRY(m_image->serialize_as_json(json));
    auto json_guides = TRY(json.add_array("guides"sv));
    for (auto const& guide : m_guides) {
        auto json_guide = TRY(json_guides.add_object());
        TRY(json_guide.add("offset"sv, (double)guide->offset()));
        if (guide->orientation() == Guide::Orientation::Vertical)
            TRY(json_guide.add("orientation"sv, "vertical"));
        else if (guide->orientation() == Guide::Orientation::Horizontal)
            TRY(json_guide.add("orientation"sv, "horizontal"));
        TRY(json_guide.finish());
    }
    TRY(json_guides.finish());
    TRY(json.finish());

    TRY(file->write_until_depleted(builder.string_view().bytes()));
    return {};
}

void ImageEditor::set_show_active_layer_boundary(bool show)
{
    if (m_show_active_layer_boundary == show)
        return;

    m_show_active_layer_boundary = show;
    update();
}

void ImageEditor::set_loaded_from_image(bool loaded_from_image)
{
    m_loaded_from_image = loaded_from_image;
}

void ImageEditor::paint_selection(Gfx::Painter& painter)
{
    if (m_image->selection().is_empty())
        return;

    draw_marching_ants(painter, m_image->selection().mask());
}

void ImageEditor::draw_marching_ants(Gfx::Painter& painter, Gfx::IntRect const& rect) const
{
    // Top line
    for (int x = rect.left(); x < rect.right(); ++x)
        draw_marching_ants_pixel(painter, x, rect.top());

    // Right line
    for (int y = rect.top() + 1; y < rect.bottom(); ++y)
        draw_marching_ants_pixel(painter, rect.right() - 1, y);

    // Bottom line
    for (int x = rect.right() - 2; x >= rect.left(); --x)
        draw_marching_ants_pixel(painter, x, rect.bottom() - 1);

    // Left line
    for (int y = rect.bottom() - 2; y > rect.top(); --y)
        draw_marching_ants_pixel(painter, rect.left(), y);
}

void ImageEditor::draw_marching_ants(Gfx::Painter& painter, Mask const& mask) const
{
    // If the zoom is < 100%, we can skip pixels to save a lot of time drawing the ants
    int step = max(1, (int)floorf(1.0f / scale()));

    // Only check the visible selection area when drawing for performance
    auto rect = this->rect();
    rect = Gfx::enclosing_int_rect(frame_to_content_rect(rect));
    rect.inflate(step * 2, step * 2); // prevent borders from having visible ants if the selection extends beyond it

    // Scan the image horizontally to find vertical borders
    for (int y = rect.top(); y < rect.bottom(); y += step) {
        bool previous_selected = false;
        for (int x = rect.left(); x < rect.right(); x += step) {
            bool this_selected = mask.get(x, y) > 0;

            if (this_selected != previous_selected) {
                Gfx::IntRect image_pixel { x, y, 1, 1 };
                auto pixel = content_to_frame_rect(image_pixel).to_type<int>();
                auto end = max(pixel.top() + 1, pixel.bottom()); // for when the zoom is < 100%

                for (int pixel_y = pixel.top(); pixel_y < end; pixel_y++) {
                    draw_marching_ants_pixel(painter, pixel.left(), pixel_y);
                }
            }

            previous_selected = this_selected;
        }
    }

    // Scan the image vertically to find horizontal borders
    for (int x = rect.left(); x < rect.right(); x += step) {

        bool previous_selected = false;
        for (int y = rect.top(); y < rect.bottom(); y += step) {
            bool this_selected = mask.get(x, y) > 0;

            if (this_selected != previous_selected) {
                Gfx::IntRect image_pixel { x, y, 1, 1 };
                auto pixel = content_to_frame_rect(image_pixel).to_type<int>();
                auto end = max(pixel.left() + 1, pixel.right()); // for when the zoom is < 100%

                for (int pixel_x = pixel.left(); pixel_x < end; pixel_x++)
                    draw_marching_ants_pixel(painter, pixel_x, pixel.top());
            }

            previous_selected = this_selected;
        }
    }
}

void ImageEditor::draw_marching_ants_pixel(Gfx::Painter& painter, int x, int y) const
{
    int pattern_index = x + y + m_marching_ants_offset;

    if (pattern_index % (marching_ant_length * 2) < marching_ant_length) {
        painter.set_pixel(x, y, Color::Black);
    } else {
        painter.set_pixel(x, y, Color::White);
    }
}

void ImageEditor::selection_did_change()
{
    update();
}

void ImageEditor::set_appended_status_info(ByteString new_status_info)
{
    m_appended_status_info = new_status_info;
    if (on_appended_status_info_change)
        on_appended_status_info_change(m_appended_status_info);
}

ByteString ImageEditor::generate_unique_layer_name(ByteString const& original_layer_name)
{
    constexpr StringView copy_string_view = " copy"sv;
    auto copy_suffix_index = original_layer_name.find_last(copy_string_view);
    if (!copy_suffix_index.has_value())
        return ByteString::formatted("{}{}", original_layer_name, copy_string_view);

    auto after_copy_suffix_view = original_layer_name.substring_view(copy_suffix_index.value() + copy_string_view.length());
    if (!after_copy_suffix_view.is_empty()) {
        auto after_copy_suffix_number = after_copy_suffix_view.trim_whitespace().to_number<int>();
        if (!after_copy_suffix_number.has_value())
            return ByteString::formatted("{}{}", original_layer_name, copy_string_view);
    }

    auto layer_with_name_exists = [this](auto name) {
        for (size_t i = 0; i < image().layer_count(); ++i) {
            if (image().layer(i).name() == name)
                return true;
        }
        return false;
    };

    auto base_layer_name = original_layer_name.substring_view(0, copy_suffix_index.value());
    StringBuilder new_layer_name;
    auto duplicate_name_count = 0;
    do {
        new_layer_name.clear();
        new_layer_name.appendff("{}{} {}", base_layer_name, copy_string_view, ++duplicate_name_count);
    } while (layer_with_name_exists(new_layer_name.string_view()));

    return new_layer_name.to_byte_string();
}

Gfx::IntRect ImageEditor::active_layer_visible_rect()
{
    if (!active_layer())
        return {};

    auto scaled_layer_rect = active_layer()->relative_rect().to_type<float>().scaled(scale(), scale()).to_type<int>().translated(content_rect().location());
    auto visible_editor_rect = ruler_visibility() ? subtract_rulers_from_rect(rect()) : rect();
    scaled_layer_rect.intersect(visible_editor_rect);
    return scaled_layer_rect;
}

}
