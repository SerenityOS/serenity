/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <Kernel/KeyCode.h>
#include <LibGUI/Action.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FontDatabase.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/SyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

//#define DEBUG_GTEXTEDITOR

namespace GUI {

TextEditor::TextEditor(Type type)
    : m_type(type)
{
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    set_document(TextDocument::create());
    set_scrollbars_enabled(is_multi_line());
    set_font(GFontDatabase::the().get_by_name("Csilla Thin"));
    // FIXME: Recompute vertical scrollbar step size on font change.
    vertical_scrollbar().set_step(line_height());
    m_cursor = { 0, 0 };
    create_actions();
}

TextEditor::~TextEditor()
{
    if (m_document)
        m_document->unregister_client(*this);
}

void TextEditor::create_actions()
{
    m_undo_action = CommonActions::make_undo_action([&](auto&) { undo(); }, this);
    m_redo_action = CommonActions::make_redo_action([&](auto&) { redo(); }, this);
    m_undo_action->set_enabled(false);
    m_redo_action->set_enabled(false);
    m_cut_action = CommonActions::make_cut_action([&](auto&) { cut(); }, this);
    m_copy_action = CommonActions::make_copy_action([&](auto&) { copy(); }, this);
    m_paste_action = CommonActions::make_paste_action([&](auto&) { paste(); }, this);
    m_delete_action = CommonActions::make_delete_action([&](auto&) { do_delete(); }, this);
    if (is_multi_line()) {
        m_go_to_line_action = Action::create(
            "Go to line...", { Mod_Ctrl, Key_L }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"), [this](auto&) {
                auto input_box = InputBox::construct("Line:", "Go to line", window());
                auto result = input_box->exec();
                if (result == InputBox::ExecOK) {
                    bool ok;
                    auto line_number = input_box->text_value().to_uint(ok);
                    if (ok)
                        set_cursor(line_number - 1, 0);
                }
            },
            this);
    }
}

void TextEditor::set_text(const StringView& text)
{
    if (is_single_line() && text.length() == line(0).length() && !memcmp(text.characters_without_null_termination(), line(0).characters(), text.length()))
        return;

    m_selection.clear();

    document().set_text(text);

    update_content_size();
    recompute_all_visual_lines();
    if (is_single_line())
        set_cursor(0, line(0).length());
    else
        set_cursor(0, 0);
    did_update_selection();
    update();
}

void TextEditor::update_content_size()
{
    int content_width = 0;
    int content_height = 0;
    for (auto& line : m_line_visual_data) {
        content_width = max(line.visual_rect.width(), content_width);
        content_height += line.visual_rect.height();
    }
    content_width += m_horizontal_content_padding * 2;
    if (is_right_text_alignment(m_text_alignment))
        content_width = max(frame_inner_rect().width(), content_width);

    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ ruler_width(), 0 });
}

TextPosition TextEditor::text_position_at(const Gfx::Point& a_position) const
{
    auto position = a_position;
    position.move_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    position.move_by(-(m_horizontal_content_padding + ruler_width()), 0);
    position.move_by(-frame_thickness(), -frame_thickness());

    size_t line_index = 0;

    if (is_line_wrapping_enabled()) {
        for (size_t i = 0; i < line_count(); ++i) {
            auto& rect = m_line_visual_data[i].visual_rect;
            if (position.y() >= rect.top() && position.y() <= rect.bottom()) {
                line_index = i;
                break;
            }
            if (position.y() > rect.bottom())
                line_index = line_count() - 1;
        }
    } else {
        line_index = (size_t)(position.y() / line_height());
    }

    line_index = max((size_t)0, min(line_index, line_count() - 1));

    size_t column_index;
    switch (m_text_alignment) {
    case Gfx::TextAlignment::CenterLeft:
        column_index = (position.x() + glyph_width() / 2) / glyph_width();
        if (is_line_wrapping_enabled()) {
            for_each_visual_line(line_index, [&](const Gfx::Rect& rect, const StringView&, size_t start_of_line) {
                if (rect.contains_vertically(position.y())) {
                    column_index += start_of_line;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
        }
        break;
    case Gfx::TextAlignment::CenterRight:
        // FIXME: Support right-aligned line wrapping, I guess.
        ASSERT(!is_line_wrapping_enabled());
        column_index = (position.x() - content_x_for_position({ line_index, 0 }) + glyph_width() / 2) / glyph_width();
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    column_index = max((size_t)0, min(column_index, line(line_index).length()));
    return { line_index, column_index };
}

void TextEditor::doubleclick_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Left)
        return;

    // NOTE: This ensures that spans are updated before we look at them.
    flush_pending_change_notification_if_needed();

    m_triple_click_timer.start();
    m_in_drag_select = false;

    auto start = text_position_at(event.position());
    auto end = start;
    auto& line = this->line(start.line());

    if (!document().has_spans()) {
        while (start.column() > 0) {
            if (isspace(line.characters()[start.column() - 1]))
                break;
            start.set_column(start.column() - 1);
        }

        while (end.column() < line.length()) {
            if (isspace(line.characters()[end.column()]))
                break;
            end.set_column(end.column() + 1);
        }
    } else {
        for (auto& span : document().spans()) {
            if (!span.range.contains(start))
                continue;
            start = span.range.start();
            end = span.range.end();
            end.set_column(end.column() + 1);
            break;
        }
    }

    m_selection.set(start, end);
    set_cursor(end);
    update();
    did_update_selection();
}

void TextEditor::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Left) {
        return;
    }

    if (m_triple_click_timer.is_valid() && m_triple_click_timer.elapsed() < 250) {
        m_triple_click_timer = Core::ElapsedTimer();

        TextPosition start;
        TextPosition end;

        if (is_multi_line()) {
            // select *current* line
            start = TextPosition(m_cursor.line(), 0);
            end = TextPosition(m_cursor.line(), line(m_cursor.line()).length());
        } else {
            // select *whole* line
            start = TextPosition(0, 0);
            end = TextPosition(line_count() - 1, line(line_count() - 1).length());
        }

        m_selection.set(start, end);
        set_cursor(end);
        return;
    }

    if (event.modifiers() & Mod_Shift) {
        if (!has_selection())
            m_selection.set(m_cursor, {});
    } else {
        m_selection.clear();
    }

    m_in_drag_select = true;

    set_cursor(text_position_at(event.position()));

    if (!(event.modifiers() & Mod_Shift)) {
        if (!has_selection())
            m_selection.set(m_cursor, {});
    }

    if (m_selection.start().is_valid() && m_selection.start() != m_cursor)
        m_selection.set_end(m_cursor);

    // FIXME: Only update the relevant rects.
    update();
    did_update_selection();
}

