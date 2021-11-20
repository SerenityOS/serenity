/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/EditingEngine.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RegularEditingEngine.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibSyntax/Highlighter.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

REGISTER_WIDGET(GUI, TextEditor)

namespace GUI {

TextEditor::TextEditor(Type type)
    : m_type(type)
{
    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_STRING_PROPERTY("placeholder", placeholder, set_placeholder);
    REGISTER_ENUM_PROPERTY("mode", mode, set_mode, Mode,
        { Editable, "Editable" },
        { ReadOnly, "ReadOnly" },
        { DisplayOnly, "DisplayOnly" });

    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_accepts_emoji_input(true);
    set_override_cursor(Gfx::StandardCursor::IBeam);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    set_document(TextDocument::create());
    if (is_single_line())
        set_visualize_trailing_whitespace(false);
    set_scrollbars_enabled(is_multi_line());
    if (is_multi_line())
        set_font(Gfx::FontDatabase::default_fixed_width_font());
    vertical_scrollbar().set_step(line_height());
    m_cursor = { 0, 0 };
    m_automatic_selection_scroll_timer = add<Core::Timer>(100, [this] {
        automatic_selection_scroll_timer_fired();
    });
    m_automatic_selection_scroll_timer->stop();
    create_actions();
    set_editing_engine(make<RegularEditingEngine>());
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
    m_cut_action->set_enabled(false);
    m_copy_action->set_enabled(false);
    m_paste_action = CommonActions::make_paste_action([&](auto&) { paste(); }, this);
    m_paste_action->set_enabled(is_editable() && Clipboard::the().fetch_mime_type().starts_with("text/"));
    if (is_multi_line()) {
        m_go_to_line_action = Action::create(
            "Go to line...", { Mod_Ctrl, Key_L }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
                String value;
                if (InputBox::show(window(), value, "Line:", "Go to line") == InputBox::ExecOK) {
                    auto line_target = value.to_uint();
                    if (line_target.has_value()) {
                        set_cursor_and_focus_line(line_target.value() - 1, 0);
                    }
                }
            },
            this);
    }
    m_select_all_action = CommonActions::make_select_all_action([this](auto&) { select_all(); }, this);
}

void TextEditor::set_text(StringView text, AllowCallback allow_callback)
{
    m_selection.clear();

    document().set_text(text, allow_callback);

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
    set_size_occupied_by_fixed_elements({ ruler_width() + gutter_width(), 0 });
}

TextPosition TextEditor::text_position_at_content_position(Gfx::IntPoint const& content_position) const
{
    auto position = content_position;
    if (is_single_line() && icon())
        position.translate_by(-(icon_size() + icon_padding()), 0);

    size_t line_index = 0;

    if (position.y() >= 0) {
        if (is_wrapping_enabled()) {
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
    }

    size_t column_index = 0;
    switch (m_text_alignment) {
    case Gfx::TextAlignment::CenterLeft:
        for_each_visual_line(line_index, [&](Gfx::IntRect const& rect, auto& view, size_t start_of_line, [[maybe_unused]] bool is_last_visual_line) {
            if (is_multi_line() && !rect.contains_vertically(position.y()) && !is_last_visual_line)
                return IterationDecision::Continue;
            column_index = start_of_line;
            if (position.x() <= 0) {
                // We're outside the text on the left side, put cursor at column 0 on this visual line.
            } else {
                int glyph_x = 0;
                size_t i = 0;
                for (; i < view.length(); ++i) {
                    int advance = font().glyph_width(view.code_points()[i]) + font().glyph_spacing();
                    if ((glyph_x + (advance / 2)) >= position.x())
                        break;
                    glyph_x += advance;
                }
                column_index += i;
            }
            return IterationDecision::Break;
        });
        break;
    case Gfx::TextAlignment::CenterRight:
        // FIXME: Support right-aligned line wrapping, I guess.
        VERIFY(!is_wrapping_enabled());
        column_index = (position.x() - content_x_for_position({ line_index, 0 }) + fixed_glyph_width() / 2) / fixed_glyph_width();
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    column_index = max((size_t)0, min(column_index, line(line_index).length()));
    return { line_index, column_index };
}

TextPosition TextEditor::text_position_at(Gfx::IntPoint const& widget_position) const
{
    auto content_position = widget_position;
    content_position.translate_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    content_position.translate_by(-(m_horizontal_content_padding + ruler_width() + gutter_width()), 0);
    content_position.translate_by(-frame_thickness(), -frame_thickness());
    return text_position_at_content_position(content_position);
}

void TextEditor::doubleclick_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;

    if (is_displayonly())
        return;

    if (!current_line().can_select())
        return;

    rehighlight_if_needed();

    m_triple_click_timer.start();
    m_in_drag_select = false;

    auto position = text_position_at(event.position());

    if (m_substitution_code_point) {
        // NOTE: If we substitute the code points, we don't want double clicking to only select a single word, since
        //       whitespace isn't visible anymore.
        m_selection = document().range_for_entire_line(position.line());
    } else if (document().has_spans()) {
        for (auto& span : document().spans()) {
            if (span.range.contains(position)) {
                m_selection = span.range;
                break;
            }
        }
    } else {
        m_selection.set_start(document().first_word_break_before(position, false));
        m_selection.set_end(document().first_word_break_after(position));
    }

    set_cursor(m_selection.end());
    update();
    did_update_selection();
}

void TextEditor::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary) {
        return;
    }

    if (on_mousedown)
        on_mousedown();

    if (is_displayonly())
        return;

    if (m_triple_click_timer.is_valid() && m_triple_click_timer.elapsed() < 250) {
        m_triple_click_timer = Core::ElapsedTimer();
        select_current_line();
        return;
    }

    if (event.modifiers() & Mod_Shift) {
        if (!has_selection())
            m_selection.set(m_cursor, {});
    } else {
        m_selection.clear();
    }

    m_in_drag_select = true;
    m_automatic_selection_scroll_timer->start();

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
    if (event.button() == MouseButton::Primary) {
        if (m_in_drag_select) {
            m_in_drag_select = false;
        }
        return;
    }
}

void TextEditor::mousemove_event(MouseEvent& event)
{
    m_last_mousemove_position = event.position();
    if (m_in_drag_select && (rect().contains(event.position()) || !m_automatic_selection_scroll_timer->is_active())) {
        set_cursor(text_position_at(event.position()));
        m_selection.set_end(m_cursor);
        did_update_selection();
        update();
        return;
    }
}

void TextEditor::select_current_line()
{
    m_selection = document().range_for_entire_line(m_cursor.line());
    set_cursor(m_selection.end());
    update();
    did_update_selection();
}

void TextEditor::automatic_selection_scroll_timer_fired()
{
    if (!m_in_drag_select) {
        m_automatic_selection_scroll_timer->stop();
        return;
    }
    set_cursor(text_position_at(m_last_mousemove_position));
    m_selection.set_end(m_cursor);
    did_update_selection();
    update();
}

