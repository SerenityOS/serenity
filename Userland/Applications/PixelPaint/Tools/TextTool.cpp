/*
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TextTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>

namespace PixelPaint {

TextToolEditor::TextToolEditor()
    : TextEditor(TextEditor::MultiLine)
{
}

void TextToolEditor::handle_keyevent(Badge<TextTool>, GUI::KeyEvent& event)
{
    TextEditor::keydown_event(event);
}

Vector<NonnullRefPtr<GUI::Action>> TextToolEditor::actions()
{
    static Vector<NonnullRefPtr<GUI::Action>> actions = { cut_action(), copy_action(), paste_action(), undo_action(), redo_action(), select_all_action() };
    return actions;
}

TextTool::TextTool()
{
    m_text_editor = TextToolEditor::construct();
    m_text_editor->set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
    m_selected_font = Gfx::FontDatabase::default_font();
    m_text_editor->set_font(m_selected_font);
    m_cursor_blink_timer = Core::Timer::create_repeating(500, [&]() {
        m_cursor_blink_state = !m_cursor_blink_state;
    });
}

void TextTool::on_primary_color_change(Color color)
{
    m_text_color = color;
}

void TextTool::on_tool_deactivation()
{
    reset_tool();
}
void TextTool::on_mousemove(Layer*, MouseEvent& event)
{
    if (m_text_input_is_active) {
        auto mouse_position = editor_stroke_position(event.layer_event().position(), 1);
        m_mouse_is_over_text = m_ants_rect.contains(mouse_position);
        m_editor->update_tool_cursor();
    }

    if (m_is_dragging) {
        auto new_position = event.layer_event().position();
        m_add_text_position = m_add_text_position + (new_position - m_drag_start_point);
        m_drag_start_point = new_position;
    }
}
void TextTool::on_mouseup(Layer*, MouseEvent&)
{
    m_is_dragging = false;
}
void TextTool::on_mousedown(Layer*, MouseEvent& event)
{
    auto start_text_region = [&] {
        m_text_color = m_editor->color_for(event.layer_event());
        m_text_input_is_active = true;
        m_text_editor->set_text(""sv);
        m_add_text_position = event.layer_event().position();
        m_editor->image().selection().begin_interactive_selection();
        m_cursor_blink_timer->start();
        m_editor->update();
    };

    if (!m_text_input_is_active) {
        start_text_region();
        return;
    }

    if (m_mouse_is_over_text) {
        m_is_dragging = true;
        m_drag_start_point = event.layer_event().position();
    } else {
        // User clicked somewhere outside the currently edited text region
        // apply the current text and then start a new one where they clicked.
        apply_text_to_layer();
        reset_tool();
        start_text_region();
    }
}

NonnullRefPtr<GUI::Widget> TextTool::get_properties_widget()
{
    if (m_properties_widget)
        return *m_properties_widget.ptr();

    auto properties_widget = GUI::Widget::construct();
    properties_widget->set_layout<GUI::VerticalBoxLayout>();

    auto& font_header = properties_widget->add<GUI::Label>("Current Font:"_string);
    font_header.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_font_label = properties_widget->add<GUI::Label>(m_selected_font->human_readable_name());

    auto& change_font_button = properties_widget->add<GUI::Button>("Change Font..."_string);
    change_font_button.on_click = [this](auto) {
        auto picker = GUI::FontPicker::construct(nullptr, m_selected_font, false);
        if (picker->exec() == GUI::Dialog::ExecResult::OK) {
            m_font_label->set_text(picker->font()->human_readable_name());
            m_selected_font = picker->font();
            m_text_editor->set_font(m_selected_font);
            m_editor->set_focus(true);
        }
    };

    m_properties_widget = properties_widget;
    return *m_properties_widget;
}

void TextTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (!m_text_input_is_active)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    painter.translate(editor_layer_location(*layer));
    auto typed_text = m_text_editor->text();
    auto text_width = max<int>(m_selected_font->width(typed_text), m_selected_font->width(" "sv));
    auto text_height = static_cast<int>(ceilf(m_selected_font->preferred_line_height() * max<int>(static_cast<int>(m_text_editor->line_count()), 1)));
    auto text_location = editor_stroke_position(m_add_text_position, 1);

    // Since ImageEditor can be zoomed in/out, we need to be able to render the preview properly scaled
    // GUI::Painter doesn't have a way to draw a font scaled directly, so we draw the text to a bitmap
    // and then scale the bitmap and blit the result to the ImageEditor.
    auto text_bitmap_result = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { text_width, text_height });
    if (text_bitmap_result.is_error())
        return;
    auto text_bitmap = text_bitmap_result.release_value();
    auto text_painter = GUI::Painter(text_bitmap);
    text_painter.set_font(*m_selected_font);
    text_painter.draw_text(Gfx::IntRect { 0, 0, text_width, text_height }, typed_text, Gfx::TextAlignment::TopLeft, m_text_color);

    m_text_editor->update();

    // Draw selected text (if any)
    if (m_text_editor->has_selection()) {
        auto selection = m_text_editor->selection();

        // Draw selected text for each line...
        auto selection_start_line = selection.normalized().start().line();
        auto selection_end_line = selection.normalized().end().line();

        for (auto i = selection_start_line; i <= selection_end_line; ++i) {

            auto start_col = 0;
            auto end_col = 0;

            // First line of selection.
            if (i == selection_start_line) {

                if (i < selection_end_line) {
                    // multiple lines selected. we select from selection start to the end of the line
                    start_col = m_text_editor->selection().normalized().start().column();
                    end_col = m_text_editor->line(i).length();
                } else {
                    // only a single line in the selection
                    start_col = m_text_editor->selection().normalized().start().column();
                    end_col = m_text_editor->selection().normalized().end().column();
                }
            } else if (i == selection_end_line) {
                // We are highlighting the final line of the selection.
                // Start from first char and continue to selection end.
                start_col = 0;
                end_col = m_text_editor->selection().normalized().end().column();
            } else {
                // We are between the start and end lines, highlight the whole thing.
                start_col = 0;
                end_col = m_text_editor->line(i).length();
            }
            auto line_selection_length = end_col - start_col;
            auto selected_string = m_text_editor->line(i).view().substring_view(start_col, line_selection_length);
            auto text_before_selection = m_text_editor->line(i).view().substring_view(0, start_col);
            auto selected_width = m_selected_font->width(selected_string);
            auto selection_x_offset = m_selected_font->width(text_before_selection);

            // the + 4 here is because that's how Painter::do_draw_text calculates line height, instead of asking
            // the font it's preferred line height. If we don't replicate that here, the letters jump around when they
            // get selected.
            auto selection_y_offset = static_cast<int>((m_selected_font->pixel_size() + 4) * i);

            auto selection_rect = Gfx::IntRect(selection_x_offset, selection_y_offset, selected_width, m_selected_font->preferred_line_height());
            text_painter.fill_rect(selection_rect, m_text_editor->palette().selection());
            text_painter.draw_text(selection_rect, selected_string, Gfx::TextAlignment::TopLeft, m_text_editor->palette().selection_text());
        }
    }

    auto scaled_width = static_cast<int>(ceilf(m_editor->scale() * static_cast<float>(text_bitmap->width())));
    auto scaled_height = static_cast<int>(ceilf(m_editor->scale() * static_cast<float>(text_bitmap->height())));
    auto scaled_rect = Gfx::IntRect(text_location.x(), text_location.y(), scaled_width, scaled_height);
    scaled_rect.set_location({ text_location.x(), text_location.y() });
    painter.draw_scaled_bitmap(scaled_rect, text_bitmap, text_bitmap->rect(), 1.0);

    // marching ants box
    auto right_padding = static_cast<int>(ceilf(m_selected_font->width("  "sv)));
    m_ants_rect = Gfx::IntRect(text_location.translated(-4, -2), { scaled_rect.width() + 4 + right_padding, scaled_rect.height() + 4 });
    m_editor->draw_marching_ants(painter, m_ants_rect);

    // Draw the blinking cursor.
    if (m_cursor_blink_state) {
        auto editor_cursor_rect = m_text_editor->cursor_content_rect();

        // TextEditor starts left most at 3, for TextTool this ends up putting the cursor in the middle of the letter.
        // Looks better if we treat 0 as left most here, so we just translate it to the left.
        editor_cursor_rect.translate_by(-3, 0);

        // ImageEditor scale is a float, but we are working with int and IntRects.
        auto scaled_cursor_x = static_cast<int>(ceilf(m_editor->scale() * static_cast<float>(editor_cursor_rect.x())));
        auto scaled_cursor_y = static_cast<int>(ceilf(m_editor->scale() * static_cast<float>(editor_cursor_rect.y())));
        auto scaled_cursor_width = static_cast<int>(ceilf(m_editor->scale() * static_cast<float>(editor_cursor_rect.width())));
        auto scaled_cursor_height = static_cast<int>(ceilf(m_editor->scale() * static_cast<float>(editor_cursor_rect.height())));

        auto scaled_cursor_rect = Gfx::IntRect { scaled_cursor_x + text_location.x(),
            scaled_cursor_y + text_location.y(),
            scaled_cursor_width,
            scaled_cursor_height };
        painter.fill_rect(scaled_cursor_rect, m_text_color);
    }
}

void TextTool::apply_text_to_layer()
{
    auto layer = m_editor->active_layer();
    GUI::Painter painter(layer->get_scratch_edited_bitmap());

    auto demo_text = m_text_editor->text();
    auto text_width = m_selected_font->width(demo_text);
    auto text_height = static_cast<int>(ceilf(m_selected_font->preferred_line_height() * static_cast<int>(m_text_editor->line_count())));

    painter.set_font(*m_selected_font);
    auto text_rect = Gfx::Rect<int>(m_add_text_position, { static_cast<int>(ceilf(text_width)), text_height });
    painter.draw_text(text_rect, demo_text, Gfx::TextAlignment::TopLeft, m_text_color);
    m_editor->did_complete_action(tool_name());
    layer->did_modify_bitmap(text_rect);
}
void TextTool::reset_tool()
{
    // This puts the tool back into initial state between text additions (except for selected font/color)
    m_text_input_is_active = false;
    m_is_dragging = false;
    m_mouse_is_over_text = false;
    m_text_editor->set_text(""sv);
    m_editor->image().selection().end_interactive_selection();
    m_cursor_blink_timer->stop();
    m_editor->update();
    m_editor->update_tool_cursor();
}
bool TextTool::on_keydown(GUI::KeyEvent& event)
{
    if (!m_text_input_is_active)
        return false;

    // Cancels current text entry
    if (event.key() == Key_Escape) {
        reset_tool();
        return true;
    }

    // A plain Return is treated as accepting the current state and rasterizing to the layer.
    // For multi-line text Shift + Enter will add new lines.
    if (event.modifiers() == Mod_None && event.key() == Key_Return) {
        apply_text_to_layer();
        reset_tool();
        return true;
    }

    // Pass key events that would normally be handled by menu shortcuts to our TextEditor subclass.
    for (auto& action : m_text_editor->actions()) {
        auto const& shortcut = action->shortcut();
        if (event.key() == shortcut.key() && event.modifiers() == shortcut.modifiers()) {
            action->activate(m_text_editor);
            return true;
        }
    }

    // Pass the key event off to our TextEditor subclass which handles all text entry features like
    // caret navigation, backspace/delete, etc.
    m_text_editor->handle_keyevent({}, event);
    m_editor->update();
    return true;
}

Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> TextTool::cursor()
{
    if (m_mouse_is_over_text)
        return Gfx::StandardCursor::Move;

    return Gfx::StandardCursor::Arrow;
}

}