void TextEditor::mouseup_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Left) {
        if (m_in_drag_select) {
            m_in_drag_select = false;
        }
        return;
    }
}

void TextEditor::mousemove_event(MouseEvent& event)
{
    if (m_in_drag_select) {
        set_cursor(text_position_at(event.position()));
        m_selection.set_end(m_cursor);
        did_update_selection();
        update();
        return;
    }
}

int TextEditor::ruler_width() const
{
    if (!m_ruler_visible)
        return 0;
    // FIXME: Resize based on needed space.
    return 5 * font().glyph_width('x') + 4;
}

Gfx::Rect TextEditor::ruler_content_rect(size_t line_index) const
{
    if (!m_ruler_visible)
        return {};
    return {
        0 - ruler_width() + horizontal_scrollbar().value(),
        line_content_rect(line_index).y(),
        ruler_width(),
        line_content_rect(line_index).height()
    };
}

Gfx::Rect TextEditor::ruler_rect_in_inner_coordinates() const
{
    return { 0, 0, ruler_width(), height() - height_occupied_by_horizontal_scrollbar() };
}

Gfx::Rect TextEditor::visible_text_rect_in_inner_coordinates() const
{
    return {
        m_horizontal_content_padding + (m_ruler_visible ? (ruler_rect_in_inner_coordinates().right() + 1) : 0),
        0,
        frame_inner_rect().width() - (m_horizontal_content_padding * 2) - width_occupied_by_vertical_scrollbar() - ruler_width(),
        frame_inner_rect().height() - height_occupied_by_horizontal_scrollbar()
    };
}