int TextEditor::ruler_width() const
{
    if (!m_ruler_visible)
        return 0;
    int line_count_digits = static_cast<int>(log10(line_count())) + 1;
    constexpr size_t padding = 5;
    return line_count() < 10 ? (line_count_digits + 1) * font().glyph_width('x') + padding : line_count_digits * font().glyph_width('x') + padding;
}

int TextEditor::gutter_width() const
{
    if (!m_gutter_visible)
        return 0;
    return line_height(); // square gutter
}

Gfx::IntRect TextEditor::ruler_content_rect(size_t line_index) const
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

Gfx::IntRect TextEditor::gutter_content_rect(size_t line_index) const
{
    if (!m_gutter_visible)
        return {};
    return {
        0 - ruler_width() - gutter_width() + horizontal_scrollbar().value(),
        line_content_rect(line_index).y(),
        gutter_width(),
        line_content_rect(line_index).height()
    };
}

Gfx::IntRect TextEditor::ruler_rect_in_inner_coordinates() const
{
    return { gutter_width(), 0, ruler_width(), height() - height_occupied_by_horizontal_scrollbar() };
}

Gfx::IntRect TextEditor::gutter_rect_in_inner_coordinates() const
{
    return { 0, 0, gutter_width(), height() - height_occupied_by_horizontal_scrollbar() };
}