void TextEditor::paint_event(PaintEvent& event)
{
    Color widget_background_color = palette().color(background_role());
    // NOTE: This ensures that spans are updated before we look at them.
    flush_pending_change_notification_if_needed();

    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), widget_background_color);

    painter.translate(frame_thickness(), frame_thickness());

    auto ruler_rect = ruler_rect_in_inner_coordinates();

    if (m_ruler_visible) {
        painter.fill_rect(ruler_rect, palette().ruler());
        painter.draw_line(ruler_rect.top_right(), ruler_rect.bottom_right(), palette().ruler_border());
    }

    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    if (m_ruler_visible)
        painter.translate(ruler_width(), 0);

    size_t first_visible_line = text_position_at(event.rect().top_left()).line();
    size_t last_visible_line = text_position_at(event.rect().bottom_right()).line();

    auto selection = normalized_selection();
    bool has_selection = selection.is_valid();

    if (m_ruler_visible) {
        for (size_t i = first_visible_line; i <= last_visible_line; ++i) {
            bool is_current_line = i == m_cursor.line();
            auto ruler_line_rect = ruler_content_rect(i);
            painter.draw_text(
                ruler_line_rect.shrunken(2, 0).translated(0, m_line_spacing / 2),
                String::number(i + 1),
                is_current_line ? Gfx::Font::default_bold_font() : font(),
                Gfx::TextAlignment::TopRight,
                is_current_line ? palette().ruler_active_text() : palette().ruler_inactive_text());
        }
    }

    Gfx::Rect text_clip_rect {
        (m_ruler_visible ? (ruler_rect_in_inner_coordinates().right() + frame_thickness() + 1) : frame_thickness()),
        frame_thickness(),
        width() - width_occupied_by_vertical_scrollbar() - ruler_width(),
        height() - height_occupied_by_horizontal_scrollbar()
    };
    painter.add_clip_rect(text_clip_rect);

    for (size_t line_index = first_visible_line; line_index <= last_visible_line; ++line_index) {
        auto& line = this->line(line_index);

        bool physical_line_has_selection = has_selection && line_index >= selection.start().line() && line_index <= selection.end().line();
        size_t first_visual_line_with_selection = 0;
        size_t last_visual_line_with_selection = 0;
        if (physical_line_has_selection) {
            if (selection.start().line() < line_index)
                first_visual_line_with_selection = 0;
            else
                first_visual_line_with_selection = visual_line_containing(line_index, selection.start().column());

            if (selection.end().line() > line_index)
                last_visual_line_with_selection = m_line_visual_data[line_index].visual_line_breaks.size();
            else
                last_visual_line_with_selection = visual_line_containing(line_index, selection.end().column());
        }

        size_t selection_start_column_within_line = selection.start().line() == line_index ? selection.start().column() : 0;
        size_t selection_end_column_within_line = selection.end().line() == line_index ? selection.end().column() : line.length();

        size_t visual_line_index = 0;
        for_each_visual_line(line_index, [&](const Gfx::Rect& visual_line_rect, const StringView& visual_line_text, size_t start_of_visual_line) {
            if (is_multi_line() && line_index == m_cursor.line())
                painter.fill_rect(visual_line_rect, widget_background_color.darkened(0.9f));
#ifdef DEBUG_GTEXTEDITOR
            painter.draw_rect(visual_line_rect, Color::Cyan);
#endif
            if (!document().has_spans()) {
                // Fast-path for plain text
                painter.draw_text(visual_line_rect, visual_line_text, m_text_alignment, palette().color(foreground_role()));
            } else {
                int advance = font().glyph_width(' ') + font().glyph_spacing();
                Gfx::Rect character_rect = { visual_line_rect.location(), { font().glyph_width(' '), line_height() } };
                for (size_t i = 0; i < visual_line_text.length(); ++i) {
                    const Gfx::Font* font = &this->font();
                    Color color;
                    Optional<Color> background_color;
                    TextPosition physical_position(line_index, start_of_visual_line + i);
                    // FIXME: This is *horribly* inefficient.
                    for (auto& span : document().spans()) {
                        if (!span.range.contains(physical_position))
                            continue;
                        color = span.color;
                        if (span.font)
                            font = span.font;
                        background_color = span.background_color;
                        break;
                    }
                    if (background_color.has_value())
                        painter.fill_rect(character_rect, background_color.value());
                    painter.draw_text(character_rect, visual_line_text.substring_view(i, 1), *font, m_text_alignment, color);
                    character_rect.move_by(advance, 0);
                }
            }
            bool physical_line_has_selection = has_selection && line_index >= selection.start().line() && line_index <= selection.end().line();
            if (physical_line_has_selection) {

                bool current_visual_line_has_selection = (line_index != selection.start().line() && line_index != selection.end().line())
                    || (visual_line_index >= first_visual_line_with_selection && visual_line_index <= last_visual_line_with_selection);
                if (current_visual_line_has_selection) {
                    bool selection_begins_on_current_visual_line = visual_line_index == first_visual_line_with_selection;
                    bool selection_ends_on_current_visual_line = visual_line_index == last_visual_line_with_selection;

                    int selection_left = selection_begins_on_current_visual_line
                        ? content_x_for_position({ line_index, (size_t)selection_start_column_within_line })
                        : m_horizontal_content_padding;

                    int selection_right = selection_ends_on_current_visual_line
                        ? content_x_for_position({ line_index, (size_t)selection_end_column_within_line })
                        : visual_line_rect.right() + 1;

                    Gfx::Rect selection_rect {
                        selection_left,
                        visual_line_rect.y(),
                        selection_right - selection_left,
                        visual_line_rect.height()
                    };

                    Color background_color = is_focused() ? palette().selection() : palette().inactive_selection();
                    Color text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();

                    painter.fill_rect(selection_rect, background_color);

                    size_t start_of_selection_within_visual_line = (size_t)max(0, (int)selection_start_column_within_line - (int)start_of_visual_line);
                    size_t end_of_selection_within_visual_line = selection_end_column_within_line - start_of_visual_line;

                    StringView visual_selected_text {
                        visual_line_text.characters_without_null_termination() + start_of_selection_within_visual_line,
                        end_of_selection_within_visual_line - start_of_selection_within_visual_line
                    };

                    painter.draw_text(selection_rect, visual_selected_text, Gfx::TextAlignment::CenterLeft, text_color);
                }
            }
            ++visual_line_index;
            return IterationDecision::Continue;
        });
    }

    if (is_focused() && m_cursor_state)
        painter.fill_rect(cursor_content_rect(), palette().text_cursor());
}

void TextEditor::toggle_selection_if_needed_for_event(const KeyEvent& event)
{
    if (event.shift() && !m_selection.is_valid()) {
        m_selection.set(m_cursor, {});
        did_update_selection();
        update();
        return;
    }
    if (!event.shift() && m_selection.is_valid()) {
        m_selection.clear();
        did_update_selection();
        update();
        return;
    }
}

void TextEditor::select_all()
{
    TextPosition start_of_document { 0, 0 };
    TextPosition end_of_document { line_count() - 1, line(line_count() - 1).length() };
    m_selection.set(start_of_document, end_of_document);
    did_update_selection();
    set_cursor(end_of_document);
    update();
}