Gfx::IntRect TextEditor::visible_text_rect_in_inner_coordinates() const
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
    Color widget_background_color = palette().color(is_enabled() ? background_role() : Gfx::ColorRole::Window);

    rehighlight_if_needed();

    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), widget_background_color);

    // NOTE: This lambda and TextEditor::text_width_for_font() are used to substitute all glyphs with m_substitution_code_point if necessary.
    //       Painter::draw_text() and Gfx::Font::width() should not be called directly, but using this lambda and TextEditor::text_width_for_font().
    auto draw_text = [&](Gfx::IntRect const& rect, auto const& raw_text, Gfx::Font const& font, Gfx::TextAlignment alignment, Gfx::Color color, bool substitue = true) {
        if (m_substitution_code_point && substitue) {
            painter.draw_text(rect, substitution_code_point_view(raw_text.length()), font, alignment, color);
        } else {
            painter.draw_text(rect, raw_text, font, alignment, color);
        }
    };

    if (is_displayonly() && is_focused()) {
        widget_background_color = palette().selection();
        Gfx::IntRect display_rect {
            widget_inner_rect().x() + 1,
            widget_inner_rect().y() + 1,
            widget_inner_rect().width() - 2,
            widget_inner_rect().height() - 2
        };
        painter.add_clip_rect(display_rect);
        painter.add_clip_rect(event.rect());
        painter.fill_rect(event.rect(), widget_background_color);
    }

    painter.translate(frame_thickness(), frame_thickness());

    if (m_gutter_visible) {
        auto gutter_rect = gutter_rect_in_inner_coordinates();
        painter.fill_rect(gutter_rect, palette().gutter());
        if (!m_ruler_visible)
            painter.draw_line(gutter_rect.top_right(), gutter_rect.bottom_right(), palette().gutter_border());
    }

    if (m_ruler_visible) {
        auto ruler_rect = ruler_rect_in_inner_coordinates();
        painter.fill_rect(ruler_rect, palette().ruler());
        painter.draw_line(ruler_rect.top_right(), ruler_rect.bottom_right(), palette().ruler_border());
    }

    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    painter.translate(gutter_width(), 0);
    painter.translate(ruler_width(), 0);

    size_t first_visible_line = text_position_at(event.rect().top_left()).line();
    size_t last_visible_line = text_position_at(event.rect().bottom_right()).line();

    auto selection = normalized_selection();
    bool has_selection = selection.is_valid();

    if (m_ruler_visible) {
        for (size_t i = first_visible_line; i <= last_visible_line; ++i) {
            bool is_current_line = i == m_cursor.line();
            auto ruler_line_rect = ruler_content_rect(i);
            // NOTE: Use Painter::draw_text() directly here, as we want to always draw the line numbers in clear text.
            painter.draw_text(
                ruler_line_rect.shrunken(2, 0).translated(0, m_line_spacing / 2),
                String::number(i + 1),
                is_current_line ? font().bold_variant() : font(),
                Gfx::TextAlignment::TopRight,
                is_current_line ? palette().ruler_active_text() : palette().ruler_inactive_text());
        }
    }

    auto text_left = 0;
    if (m_ruler_visible)
        text_left = ruler_rect_in_inner_coordinates().right() + 1;
    else if (m_gutter_visible)
        text_left = gutter_rect_in_inner_coordinates().right() + 1;
    text_left += frame_thickness();

    Gfx::IntRect text_clip_rect {
        0,
        frame_thickness(),
        width() - width_occupied_by_vertical_scrollbar() - text_left,
        height() - height_occupied_by_horizontal_scrollbar()
    };
    text_clip_rect.translate_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    painter.add_clip_rect(text_clip_rect);

    size_t span_index = 0;
    if (document().has_spans()) {
        for (;;) {
            if (span_index >= document().spans().size() || document().spans()[span_index].range.end().line() >= first_visible_line) {
                break;
            }
            ++span_index;
        }
    }

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
        for_each_visual_line(line_index, [&](Gfx::IntRect const& visual_line_rect, auto& visual_line_text, size_t start_of_visual_line, [[maybe_unused]] bool is_last_visual_line) {
            if (is_multi_line() && line_index == m_cursor.line() && is_cursor_line_highlighted())
                painter.fill_rect(visual_line_rect, widget_background_color.darkened(0.9f));
            if constexpr (TEXTEDITOR_DEBUG)
                painter.draw_rect(visual_line_rect, Color::Cyan);

            if (!placeholder().is_empty() && document().is_empty() && line_index == 0) {
                auto line_rect = visual_line_rect;
                line_rect.set_width(text_width_for_font(placeholder(), font()));
                draw_text(line_rect, placeholder(), font(), m_text_alignment, palette().color(Gfx::ColorRole::PlaceholderText), false);
            } else if (!document().has_spans()) {
                // Fast-path for plain text
                auto color = palette().color(is_enabled() ? foreground_role() : Gfx::ColorRole::DisabledText);
                if (is_displayonly() && is_focused())
                    color = palette().color(is_enabled() ? Gfx::ColorRole::SelectionText : Gfx::ColorRole::DisabledText);
                draw_text(visual_line_rect, visual_line_text, font(), m_text_alignment, color);
            } else {
                auto unspanned_color = palette().color(is_enabled() ? foreground_role() : Gfx::ColorRole::DisabledText);
                if (is_displayonly() && is_focused())
                    unspanned_color = palette().color(is_enabled() ? Gfx::ColorRole::SelectionText : Gfx::ColorRole::DisabledText);
                RefPtr<Gfx::Font> unspanned_font = this->font();

                size_t next_column = 0;
                Gfx::IntRect span_rect = { visual_line_rect.location(), { 0, line_height() } };

                auto draw_text_helper = [&](size_t start, size_t end, RefPtr<Gfx::Font>& font, Color& color, Optional<Color> background_color = {}, bool underline = false) {
                    size_t length = end - start;
                    if (length == 0)
                        return;
                    auto text = visual_line_text.substring_view(start, length);
                    span_rect.set_width(font->width(text));
                    if (background_color.has_value()) {
                        painter.fill_rect(span_rect, background_color.value());
                    }
                    draw_text(span_rect, text, *font, m_text_alignment, color);
                    if (underline) {
                        painter.draw_line(span_rect.bottom_left().translated(0, 1), span_rect.bottom_right().translated(0, 1), color);
                    }
                    span_rect.translate_by(span_rect.width(), 0);
                };
                for (;;) {
                    if (span_index >= document().spans().size()) {
                        break;
                    }
                    auto& span = document().spans()[span_index];
                    if (!span.range.is_valid()) {
                        ++span_index;
                        continue;
                    }
                    if (span.range.end().line() < line_index) {
                        dbgln_if(TEXTEDITOR_DEBUG, "spans not sorted (span end {}:{} is before current line {}) => ignoring", span.range.end().line(), span.range.end().column(), line_index);
                        ++span_index;
                        continue;
                    }
                    if (span.range.start().line() > line_index
                        || (span.range.start().line() == line_index && span.range.start().column() >= start_of_visual_line + visual_line_text.length())) {
                        // no more spans in this line, moving on
                        break;
                    }
                    if (span.range.start().line() == span.range.end().line() && span.range.end().column() < span.range.start().column()) {
                        dbgln_if(TEXTEDITOR_DEBUG, "span form {}:{} to {}:{} has negative length => ignoring", span.range.start().line(), span.range.start().column(), span.range.end().line(), span.range.end().column());
                        ++span_index;
                        continue;
                    }
                    if (span.range.end().line() == line_index && span.range.end().column() < start_of_visual_line + next_column) {
                        dbgln_if(TEXTEDITOR_DEBUG, "spans not sorted (span end {}:{} is before current position {}:{}) => ignoring",
                            span.range.end().line(), span.range.end().column(), line_index, start_of_visual_line + next_column);
                        ++span_index;
                        continue;
                    }
                    size_t span_start;
                    if (span.range.start().line() < line_index || span.range.start().column() < start_of_visual_line) {
                        span_start = 0;
                    } else {
                        span_start = span.range.start().column() - start_of_visual_line;
                    }
                    if (span_start < next_column) {
                        dbgln_if(TEXTEDITOR_DEBUG, "span started before the current position, maybe two spans overlap? (span start {} is before current position {}) => ignoring", span_start, next_column);
                        ++span_index;
                        continue;
                    }
                    size_t span_end;
                    bool span_consumned;
                    if (span.range.end().line() > line_index || span.range.end().column() > start_of_visual_line + visual_line_text.length()) {
                        span_end = visual_line_text.length();
                        span_consumned = false;
                    } else {
                        span_end = span.range.end().column() - start_of_visual_line;
                        span_consumned = true;
                    }

                    if (span_start != next_column) {
                        // draw unspanned text between spans
                        draw_text_helper(next_column, span_start, unspanned_font, unspanned_color);
                    }
                    auto font = unspanned_font;
                    if (span.attributes.bold) {
                        if (auto bold_font = Gfx::FontDatabase::the().get(font->family(), font->presentation_size(), 700))
                            font = bold_font;
                    }
                    draw_text_helper(span_start, span_end, font, span.attributes.color, span.attributes.background_color, span.attributes.underline);
                    next_column = span_end;
                    if (!span_consumned) {
                        // continue with same span on next line
                        break;
                    } else {
                        ++span_index;
                    }
                }
                // draw unspanned text after last span
                if (next_column < visual_line_text.length()) {
                    draw_text_helper(next_column, visual_line_text.length(), unspanned_font, unspanned_color);
                }
                // consume all spans that should end this line
                // this is necessary since the spans can include the new line character
                while (is_last_visual_line && span_index < document().spans().size()) {
                    auto& span = document().spans()[span_index];
                    if (span.range.end().line() == line_index) {
                        ++span_index;
                    } else {
                        break;
                    }
                }
            }

            if (m_visualize_trailing_whitespace && line.ends_in_whitespace()) {
                size_t physical_column;
                auto last_non_whitespace_column = line.last_non_whitespace_column();
                if (last_non_whitespace_column.has_value())
                    physical_column = last_non_whitespace_column.value() + 1;
                else
                    physical_column = 0;
                size_t end_of_visual_line = (start_of_visual_line + visual_line_text.length());
                if (physical_column < end_of_visual_line) {
                    size_t visual_column = physical_column > start_of_visual_line ? (physical_column - start_of_visual_line) : 0;
                    Gfx::IntRect whitespace_rect {
                        content_x_for_position({ line_index, visual_column }),
                        visual_line_rect.y(),
                        text_width_for_font(visual_line_text.substring_view(visual_column, visual_line_text.length() - visual_column), font()),
                        visual_line_rect.height()
                    };
                    painter.fill_rect_with_dither_pattern(whitespace_rect, Color(), Color(255, 192, 192));
                }
            }

            if (m_visualize_leading_whitespace && line.leading_spaces() > 0) {
                size_t physical_column = line.leading_spaces();
                size_t end_of_leading_whitespace = (start_of_visual_line + physical_column);
                size_t end_of_visual_line = (start_of_visual_line + visual_line_text.length());
                if (end_of_leading_whitespace < end_of_visual_line) {
                    Gfx::IntRect whitespace_rect {
                        content_x_for_position({ line_index, start_of_visual_line }),
                        visual_line_rect.y(),
                        text_width_for_font(visual_line_text.substring_view(0, end_of_leading_whitespace), font()),
                        visual_line_rect.height()
                    };
                    painter.fill_rect_with_dither_pattern(whitespace_rect, Color(), Color(192, 255, 192));
                }
            }

            if (physical_line_has_selection && window()->focused_widget() == this) {
                size_t start_of_selection_within_visual_line = (size_t)max(0, (int)selection_start_column_within_line - (int)start_of_visual_line);
                size_t end_of_selection_within_visual_line = selection_end_column_within_line - start_of_visual_line;

                bool current_visual_line_has_selection = start_of_selection_within_visual_line != end_of_selection_within_visual_line
                    && ((line_index != selection.start().line() && line_index != selection.end().line())
                        || (visual_line_index >= first_visual_line_with_selection && visual_line_index <= last_visual_line_with_selection));
                if (current_visual_line_has_selection) {
                    bool selection_begins_on_current_visual_line = visual_line_index == first_visual_line_with_selection;
                    bool selection_ends_on_current_visual_line = visual_line_index == last_visual_line_with_selection;

                    int selection_left = selection_begins_on_current_visual_line
                        ? content_x_for_position({ line_index, (size_t)selection_start_column_within_line })
                        : m_horizontal_content_padding;

                    int selection_right = selection_ends_on_current_visual_line
                        ? content_x_for_position({ line_index, (size_t)selection_end_column_within_line })
                        : visual_line_rect.right() + 1;

                    Gfx::IntRect selection_rect {
                        selection_left,
                        visual_line_rect.y(),
                        selection_right - selection_left,
                        visual_line_rect.height()
                    };

                    Color background_color = window()->is_active() ? palette().selection() : palette().inactive_selection();
                    Color text_color = window()->is_active() ? palette().selection_text() : palette().inactive_selection_text();

                    painter.fill_rect(selection_rect, background_color);

                    if (visual_line_text.code_points()) {
                        Utf32View visual_selected_text {
                            visual_line_text.code_points() + start_of_selection_within_visual_line,
                            end_of_selection_within_visual_line - start_of_selection_within_visual_line
                        };

                        draw_text(selection_rect, visual_selected_text, font(), Gfx::TextAlignment::CenterLeft, text_color);
                    }
                }
            }

            ++visual_line_index;
            return IterationDecision::Continue;
        });
    }

    if (!is_multi_line() && m_icon) {
        Gfx::IntRect icon_rect { icon_padding(), 1, icon_size(), icon_size() };
        painter.draw_scaled_bitmap(icon_rect, *m_icon, m_icon->rect());
    }

    if (is_focused() && m_cursor_state && !is_displayonly())
        painter.fill_rect(cursor_content_rect(), palette().text_cursor());
}