void TextEditor::get_selection_line_boundaries(size_t& first_line, size_t& last_line)
{
    auto selection = normalized_selection();
    if (!selection.is_valid()) {
        first_line = m_cursor.line();
        last_line = m_cursor.line();
        return;
    }
    first_line = selection.start().line();
    last_line = selection.end().line();
    if (first_line != last_line && selection.end().column() == 0)
        last_line -= 1;
}

void TextEditor::move_selected_lines_up()
{
    size_t first_line;
    size_t last_line;
    get_selection_line_boundaries(first_line, last_line);

    if (first_line == 0)
        return;

    auto& lines = document().lines();
    lines.insert((int)last_line, lines.take((int)first_line - 1));
    m_cursor = { first_line - 1, 0 };

    if (has_selection()) {
        m_selection.set_start({ first_line - 1, 0 });
        m_selection.set_end({ last_line - 1, line(last_line - 1).length() });
    }

    did_change();
    update();
}

void TextEditor::move_selected_lines_down()
{
    size_t first_line;
    size_t last_line;
    get_selection_line_boundaries(first_line, last_line);

    auto& lines = document().lines();
    if (last_line >= (size_t)(lines.size() - 1))
        return;

    lines.insert((int)first_line, lines.take((int)last_line + 1));
    m_cursor = { first_line + 1, 0 };

    if (has_selection()) {
        m_selection.set_start({ first_line + 1, 0 });
        m_selection.set_end({ last_line + 1, line(last_line + 1).length() });
    }

    did_change();
    update();
}

void TextEditor::sort_selected_lines()
{
    if (is_readonly())
        return;

    if (!has_selection())
        return;

    size_t first_line;
    size_t last_line;
    get_selection_line_boundaries(first_line, last_line);

    auto& lines = document().lines();

    auto start = lines.begin() + (int)first_line;
    auto end = lines.begin() + (int)last_line + 1;

    quick_sort(start, end, [](auto& a, auto& b) {
        return strcmp(a.characters(), b.characters()) < 0;
    });

    did_change();
    update();
}