void TextEditor::select_all()
{
    TextPosition start_of_document { 0, 0 };
    TextPosition end_of_document { line_count() - 1, line(line_count() - 1).length() };
    m_selection.set(end_of_document, start_of_document);
    did_update_selection();
    set_cursor(start_of_document);
    update();
}

void TextEditor::keydown_event(KeyEvent& event)
{
    if (m_autocomplete_box && m_autocomplete_box->is_visible() && (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Tab)) {
        TemporaryChange change { m_should_keep_autocomplete_box, true };
        if (m_autocomplete_box->apply_suggestion() == AutocompleteProvider::Entry::HideAutocompleteAfterApplying::Yes)
            hide_autocomplete();
        else
            try_update_autocomplete();
        return;
    }

    if (m_autocomplete_box && m_autocomplete_box->is_visible() && event.key() == KeyCode::Key_Escape) {
        hide_autocomplete();
        return;
    }

    if (m_autocomplete_box && m_autocomplete_box->is_visible() && event.key() == KeyCode::Key_Up) {
        m_autocomplete_box->previous_suggestion();
        return;
    }

    if (m_autocomplete_box && m_autocomplete_box->is_visible() && event.key() == KeyCode::Key_Down) {
        m_autocomplete_box->next_suggestion();
        return;
    }

    if (is_single_line()) {
        if (event.key() == KeyCode::Key_Tab)
            return AbstractScrollableWidget::keydown_event(event);

        if (event.modifiers() == KeyModifier::Mod_Shift && event.key() == KeyCode::Key_Return) {
            if (on_shift_return_pressed)
                on_shift_return_pressed();
            return;
        }

        if (event.key() == KeyCode::Key_Return) {
            if (on_return_pressed)
                on_return_pressed();
            return;
        }

        if (event.key() == KeyCode::Key_Up) {
            if (on_up_pressed)
                on_up_pressed();
            return;
        }

        if (event.key() == KeyCode::Key_Down) {
            if (on_down_pressed)
                on_down_pressed();
            return;
        }

        if (event.key() == KeyCode::Key_PageUp) {
            if (on_pageup_pressed)
                on_pageup_pressed();
            return;
        }

        if (event.key() == KeyCode::Key_PageDown) {
            if (on_pagedown_pressed)
                on_pagedown_pressed();
            return;
        }

    } else if (!is_multi_line()) {
        VERIFY_NOT_REACHED();
    }

    ArmedScopeGuard update_autocomplete { [&] {
        try_update_autocomplete();
    } };

    if (is_multi_line() && !event.shift() && !event.alt() && event.ctrl() && event.key() == KeyCode::Key_Space) {
        if (m_autocomplete_provider) {
            try_show_autocomplete(UserRequestedAutocomplete::Yes);
            update_autocomplete.disarm();
            return;
        }
    }

    if (m_editing_engine->on_key(event))
        return;

    if (event.key() == KeyCode::Key_Escape) {
        if (on_escape_pressed)
            on_escape_pressed();
        return;
    }

    if (event.modifiers() == Mod_Shift && event.key() == KeyCode::Key_Delete) {
        if (m_autocomplete_box)
            hide_autocomplete();
        return;
    }

    if (event.key() == KeyCode::Key_Delete) {
        if (!is_editable())
            return;
        if (m_autocomplete_box)
            hide_autocomplete();
        if (has_selection()) {
            delete_selection();
            did_update_selection();
            return;
        }

        if (m_cursor.column() < current_line().length()) {
            // Delete within line
            int erase_count = 1;
            if (event.modifiers() == Mod_Ctrl) {
                auto word_break_pos = document().first_word_break_after(m_cursor);
                erase_count = word_break_pos.column() - m_cursor.column();
            }
            TextRange erased_range(m_cursor, { m_cursor.line(), m_cursor.column() + erase_count });
            execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range);
            return;
        }
        if (m_cursor.column() == current_line().length() && m_cursor.line() != line_count() - 1) {
            // Delete at end of line; merge with next line
            size_t erase_count = 0;
            if (event.modifiers() == Mod_Ctrl) {
                erase_count = document().first_word_break_after({ m_cursor.line() + 1, 0 }).column();
            }
            TextRange erased_range(m_cursor, { m_cursor.line() + 1, erase_count });
            execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range);
            return;
        }
        return;
    }

    if (event.key() == KeyCode::Key_Backspace) {
        if (!is_editable())
            return;
        if (m_autocomplete_box)
            hide_autocomplete();
        if (has_selection()) {
            delete_selection();
            did_update_selection();
            return;
        }
        if (m_cursor.column() > 0) {
            int erase_count = 1;
            if (event.modifiers() == Mod_Ctrl) {
                auto word_break_pos = document().first_word_break_before(m_cursor, true);
                erase_count = m_cursor.column() - word_break_pos.column();
            } else if (current_line().first_non_whitespace_column() >= m_cursor.column()) {
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

    if (!event.ctrl() && !event.alt() && event.code_point() != 0) {
        TemporaryChange change { m_should_keep_autocomplete_box, true };
        add_code_point(event.code_point());
        return;
    }

    event.ignore();
}

void TextEditor::delete_previous_word()
{
    TextRange to_erase(document().first_word_before(m_cursor, true), m_cursor);
    execute<RemoveTextCommand>(document().text_in_range(to_erase), to_erase);
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
        start = { m_cursor.line() - 1, line(m_cursor.line() - 1).length() };
        end = { m_cursor.line(), line(m_cursor.line()).length() };
    } else {
        start = { m_cursor.line(), 0 };
        end = { m_cursor.line() + 1, 0 };
    }

    TextRange erased_range(start, end);
    execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range);
}

void TextEditor::delete_previous_char()
{
    if (!is_editable())
        return;

    if (has_selection())
        return delete_selection();

    TextRange to_erase({ m_cursor.line(), m_cursor.column() - 1 }, m_cursor);
    if (m_cursor.column() == 0 && m_cursor.line() != 0) {
        size_t prev_line_len = line(m_cursor.line() - 1).length();
        to_erase.set_start({ m_cursor.line() - 1, prev_line_len });
    }

    execute<RemoveTextCommand>(document().text_in_range(to_erase), to_erase);
}

void TextEditor::delete_from_line_start_to_cursor()
{
    TextPosition start(m_cursor.line(), current_line().first_non_whitespace_column());
    TextRange to_erase(start, m_cursor);
    execute<RemoveTextCommand>(document().text_in_range(to_erase), to_erase);
}

void TextEditor::do_delete()
{
    if (!is_editable())
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

void TextEditor::add_code_point(u32 code_point)
{
    if (!is_editable())
        return;

    StringBuilder sb;
    sb.append_code_point(code_point);

    if (should_autocomplete_automatically()) {
        if (sb.string_view().is_whitespace())
            m_autocomplete_timer->stop();
        else
            m_autocomplete_timer->start();
    }
    insert_at_cursor_or_replace_selection(sb.to_string());
};

void TextEditor::reset_cursor_blink()
{
    m_cursor_state = true;
    update_cursor();
    stop_timer();
    start_timer(500);
}

void TextEditor::update_selection(bool is_selecting)
{
    if (is_selecting && !selection().is_valid()) {
        selection().set(cursor(), {});
        did_update_selection();
        update();
        return;
    }
    if (!is_selecting && selection().is_valid()) {
        selection().clear();
        did_update_selection();
        update();
        return;
    }
    if (is_selecting && selection().start().is_valid()) {
        selection().set_end(cursor());
        did_update_selection();
        update();
        return;
    }
}

int TextEditor::content_x_for_position(TextPosition const& position) const
{
    auto& line = this->line(position.line());
    int x_offset = 0;
    switch (m_text_alignment) {
    case Gfx::TextAlignment::CenterLeft:
        for_each_visual_line(position.line(), [&](Gfx::IntRect const&, auto& visual_line_view, size_t start_of_visual_line, bool is_last_visual_line) {
            size_t offset_in_visual_line = position.column() - start_of_visual_line;
            auto before_line_end = is_last_visual_line ? (offset_in_visual_line <= visual_line_view.length()) : (offset_in_visual_line < visual_line_view.length());
            if (position.column() >= start_of_visual_line && before_line_end) {
                if (offset_in_visual_line == 0) {
                    x_offset = 0;
                } else {
                    x_offset = text_width_for_font(visual_line_view.substring_view(0, offset_in_visual_line), font());
                    x_offset += font().glyph_spacing();
                }
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return m_horizontal_content_padding + ((is_single_line() && icon()) ? (icon_size() + icon_padding()) : 0) + x_offset;
    case Gfx::TextAlignment::CenterRight:
        // FIXME
        VERIFY(!is_wrapping_enabled());
        return content_width() - m_horizontal_content_padding - (line.length() * fixed_glyph_width()) + (position.column() * fixed_glyph_width());
    default:
        VERIFY_NOT_REACHED();
    }
}

int TextEditor::text_width_for_font(auto const& text, Gfx::Font const& font) const
{
    if (m_substitution_code_point)
        return font.width(substitution_code_point_view(text.length()));
    else
        return font.width(text);
}

Utf32View TextEditor::substitution_code_point_view(size_t length) const
{
    VERIFY(m_substitution_code_point);
    if (!m_substitution_string_data)
        m_substitution_string_data = make<Vector<u32>>();
    if (!m_substitution_string_data->is_empty())
        VERIFY(m_substitution_string_data->first() == m_substitution_code_point);
    while (m_substitution_string_data->size() < length)
        m_substitution_string_data->append(m_substitution_code_point);
    return Utf32View { m_substitution_string_data->data(), length };
}

Gfx::IntRect TextEditor::content_rect_for_position(TextPosition const& position) const
{
    if (!position.is_valid())
        return {};
    VERIFY(!lines().is_empty());
    VERIFY(position.column() <= (current_line().length() + 1));

    int x = content_x_for_position(position);

    if (is_single_line()) {
        Gfx::IntRect rect { x, 0, 1, line_height() };
        rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return rect;
    }

    Gfx::IntRect rect;
    for_each_visual_line(position.line(), [&](Gfx::IntRect const& visual_line_rect, auto& view, size_t start_of_visual_line, bool is_last_visual_line) {
        auto before_line_end = is_last_visual_line ? ((position.column() - start_of_visual_line) <= view.length()) : ((position.column() - start_of_visual_line) < view.length());
        if (position.column() >= start_of_visual_line && before_line_end) {
            // NOTE: We have to subtract the horizontal padding here since it's part of the visual line rect
            //       *and* included in what we get from content_x_for_position().
            rect = {
                visual_line_rect.x() + x - (m_horizontal_content_padding),
                visual_line_rect.y(),
                m_editing_engine->cursor_width() == CursorWidth::WIDE ? 7 : 1,
                line_height()
            };
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return rect;
}

Gfx::IntRect TextEditor::cursor_content_rect() const
{
    return content_rect_for_position(m_cursor);
}

Gfx::IntRect TextEditor::line_widget_rect(size_t line_index) const
{
    auto rect = line_content_rect(line_index);
    rect.set_x(frame_thickness());
    rect.set_width(frame_inner_rect().width());
    rect.translate_by(0, -(vertical_scrollbar().value()));
    rect.translate_by(0, frame_thickness());
    rect.intersect(frame_inner_rect());
    return rect;
}

void TextEditor::scroll_position_into_view(TextPosition const& position)
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
    if (!m_reflow_deferred)
        scroll_position_into_view(m_cursor);
}

Gfx::IntRect TextEditor::line_content_rect(size_t line_index) const
{
    auto& line = this->line(line_index);
    if (is_single_line()) {
        Gfx::IntRect line_rect = { content_x_for_position({ line_index, 0 }), 0, text_width_for_font(line.view(), font()), font().glyph_height() + 4 };
        line_rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return line_rect;
    }
    if (is_wrapping_enabled())
        return m_line_visual_data[line_index].visual_rect;
    return {
        content_x_for_position({ line_index, 0 }),
        (int)line_index * line_height(),
        text_width_for_font(line.view(), font()),
        line_height()
    };
}

void TextEditor::set_cursor_and_focus_line(size_t line, size_t column)
{
    u_int index_max = line_count() - 1;
    set_cursor(line, column);
    if (line > 1 && line < index_max) {
        int headroom = frame_inner_rect().height() / 3;
        do {
            auto line_data = m_line_visual_data[line];
            headroom -= line_data.visual_rect.height();
            line--;
        } while (line > 0 && headroom > 0);

        Gfx::IntRect rect = { 0, line_content_rect(line).y(),
            1, frame_inner_rect().height() };
        scroll_into_view(rect, false, true);
    }
}

void TextEditor::update_cursor()
{
    update(line_widget_rect(m_cursor.line()));
}

void TextEditor::set_cursor(size_t line, size_t column)
{
    set_cursor({ line, column });
}

void TextEditor::set_cursor(TextPosition const& a_position)
{
    VERIFY(!lines().is_empty());

    TextPosition position = a_position;

    if (position.line() >= line_count())
        position.set_line(line_count() - 1);

    if (position.column() > lines()[position.line()].length())
        position.set_column(lines()[position.line()].length());

    if (m_cursor != position && is_visual_data_up_to_date()) {
        // NOTE: If the old cursor is no longer valid, repaint everything just in case.
        auto old_cursor_line_rect = m_cursor.line() < line_count()
            ? line_widget_rect(m_cursor.line())
            : rect();
        m_cursor = position;
        m_cursor_state = true;
        scroll_cursor_into_view();
        update(old_cursor_line_rect);
        update_cursor();
    } else if (m_cursor != position) {
        m_cursor = position;
        m_cursor_state = true;
    }
    cursor_did_change();
    if (on_cursor_change)
        on_cursor_change();
    if (m_highlighter)
        m_highlighter->cursor_did_change();
}

void TextEditor::focusin_event(FocusEvent& event)
{
    if (event.source() == FocusSource::Keyboard)
        select_all();
    m_cursor_state = true;
    update_cursor();
    stop_timer();
    start_timer(500);
    if (on_focusin)
        on_focusin();
}

void TextEditor::focusout_event(FocusEvent&)
{
    if (is_displayonly() && has_selection())
        m_selection.clear();
    stop_timer();
    if (on_focusout)
        on_focusout();
}

void TextEditor::timer_event(Core::TimerEvent&)
{
    m_cursor_state = !m_cursor_state;
    if (is_focused())
        update_cursor();
}

bool TextEditor::write_to_file(String const& path)
{
    int fd = open(path.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
    if (fd < 0) {
        perror("open");
        return false;
    }

    return write_to_file_and_close(fd);
}

bool TextEditor::write_to_file_and_close(int fd)
{
    ScopeGuard fd_guard = [fd] { close(fd); };

    off_t file_size = 0;
    if (line_count() == 1 && line(0).is_empty()) {
        // Truncate to zero.
    } else {
        // Compute the final file size and ftruncate() to make writing fast.
        // FIXME: Remove this once the kernel is smart enough to do this instead.
        for (size_t i = 0; i < line_count(); ++i)
            file_size += line(i).length();
        file_size += line_count();
    }

    if (ftruncate(fd, file_size) < 0) {
        perror("ftruncate");
        return false;
    }

    if (file_size == 0) {
        // A size 0 file doesn't need a data copy.
    } else {
        for (size_t i = 0; i < line_count(); ++i) {
            auto& line = this->line(i);
            if (line.length()) {
                auto line_as_utf8 = line.to_utf8();
                ssize_t nwritten = write(fd, line_as_utf8.characters(), line_as_utf8.length());
                if (nwritten < 0) {
                    perror("write");
                    return false;
                }
            }
            char ch = '\n';
            ssize_t nwritten = write(fd, &ch, 1);
            if (nwritten != 1) {
                perror("write");
                return false;
            }
        }
    }
    document().set_unmodified();
    return true;
}

String TextEditor::text() const
{
    return document().text();
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

size_t TextEditor::number_of_selected_words() const
{
    if (!has_selection())
        return 0;

    size_t word_count = 0;
    bool in_word = false;
    auto selected_text = this->selected_text();
    for (char c : selected_text) {
        if (in_word && is_ascii_space(c)) {
            in_word = false;
            word_count++;
            continue;
        }
        if (!in_word && !is_ascii_space(c))
            in_word = true;
    }
    if (in_word)
        word_count++;

    return word_count;
}

size_t TextEditor::number_of_words() const
{
    if (document().is_empty())
        return 0;

    size_t word_count = 0;
    bool in_word = false;
    auto text = this->text();
    for (char c : text) {
        if (in_word && is_ascii_space(c)) {
            in_word = false;
            word_count++;
            continue;
        }
        if (!in_word && !is_ascii_space(c))
            in_word = true;
    }
    if (in_word)
        word_count++;

    return word_count;
}

void TextEditor::delete_selection()
{
    auto selection = normalized_selection();
    auto selected = selected_text();
    m_selection.clear();
    execute<RemoveTextCommand>(selected, selection);
    did_update_selection();
    did_change();
    set_cursor(selection.start());
    update();
}

void TextEditor::delete_text_range(TextRange range)
{
    auto normalized_range = range.normalized();
    execute<RemoveTextCommand>(document().text_in_range(normalized_range), normalized_range);
    did_change();
    set_cursor(normalized_range.start());
    update();
}

void TextEditor::insert_at_cursor_or_replace_selection(StringView text)
{
    ReflowDeferrer defer(*this);
    VERIFY(is_editable());
    if (has_selection())
        delete_selection();

    // Check if adding a newline leaves the previous line as just whitespace.
    auto const clear_length = m_cursor.column();
    auto const should_clear_last_line = text == "\n"
        && clear_length > 0
        && current_line().leading_spaces() == clear_length;

    execute<InsertTextCommand>(text, m_cursor);

    if (should_clear_last_line) { // If it does leave just whitespace, clear it.
        auto const original_cursor_position = cursor();
        TextPosition start(original_cursor_position.line() - 1, 0);
        TextPosition end(original_cursor_position.line() - 1, clear_length);
        TextRange erased_range(start, end);
        execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range);
        set_cursor(original_cursor_position);
    }
}

void TextEditor::cut()
{
    if (!is_editable())
        return;
    auto selected_text = this->selected_text();
    dbgln_if(TEXTEDITOR_DEBUG, "Cut: \"{}\"", selected_text);
    Clipboard::the().set_plain_text(selected_text);
    delete_selection();
}

void TextEditor::copy()
{
    auto selected_text = this->selected_text();
    dbgln_if(TEXTEDITOR_DEBUG, "Copy: \"{}\"\n", selected_text);
    Clipboard::the().set_plain_text(selected_text);
}

void TextEditor::paste()
{
    if (!is_editable())
        return;

    auto [data, mime_type, _] = GUI::Clipboard::the().fetch_data_and_type();
    if (!mime_type.starts_with("text/"))
        return;

    if (data.is_empty())
        return;

    dbgln_if(TEXTEDITOR_DEBUG, "Paste: \"{}\"", String::copy(data));

    TemporaryChange change(m_automatic_indentation_enabled, false);
    insert_at_cursor_or_replace_selection(data);
}

void TextEditor::defer_reflow()
{
    ++m_reflow_deferred;
}

void TextEditor::undefer_reflow()
{
    VERIFY(m_reflow_deferred);
    if (!--m_reflow_deferred) {
        if (m_reflow_requested) {
            recompute_all_visual_lines();
            scroll_cursor_into_view();
        }
    }
}

void TextEditor::try_show_autocomplete(UserRequestedAutocomplete user_requested_autocomplete)
{
    force_update_autocomplete([&, user_requested_autocomplete = move(user_requested_autocomplete)] {
        if (user_requested_autocomplete == Yes || m_autocomplete_box->has_suggestions()) {
            auto position = content_rect_for_position(cursor()).translated(0, -visible_content_rect().y()).bottom_right().translated(screen_relative_rect().top_left().translated(ruler_width(), 0).translated(10, 5));
            m_autocomplete_box->show(position);
        }
    });
}

void TextEditor::try_update_autocomplete(Function<void()> callback)
{
    if (m_autocomplete_box && m_autocomplete_box->is_visible())
        force_update_autocomplete(move(callback));
}

void TextEditor::force_update_autocomplete(Function<void()> callback)
{
    if (m_autocomplete_provider) {
        m_autocomplete_provider->provide_completions([&, callback = move(callback)](auto completions) {
            m_autocomplete_box->update_suggestions(move(completions));
            if (callback)
                callback();
        });
    }
}

void TextEditor::hide_autocomplete_if_needed()
{
    if (!m_should_keep_autocomplete_box)
        hide_autocomplete();
}

void TextEditor::hide_autocomplete()
{
    if (m_autocomplete_box) {
        m_autocomplete_box->close();
        if (m_autocomplete_timer)
            m_autocomplete_timer->stop();
    }
}

void TextEditor::enter_event(Core::Event&)
{
    m_automatic_selection_scroll_timer->stop();
}

void TextEditor::leave_event(Core::Event&)
{
    if (m_in_drag_select)
        m_automatic_selection_scroll_timer->start();
}

void TextEditor::did_change(AllowCallback allow_callback)
{
    update_content_size();
    recompute_all_visual_lines();
    hide_autocomplete_if_needed();
    m_needs_rehighlight = true;
    if (!m_has_pending_change_notification) {
        m_has_pending_change_notification = true;
        deferred_invoke([this, allow_callback] {
            m_has_pending_change_notification = false;
            if (on_change && allow_callback == AllowCallback::Yes)
                on_change();
        });
    }
}
void TextEditor::set_mode(const Mode mode)
{
    if (m_mode == mode)
        return;
    m_mode = mode;
    switch (mode) {
    case Editable:
        m_cut_action->set_enabled(has_selection() && !text_is_secret());
        m_paste_action->set_enabled(true);
        set_accepts_emoji_input(true);
        break;
    case DisplayOnly:
    case ReadOnly:
        m_cut_action->set_enabled(false);
        m_paste_action->set_enabled(false);
        set_accepts_emoji_input(false);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    if (!is_displayonly())
        set_override_cursor(Gfx::StandardCursor::IBeam);
    else
        set_override_cursor(Gfx::StandardCursor::None);
}

void TextEditor::did_update_selection()
{
    m_cut_action->set_enabled(is_editable() && has_selection() && !text_is_secret());
    m_copy_action->set_enabled(has_selection() && !text_is_secret());
    if (on_selection_change)
        on_selection_change();
    if (is_wrapping_enabled()) {
        // FIXME: Try to repaint less.
        update();
    }
}

void TextEditor::context_menu_event(ContextMenuEvent& event)
{
    if (is_displayonly())
        return;

    if (!m_context_menu) {
        m_context_menu = Menu::construct();
        m_context_menu->add_action(undo_action());
        m_context_menu->add_action(redo_action());
        m_context_menu->add_separator();
        m_context_menu->add_action(cut_action());
        m_context_menu->add_action(copy_action());
        m_context_menu->add_action(paste_action());
        m_context_menu->add_separator();
        m_context_menu->add_action(select_all_action());
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
    AbstractScrollableWidget::resize_event(event);
    update_content_size();
    recompute_all_visual_lines();
}

void TextEditor::theme_change_event(ThemeChangeEvent& event)
{
    AbstractScrollableWidget::theme_change_event(event);
    m_needs_rehighlight = true;
}

void TextEditor::set_selection(TextRange const& selection)
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
    if (m_reflow_deferred) {
        m_reflow_requested = true;
        return;
    }

    m_reflow_requested = false;

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
    for_each_visual_line(line_index, [&](Gfx::IntRect const&, auto& view, size_t start_of_visual_line, [[maybe_unused]] bool is_last_visual_line) {
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

    if (is_wrapping_enabled()) {
        int line_width_so_far = 0;

        size_t last_whitespace_index = 0;
        size_t line_width_since_last_whitespace = 0;
        auto glyph_spacing = font().glyph_spacing();
        for (size_t i = 0; i < line.length(); ++i) {
            auto code_point = line.code_points()[i];
            if (is_ascii_space(code_point)) {
                last_whitespace_index = i;
                line_width_since_last_whitespace = 0;
            }
            auto glyph_width = font().glyph_or_emoji_width(code_point);
            line_width_since_last_whitespace += glyph_width + glyph_spacing;
            if ((line_width_so_far + glyph_width + glyph_spacing) > available_width) {
                if (m_wrapping_mode == WrappingMode::WrapAtWords && last_whitespace_index != 0) {
                    // Plus 1 to get the first letter of the word.
                    visual_data.visual_line_breaks.append(last_whitespace_index + 1);
                    line_width_so_far = line_width_since_last_whitespace;
                    last_whitespace_index = 0;
                    line_width_since_last_whitespace = 0;
                } else {
                    visual_data.visual_line_breaks.append(i);
                    line_width_so_far = glyph_width + glyph_spacing;
                }
                continue;
            }
            line_width_so_far += glyph_width + glyph_spacing;
        }
    }

    visual_data.visual_line_breaks.append(line.length());

    if (is_wrapping_enabled())
        visual_data.visual_rect = { m_horizontal_content_padding, 0, available_width, static_cast<int>(visual_data.visual_line_breaks.size()) * line_height() };
    else
        visual_data.visual_rect = { m_horizontal_content_padding, 0, text_width_for_font(line.view(), font()), line_height() };
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
        auto visual_line_view = Utf32View(line.code_points() + start_of_line, visual_line_break - start_of_line);
        Gfx::IntRect visual_line_rect {
            visual_data.visual_rect.x(),
            visual_data.visual_rect.y() + ((int)visual_line_index * line_height()),
            text_width_for_font(visual_line_view, font()) + font().glyph_spacing(),
            line_height()
        };
        if (is_right_text_alignment(text_alignment()))
            visual_line_rect.set_right_without_resize(editor_visible_text_rect.right());
        if (is_single_line()) {
            visual_line_rect.center_vertically_within(editor_visible_text_rect);
            if (m_icon)
                visual_line_rect.translate_by(icon_size() + icon_padding(), 0);
        }
        if (callback(visual_line_rect, visual_line_view, start_of_line, visual_line_index == visual_data.visual_line_breaks.size() - 1) == IterationDecision::Break)
            break;
        start_of_line = visual_line_break;
        ++visual_line_index;
    }
}

void TextEditor::set_wrapping_mode(WrappingMode mode)
{
    if (m_wrapping_mode == mode)
        return;

    m_wrapping_mode = mode;
    horizontal_scrollbar().set_visible(m_wrapping_mode == WrappingMode::NoWrap);
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
    AbstractScrollableWidget::did_change_font();
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

void TextEditor::document_did_change(AllowCallback allow_callback)
{
    did_change(allow_callback);
    update();
}

void TextEditor::document_did_update_undo_stack()
{
    auto make_action_text = [](auto prefix, auto suffix) {
        StringBuilder builder;
        builder.append(prefix);
        if (suffix.has_value()) {
            builder.append(' ');
            builder.append(suffix.value());
        }
        return builder.to_string();
    };

    m_undo_action->set_enabled(can_undo() && !text_is_secret());
    m_redo_action->set_enabled(can_redo() && !text_is_secret());

    m_undo_action->set_text(make_action_text("&Undo", document().undo_stack().undo_action_text()));
    m_redo_action->set_text(make_action_text("&Redo", document().undo_stack().redo_action_text()));

    // FIXME: This is currently firing more often than it should.
    //        Ideally we'd only send this out when the undo stack modified state actually changes.
    if (on_modified_change)
        on_modified_change(document().is_modified());
}

void TextEditor::document_did_set_text(AllowCallback allow_callback)
{
    m_line_visual_data.clear();
    for (size_t i = 0; i < m_document->line_count(); ++i)
        m_line_visual_data.append(make<LineVisualData>());
    document_did_change(allow_callback);
}

void TextEditor::document_did_set_cursor(TextPosition const& position)
{
    set_cursor(position);
}

void TextEditor::cursor_did_change()
{
    hide_autocomplete_if_needed();
}

void TextEditor::clipboard_content_did_change(String const& mime_type)
{
    m_paste_action->set_enabled(is_editable() && mime_type.starts_with("text/"));
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
    set_cursor(0, 0);
    if (has_selection())
        m_selection.clear();
    recompute_all_visual_lines();
    update();
    m_document->register_client(*this);
}

void TextEditor::rehighlight_if_needed()
{
    if (!m_needs_rehighlight)
        return;
    if (m_highlighter)
        m_highlighter->rehighlight(palette());
    m_needs_rehighlight = false;
}

Syntax::Highlighter const* TextEditor::syntax_highlighter() const
{
    return m_highlighter.ptr();
}

void TextEditor::set_syntax_highlighter(OwnPtr<Syntax::Highlighter> highlighter)
{
    if (m_highlighter)
        m_highlighter->detach();
    m_highlighter = move(highlighter);
    if (m_highlighter) {
        m_highlighter->attach(*this);
        m_needs_rehighlight = true;
    } else
        document().set_spans({});
}

AutocompleteProvider const* TextEditor::autocomplete_provider() const
{
    return m_autocomplete_provider.ptr();
}

void TextEditor::set_autocomplete_provider(OwnPtr<AutocompleteProvider>&& provider)
{
    if (m_autocomplete_provider)
        m_autocomplete_provider->detach();
    m_autocomplete_provider = move(provider);
    if (m_autocomplete_provider) {
        m_autocomplete_provider->attach(*this);
        if (!m_autocomplete_box)
            m_autocomplete_box = make<AutocompleteBox>(*this);
    }
    if (m_autocomplete_box)
        hide_autocomplete();
}

EditingEngine const* TextEditor::editing_engine() const
{
    return m_editing_engine.ptr();
}

void TextEditor::set_editing_engine(OwnPtr<EditingEngine> editing_engine)
{
    if (m_editing_engine)
        m_editing_engine->detach();
    m_editing_engine = move(editing_engine);

    VERIFY(m_editing_engine);
    m_editing_engine->attach(*this);

    m_cursor_state = true;
    update_cursor();
    stop_timer();
    start_timer(500);
}

int TextEditor::line_height() const
{
    return font().glyph_height() + m_line_spacing;
}

int TextEditor::fixed_glyph_width() const
{
    VERIFY(font().is_fixed_width());
    return font().glyph_width(' ');
}

void TextEditor::set_icon(Gfx::Bitmap const* icon)
{
    if (m_icon == icon)
        return;
    m_icon = icon;
    update();
}

void TextEditor::set_visualize_trailing_whitespace(bool enabled)
{
    if (m_visualize_trailing_whitespace == enabled)
        return;
    m_visualize_trailing_whitespace = enabled;
    update();
}

void TextEditor::set_visualize_leading_whitespace(bool enabled)
{
    if (m_visualize_leading_whitespace == enabled)
        return;
    m_visualize_leading_whitespace = enabled;
    update();
}

void TextEditor::set_should_autocomplete_automatically(bool value)
{
    if (value == should_autocomplete_automatically())
        return;

    if (value) {
        VERIFY(m_autocomplete_provider);
        m_autocomplete_timer = Core::Timer::create_single_shot(m_automatic_autocomplete_delay_ms, [this] {
            if (m_autocomplete_box && !m_autocomplete_box->is_visible())
                try_show_autocomplete(UserRequestedAutocomplete::No);
        });
        return;
    }

    remove_child(*m_autocomplete_timer);
    m_autocomplete_timer = nullptr;
}

void TextEditor::set_substitution_code_point(u32 code_point)
{
    VERIFY(is_unicode(code_point));
    m_substitution_string_data.clear();
    m_substitution_code_point = code_point;
}

int TextEditor::number_of_visible_lines() const
{
    return visible_content_rect().height() / line_height();
}

void TextEditor::set_ruler_visible(bool visible)
{
    if (m_ruler_visible == visible)
        return;
    m_ruler_visible = visible;
    recompute_all_visual_lines();
    update();
}

void TextEditor::set_gutter_visible(bool visible)
{
    if (m_gutter_visible == visible)
        return;
    m_gutter_visible = visible;
    recompute_all_visual_lines();
    update();
}

void TextEditor::set_cursor_line_highlighting(bool highlighted)
{
    if (m_cursor_line_highlighting == highlighted)
        return;
    m_cursor_line_highlighting = highlighted;
    update();
}

void TextEditor::undo()
{
    clear_selection();
    document().undo();
}

void TextEditor::redo()
{
    clear_selection();
    document().redo();
}

void TextEditor::set_text_is_secret(bool text_is_secret)
{
    m_text_is_secret = text_is_secret;
    document_did_update_undo_stack();
    did_update_selection();
}

}