void TextEditor::keydown_event(KeyEvent& event)
{
    if (is_single_line() && event.key() == KeyCode::Key_Tab)
        return Widget::keydown_event(event);

    if (is_single_line() && event.key() == KeyCode::Key_Return) {
        if (on_return_pressed)
            on_return_pressed();
        return;
    }

    if (event.key() == KeyCode::Key_Escape) {
        if (on_escape_pressed)
            on_escape_pressed();
        return;
    }
    if (is_multi_line() && event.key() == KeyCode::Key_Up) {
        if (m_cursor.line() > 0) {
            if (event.ctrl() && event.shift()) {
                move_selected_lines_up();
                return;
            }
            size_t new_line = m_cursor.line() - 1;
            size_t new_column = min(m_cursor.column(), line(new_line).length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (is_multi_line() && event.key() == KeyCode::Key_Down) {
        if (m_cursor.line() < (line_count() - 1)) {
            if (event.ctrl() && event.shift()) {
                move_selected_lines_down();
                return;
            }
            size_t new_line = m_cursor.line() + 1;
            size_t new_column = min(m_cursor.column(), line(new_line).length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (is_multi_line() && event.key() == KeyCode::Key_PageUp) {
        if (m_cursor.line() > 0) {
            size_t page_step = (size_t)visible_content_rect().height() / (size_t)line_height();
            size_t new_line = m_cursor.line() < page_step ? 0 : m_cursor.line() - page_step;
            size_t new_column = min(m_cursor.column(), line(new_line).length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (is_multi_line() && event.key() == KeyCode::Key_PageDown) {
        if (m_cursor.line() < (line_count() - 1)) {
            int new_line = min(line_count() - 1, m_cursor.line() + visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), lines()[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_Left) {
        if (event.ctrl() && document().has_spans()) {
            // FIXME: Do something nice when the document has no spans.
            auto span = document().first_non_skippable_span_before(m_cursor);
            TextPosition new_cursor = !span.has_value()
                ? TextPosition(0, 0)
                : span.value().range.start();
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_cursor);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
            return;
        }
        if (m_cursor.column() > 0) {
            int new_column = m_cursor.column() - 1;
            toggle_selection_if_needed_for_event(event);
            set_cursor(m_cursor.line(), new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        } else if (m_cursor.line() > 0) {
            int new_line = m_cursor.line() - 1;
            int new_column = lines()[new_line].length();
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
        if (event.ctrl() && document().has_spans()) {
            // FIXME: Do something nice when the document has no spans.
            auto span = document().first_non_skippable_span_after(m_cursor);
            TextPosition new_cursor = !span.has_value()
                ? document().spans().last().range.end()
                : span.value().range.start();
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_cursor);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
            return;
        }
        int new_line = m_cursor.line();
        int new_column = m_cursor.column();
        if (m_cursor.column() < current_line().length()) {
            new_line = m_cursor.line();
            new_column = m_cursor.column() + 1;
        } else if (m_cursor.line() != line_count() - 1) {
            new_line = m_cursor.line() + 1;
            new_column = 0;
        }
        toggle_selection_if_needed_for_event(event);
        set_cursor(new_line, new_column);
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_Home) {
        size_t first_nonspace_column = current_line().first_non_whitespace_column();
        toggle_selection_if_needed_for_event(event);
        if (m_cursor.column() == first_nonspace_column)
            set_cursor(m_cursor.line(), 0);
        else
            set_cursor(m_cursor.line(), first_nonspace_column);
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_End) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(m_cursor.line(), current_line().length());
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_Home) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(0, 0);
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(line_count() - 1, lines()[line_count() - 1].length());
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.modifiers() == Mod_Ctrl && event.key() == KeyCode::Key_A) {
        select_all();
        return;
    }
    if (event.alt() && event.shift() && event.key() == KeyCode::Key_S) {
        sort_selected_lines();
        return;
    }
    if (event.key() == KeyCode::Key_Backspace) {
        if (is_readonly())
            return;
        if (has_selection()) {
            delete_selection();
            did_update_selection();
            return;
        }
        if (m_cursor.column() > 0) {
            int erase_count = 1;
            if (current_line().first_non_whitespace_column() >= m_cursor.column()) {
                int new_column;
                if (m_cursor.column() % m_soft_tab_width == 0)
                    new_column = m_cursor.column() - m_soft_tab_width;
                else
                    new_column = (m_cursor.column() / m_soft_tab_width) * m_soft_tab_width;
                erase_count = m_cursor.column() - new_column;
            }

            // Backspace within line
            TextRange erased_range({ m_cursor.line(), m_cursor.column() - erase_count }, m_cursor);
            auto erased_text = document().text_in_range(erased_range);
            execute<RemoveTextCommand>(erased_text, erased_range);
            return;
        }
        if (m_cursor.column() == 0 && m_cursor.line() != 0) {
            // Backspace at column 0; merge with previous line
            size_t previous_length = line(m_cursor.line() - 1).length();
            TextRange erased_range({ m_cursor.line() - 1, previous_length }, m_cursor);
            execute<RemoveTextCommand>("\n", erased_range);
            return;
        }
        return;
    }

    if (event.modifiers() == Mod_Shift && event.key() == KeyCode::Key_Delete) {
        if (is_readonly())
            return;
        delete_current_line();
        return;
    }

    if (event.key() == KeyCode::Key_Delete) {
        if (is_readonly())
            return;
        do_delete();
        return;
    }

    if (!is_readonly() && !event.ctrl() && !event.alt() && !event.text().is_empty()) {
        insert_at_cursor_or_replace_selection(event.text());
        return;
    }

    event.ignore();
}

void TextEditor::delete_current_line()
{
    if (has_selection())
        return delete_selection();

    TextPosition start;
    TextPosition end;
    if (m_cursor.line() == 0 && line_count() == 1) {
        start = { 0, 0 };
        end = { 0, line(0).length() };
    } else if (m_cursor.line() == line_count() - 1) {
        start = { m_cursor.line() - 1, line(m_cursor.line()).length() };
        end = { m_cursor.line(), line(m_cursor.line()).length() };
    } else {
        start = { m_cursor.line(), 0 };
        end = { m_cursor.line() + 1, 0 };
    }

    TextRange erased_range(start, end);
    execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range);
}

void TextEditor::do_delete()
{
    if (is_readonly())
        return;

    if (has_selection())
        return delete_selection();

    if (m_cursor.column() < current_line().length()) {
        // Delete within line
        TextRange erased_range(m_cursor, { m_cursor.line(), m_cursor.column() + 1 });
        execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range);
        return;
    }
    if (m_cursor.column() == current_line().length() && m_cursor.line() != line_count() - 1) {
        // Delete at end of line; merge with next line
        TextRange erased_range(m_cursor, { m_cursor.line() + 1, 0 });
        execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range);
        return;
    }
}

int TextEditor::content_x_for_position(const TextPosition& position) const
{
    auto& line = this->line(position.line());
    int x_offset = -1;
    switch (m_text_alignment) {
    case Gfx::TextAlignment::CenterLeft:
        for_each_visual_line(position.line(), [&](const Gfx::Rect&, const StringView& view, size_t start_of_visual_line) {
            if (position.column() >= start_of_visual_line && ((position.column() - start_of_visual_line) <= view.length())) {
                x_offset = (position.column() - start_of_visual_line) * glyph_width();
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return m_horizontal_content_padding + x_offset;
    case Gfx::TextAlignment::CenterRight:
        // FIXME
        ASSERT(!is_line_wrapping_enabled());
        return content_width() - m_horizontal_content_padding - (line.length() * glyph_width()) + (position.column() * glyph_width());
    default:
        ASSERT_NOT_REACHED();
    }
}

Gfx::Rect TextEditor::content_rect_for_position(const TextPosition& position) const
{
    if (!position.is_valid())
        return {};
    ASSERT(!lines().is_empty());
    ASSERT(position.column() <= (current_line().length() + 1));

    int x = content_x_for_position(position);

    if (is_single_line()) {
        Gfx::Rect rect { x, 0, 1, font().glyph_height() + 2 };
        rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return rect;
    }

    Gfx::Rect rect;
    for_each_visual_line(position.line(), [&](const Gfx::Rect& visual_line_rect, const StringView& view, size_t start_of_visual_line) {
        if (position.column() >= start_of_visual_line && ((position.column() - start_of_visual_line) <= view.length())) {
            // NOTE: We have to subtract the horizontal padding here since it's part of the visual line rect
            //       *and* included in what we get from content_x_for_position().
            rect = {
                visual_line_rect.x() + x - (m_horizontal_content_padding),
                visual_line_rect.y(),
                1,
                line_height()
            };
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return rect;
}

Gfx::Rect TextEditor::cursor_content_rect() const
{
    return content_rect_for_position(m_cursor);
}

Gfx::Rect TextEditor::line_widget_rect(size_t line_index) const
{
    auto rect = line_content_rect(line_index);
    rect.set_x(frame_thickness());
    rect.set_width(frame_inner_rect().width());
    rect.move_by(0, -(vertical_scrollbar().value()));
    rect.move_by(0, frame_thickness());
    rect.intersect(frame_inner_rect());
    return rect;
}

void TextEditor::scroll_position_into_view(const TextPosition& position)
{
    auto rect = content_rect_for_position(position);
    if (position.column() == 0)
        rect.set_x(content_x_for_position({ position.line(), 0 }) - 2);
    else if (position.column() == line(position.line()).length())
        rect.set_x(content_x_for_position({ position.line(), line(position.line()).length() }) + 2);
    scroll_into_view(rect, true, true);
}

void TextEditor::scroll_cursor_into_view()
{
    scroll_position_into_view(m_cursor);
}

Gfx::Rect TextEditor::line_content_rect(size_t line_index) const
{
    auto& line = this->line(line_index);
    if (is_single_line()) {
        Gfx::Rect line_rect = { content_x_for_position({ line_index, 0 }), 0, (int)line.length() * glyph_width(), font().glyph_height() + 2 };
        line_rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return line_rect;
    }
    if (is_line_wrapping_enabled())
        return m_line_visual_data[line_index].visual_rect;
    return {
        content_x_for_position({ line_index, 0 }),
        (int)line_index * line_height(),
        (int)line.length() * glyph_width(),
        line_height()
    };
}

void TextEditor::update_cursor()
{
    update(line_widget_rect(m_cursor.line()));
}

void TextEditor::set_cursor(size_t line, size_t column)
{
    set_cursor({ line, column });
}

void TextEditor::set_cursor(const TextPosition& a_position)
{
    ASSERT(!lines().is_empty());

    TextPosition position = a_position;

    if (position.line() >= line_count())
        position.set_line(line_count() - 1);

    if (position.column() > lines()[position.line()].length())
        position.set_column(lines()[position.line()].length());

    if (m_cursor != position) {
        // NOTE: If the old cursor is no longer valid, repaint everything just in case.
        auto old_cursor_line_rect = m_cursor.line() < line_count()
            ? line_widget_rect(m_cursor.line())
            : rect();
        m_cursor = position;
        m_cursor_state = true;
        scroll_cursor_into_view();
        update(old_cursor_line_rect);
        update_cursor();
    }
    cursor_did_change();
    if (on_cursor_change)
        on_cursor_change();
    if (m_highlighter)
        m_highlighter->cursor_did_change();
}

void TextEditor::focusin_event(Core::Event&)
{
    update_cursor();
    start_timer(500);
}

void TextEditor::focusout_event(Core::Event&)
{
    stop_timer();
}

void TextEditor::timer_event(Core::TimerEvent&)
{
    m_cursor_state = !m_cursor_state;
    if (is_focused())
        update_cursor();
}

bool TextEditor::write_to_file(const StringView& path)
{
    int fd = open_with_path_length(path.characters_without_null_termination(), path.length(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
    if (fd < 0) {
        perror("open");
        return false;
    }

    // Compute the final file size and ftruncate() to make writing fast.
    // FIXME: Remove this once the kernel is smart enough to do this instead.
    off_t file_size = 0;
    for (size_t i = 0; i < line_count(); ++i)
        file_size += line(i).length();
    file_size += line_count() - 1;

    int rc = ftruncate(fd, file_size);
    if (rc < 0) {
        perror("ftruncate");
        return false;
    }

    for (size_t i = 0; i < line_count(); ++i) {
        auto& line = this->line(i);
        if (line.length()) {
            ssize_t nwritten = write(fd, line.characters(), line.length());
            if (nwritten < 0) {
                perror("write");
                close(fd);
                return false;
            }
        }
        if (i != line_count() - 1) {
            char ch = '\n';
            ssize_t nwritten = write(fd, &ch, 1);
            if (nwritten != 1) {
                perror("write");
                close(fd);
                return false;
            }
        }
    }

    close(fd);
    return true;
}

String TextEditor::text() const
{
    StringBuilder builder;
    for (size_t i = 0; i < line_count(); ++i) {
        auto& line = this->line(i);
        builder.append(line.characters(), line.length());
        if (i != line_count() - 1)
            builder.append('\n');
    }
    return builder.to_string();
}

void TextEditor::clear()
{
    document().remove_all_lines();
    document().append_line(make<TextDocumentLine>(document()));
    m_selection.clear();
    did_update_selection();
    set_cursor(0, 0);
    update();
}

String TextEditor::selected_text() const
{
    if (!has_selection())
        return {};

    return document().text_in_range(m_selection);
}

void TextEditor::delete_selection()
{
    auto selection = normalized_selection();
    execute<RemoveTextCommand>(selected_text(), selection);
    m_selection.clear();
    did_update_selection();
    did_change();
    set_cursor(selection.start());
    update();
}

void TextEditor::insert_at_cursor_or_replace_selection(const StringView& text)
{
    ASSERT(!is_readonly());
    if (has_selection())
        delete_selection();
    execute<InsertTextCommand>(text, m_cursor);
}

void TextEditor::cut()
{
    if (is_readonly())
        return;
    auto selected_text = this->selected_text();
    printf("Cut: \"%s\"\n", selected_text.characters());
    Clipboard::the().set_data(selected_text);
    delete_selection();
}

void TextEditor::copy()
{
    auto selected_text = this->selected_text();
    printf("Copy: \"%s\"\n", selected_text.characters());
    Clipboard::the().set_data(selected_text);
}

void TextEditor::paste()
{
    if (is_readonly())
        return;
    auto paste_text = Clipboard::the().data();
    printf("Paste: \"%s\"\n", paste_text.characters());

    TemporaryChange change(m_automatic_indentation_enabled, false);
    insert_at_cursor_or_replace_selection(paste_text);
}

void TextEditor::enter_event(Core::Event&)
{
    ASSERT(window());
    window()->set_override_cursor(StandardCursor::IBeam);
}

void TextEditor::leave_event(Core::Event&)
{
    ASSERT(window());
    window()->set_override_cursor(StandardCursor::None);
}

void TextEditor::did_change()
{
    update_content_size();
    recompute_all_visual_lines();
    m_undo_action->set_enabled(can_undo());
    m_redo_action->set_enabled(can_redo());
    if (!m_has_pending_change_notification) {
        m_has_pending_change_notification = true;
        deferred_invoke([this](auto&) {
            if (!m_has_pending_change_notification)
                return;
            if (on_change)
                on_change();
            if (m_highlighter)
                m_highlighter->rehighlight();
            m_has_pending_change_notification = false;
        });
    }
}

void TextEditor::set_readonly(bool readonly)
{
    if (m_readonly == readonly)
        return;
    m_readonly = readonly;
    m_cut_action->set_enabled(!is_readonly() && has_selection());
    m_delete_action->set_enabled(!is_readonly());
    m_paste_action->set_enabled(!is_readonly());
}

void TextEditor::did_update_selection()
{
    m_cut_action->set_enabled(!is_readonly() && has_selection());
    m_copy_action->set_enabled(has_selection());
    if (on_selection_change)
        on_selection_change();
    if (is_line_wrapping_enabled()) {
        // FIXME: Try to repaint less.
        update();
    }
}

void TextEditor::context_menu_event(ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = Menu::construct();
        m_context_menu->add_action(undo_action());
        m_context_menu->add_action(redo_action());
        m_context_menu->add_separator();
        m_context_menu->add_action(cut_action());
        m_context_menu->add_action(copy_action());
        m_context_menu->add_action(paste_action());
        m_context_menu->add_action(delete_action());
        if (is_multi_line()) {
            m_context_menu->add_separator();
            m_context_menu->add_action(go_to_line_action());
        }
        if (!m_custom_context_menu_actions.is_empty()) {
            m_context_menu->add_separator();
            for (auto& action : m_custom_context_menu_actions) {
                m_context_menu->add_action(action);
            }
        }
    }
    m_context_menu->popup(event.screen_position());
}

void TextEditor::set_text_alignment(Gfx::TextAlignment alignment)
{
    if (m_text_alignment == alignment)
        return;
    m_text_alignment = alignment;
    update();
}

void TextEditor::resize_event(ResizeEvent& event)
{
    ScrollableWidget::resize_event(event);
    update_content_size();
    recompute_all_visual_lines();
}

void TextEditor::set_selection(const TextRange& selection)
{
    if (m_selection == selection)
        return;
    m_selection = selection;
    set_cursor(m_selection.end());
    scroll_position_into_view(normalized_selection().start());
    update();
}

void TextEditor::clear_selection()
{
    if (!has_selection())
        return;
    m_selection.clear();
    update();
}

void TextEditor::recompute_all_visual_lines()
{
    int y_offset = 0;
    for (size_t line_index = 0; line_index < line_count(); ++line_index) {
        recompute_visual_lines(line_index);
        m_line_visual_data[line_index].visual_rect.set_y(y_offset);
        y_offset += m_line_visual_data[line_index].visual_rect.height();
    }

    update_content_size();
}

void TextEditor::ensure_cursor_is_valid()
{
    auto new_cursor = m_cursor;
    if (new_cursor.line() >= line_count())
        new_cursor.set_line(line_count() - 1);
    if (new_cursor.column() > line(new_cursor.line()).length())
        new_cursor.set_column(line(new_cursor.line()).length());
    if (m_cursor != new_cursor)
        set_cursor(new_cursor);
}

size_t TextEditor::visual_line_containing(size_t line_index, size_t column) const
{
    size_t visual_line_index = 0;
    for_each_visual_line(line_index, [&](const Gfx::Rect&, const StringView& view, size_t start_of_visual_line) {
        if (column >= start_of_visual_line && ((column - start_of_visual_line) < view.length()))
            return IterationDecision::Break;
        ++visual_line_index;
        return IterationDecision::Continue;
    });
    return visual_line_index;
}

void TextEditor::recompute_visual_lines(size_t line_index)
{
    auto& line = document().line(line_index);
    auto& visual_data = m_line_visual_data[line_index];

    visual_data.visual_line_breaks.clear_with_capacity();

    int available_width = visible_text_rect_in_inner_coordinates().width();

    if (is_line_wrapping_enabled()) {
        int line_width_so_far = 0;

        for (size_t i = 0; i < line.length(); ++i) {
            auto ch = line.characters()[i];
            auto glyph_width = font().glyph_width(ch);
            if ((line_width_so_far + glyph_width) > available_width) {
                visual_data.visual_line_breaks.append(i);
                line_width_so_far = glyph_width;
                continue;
            }
            line_width_so_far += glyph_width;
        }
    }

    visual_data.visual_line_breaks.append(line.length());

    if (is_line_wrapping_enabled())
        visual_data.visual_rect = { m_horizontal_content_padding, 0, available_width, visual_data.visual_line_breaks.size() * line_height() };
    else
        visual_data.visual_rect = { m_horizontal_content_padding, 0, font().width(line.view()), line_height() };
}

template<typename Callback>
void TextEditor::for_each_visual_line(size_t line_index, Callback callback) const
{
    auto editor_visible_text_rect = visible_text_rect_in_inner_coordinates();
    size_t start_of_line = 0;
    size_t visual_line_index = 0;

    auto& line = document().line(line_index);
    auto& visual_data = m_line_visual_data[line_index];

    for (auto visual_line_break : visual_data.visual_line_breaks) {
        auto visual_line_view = StringView(line.characters() + start_of_line, visual_line_break - start_of_line);
        Gfx::Rect visual_line_rect {
            visual_data.visual_rect.x(),
            visual_data.visual_rect.y() + ((int)visual_line_index * line_height()),
            font().width(visual_line_view),
            line_height()
        };
        if (is_right_text_alignment(text_alignment()))
            visual_line_rect.set_right_without_resize(editor_visible_text_rect.right());
        if (!is_multi_line())
            visual_line_rect.center_vertically_within(editor_visible_text_rect);
        if (callback(visual_line_rect, visual_line_view, start_of_line) == IterationDecision::Break)
            break;
        start_of_line = visual_line_break;
        ++visual_line_index;
    }
}

void TextEditor::set_line_wrapping_enabled(bool enabled)
{
    if (m_line_wrapping_enabled == enabled)
        return;

    m_line_wrapping_enabled = enabled;
    horizontal_scrollbar().set_visible(!m_line_wrapping_enabled);
    update_content_size();
    recompute_all_visual_lines();
    update();
}

void TextEditor::add_custom_context_menu_action(Action& action)
{
    m_custom_context_menu_actions.append(action);
}

void TextEditor::did_change_font()
{
    vertical_scrollbar().set_step(line_height());
    recompute_all_visual_lines();
    update();
    Widget::did_change_font();
}

void TextEditor::document_did_append_line()
{
    m_line_visual_data.append(make<LineVisualData>());
    recompute_all_visual_lines();
    update();
}

void TextEditor::document_did_remove_line(size_t line_index)
{
    m_line_visual_data.remove(line_index);
    recompute_all_visual_lines();
    update();
}

void TextEditor::document_did_remove_all_lines()
{
    m_line_visual_data.clear();
    recompute_all_visual_lines();
    update();
}

void TextEditor::document_did_insert_line(size_t line_index)
{
    m_line_visual_data.insert(line_index, make<LineVisualData>());
    recompute_all_visual_lines();
    update();
}

void TextEditor::document_did_change()
{
    did_change();
    update();
}

void TextEditor::document_did_set_text()
{
    m_line_visual_data.clear();
    for (size_t i = 0; i < m_document->line_count(); ++i)
        m_line_visual_data.append(make<LineVisualData>());
    document_did_change();
}

void TextEditor::document_did_set_cursor(const TextPosition& position)
{
    set_cursor(position);
}

void TextEditor::set_document(TextDocument& document)
{
    if (m_document.ptr() == &document)
        return;
    if (m_document)
        m_document->unregister_client(*this);
    m_document = document;
    m_line_visual_data.clear();
    for (size_t i = 0; i < m_document->line_count(); ++i) {
        m_line_visual_data.append(make<LineVisualData>());
    }
    m_cursor = { 0, 0 };
    if (has_selection())
        m_selection.clear();
    recompute_all_visual_lines();
    update();
    m_document->register_client(*this);
}

void TextEditor::flush_pending_change_notification_if_needed()
{
    if (!m_has_pending_change_notification)
        return;
    if (on_change)
        on_change();
    if (m_highlighter)
        m_highlighter->rehighlight();
    m_has_pending_change_notification = false;
}

void TextEditor::set_syntax_highlighter(OwnPtr<SyntaxHighlighter> highlighter)
{
    if (m_highlighter)
        m_highlighter->detach();
    m_highlighter = move(highlighter);
    if (m_highlighter) {
        m_highlighter->attach(*this);
        m_highlighter->rehighlight();
    }
}

int TextEditor::line_height() const
{
    return font().glyph_height() + m_line_spacing;
}

int TextEditor::glyph_width() const
{
    return font().glyph_width('x');
}

}
