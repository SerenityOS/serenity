/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
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
#include <LibGUI/EmojiInputDialog.h>
#include <LibGUI/IncrementalSearchBanner.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RegularEditingEngine.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StandardCursor.h>
#include <LibSyntax/Highlighter.h>
#include <LibUnicode/Segmentation.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

REGISTER_WIDGET(GUI, TextEditor)

namespace GUI {

static constexpr StringView folded_region_summary_text = " ..."sv;

TextEditor::TextEditor(Type type)
    : m_type(type)
{
    REGISTER_DEPRECATED_STRING_PROPERTY("text", text, set_text);
    REGISTER_DEPRECATED_STRING_PROPERTY("placeholder", placeholder, set_placeholder);
    REGISTER_BOOL_PROPERTY("gutter", is_gutter_visible, set_gutter_visible);
    REGISTER_BOOL_PROPERTY("ruler", is_ruler_visible, set_ruler_visible);
    REGISTER_ENUM_PROPERTY("mode", mode, set_mode, Mode,
        { Editable, "Editable" },
        { ReadOnly, "ReadOnly" },
        { DisplayOnly, "DisplayOnly" });

    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_or_clear_emoji_input_callback();
    set_override_cursor(Gfx::StandardCursor::IBeam);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    set_document(TextDocument::create());
    if (is_single_line())
        set_visualize_trailing_whitespace(false);
    set_scrollbars_enabled(is_multi_line());
    if (is_multi_line()) {
        set_font(Gfx::FontDatabase::default_fixed_width_font());
        set_wrapping_mode(WrappingMode::WrapAtWords);
        m_search_banner = GUI::IncrementalSearchBanner::try_create(*this).release_value_but_fixme_should_propagate_errors();
        set_banner_widget(m_search_banner);
    }
    vertical_scrollbar().set_step(line_height());
    m_cursor = { 0, 0 };
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
    m_paste_action->set_enabled(is_editable() && Clipboard::the().fetch_mime_type().starts_with("text/"sv));
    if (is_multi_line()) {
        m_go_to_line_or_column_action = Action::create(
            "Go to Line/Column...", { Mod_Ctrl, Key_L }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-to.png"sv).release_value_but_fixme_should_propagate_errors(), [this](auto&) {
                String value;
                if (InputBox::show(window(), value, "Enter the line, or line:column:"sv, "Go to Line/Column"sv) == InputBox::ExecResult::OK) {
                    // If there is a `:` in the string, the format is expected to be `line:column`. E.g: `123:45`
                    if (value.contains(':')) {
                        auto line_and_column_or_error = value.split(':');
                        if (line_and_column_or_error.is_error()) {
                            return;
                        }

                        auto line_and_column = line_and_column_or_error.value();
                        if (line_and_column.size() != 2) {
                            return;
                        }

                        auto line_target = AK::StringUtils::convert_to_uint(line_and_column.at(0));
                        auto column_target = AK::StringUtils::convert_to_uint(line_and_column.at(1));
                        if (line_target.has_value() && column_target.has_value()) {
                            set_cursor_and_focus_line(line_target.value() - 1, column_target.value());
                        }

                        return;
                    }

                    // If there is no `:` in the string, we just treat the integer as the line
                    auto line_target = AK::StringUtils::convert_to_uint(value.bytes_as_string_view());
                    if (line_target.has_value()) {
                        set_cursor_and_focus_line(line_target.value() - 1, 0);
                    }
                }
            },
            this);
    }
    m_select_all_action = CommonActions::make_select_all_action([this](auto&) { select_all(); }, this);
    m_insert_emoji_action = CommonActions::make_insert_emoji_action([&](auto&) { insert_emoji(); }, this);
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
    for (auto& line : m_line_data) {
        content_width = max(line->visual_rect.width(), content_width);
        content_height += line->visual_rect.height();
    }
    content_width += m_horizontal_content_padding * 2;
    if (is_right_text_alignment(m_text_alignment))
        content_width = max(frame_inner_rect().width(), content_width);

    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ fixed_elements_width(), 0 });
}

TextPosition TextEditor::text_position_at_content_position(Gfx::IntPoint content_position) const
{
    auto position = content_position;
    if (is_single_line() && icon())
        position.translate_by(-(icon_size() + icon_padding()), 0);

    size_t line_index = 0;

    if (position.y() >= 0) {
        size_t last_visible_line_index = 0;
        // FIXME: Oh boy is this a slow way of calculating this!
        // NOTE: Offset by 1 in calculations is because we can't do `i >= 0` with an unsigned type.
        for (size_t i = line_count(); i > 0; --i) {
            if (document().line_is_visible(i - 1)) {
                last_visible_line_index = i - 1;
                break;
            }
        }

        for (size_t i = 0; i < line_count(); ++i) {
            if (!document().line_is_visible(i))
                continue;

            auto& rect = m_line_data[i]->visual_rect;
            if (rect.contains_vertically(position.y())) {
                line_index = i;
                break;
            }
            if (position.y() >= rect.bottom())
                line_index = last_visible_line_index;
        }
        line_index = max((size_t)0, min(line_index, last_visible_line_index));
    }

    size_t column_index = 0;
    switch (m_text_alignment) {
    case Gfx::TextAlignment::CenterLeft:
        for_each_visual_line(line_index, [&](Gfx::IntRect const& rect, auto& view, size_t start_of_line, [[maybe_unused]] bool is_last_visual_line) {
            if (is_multi_line() && !rect.contains_vertically(position.y()) && !is_last_visual_line && position.y() >= 0)
                return IterationDecision::Continue;

            column_index = start_of_line;
            int glyph_x = 0;

            if (position.x() <= 0) {
                // We're outside the text on the left side, put cursor at column 0 on this visual line.
                return IterationDecision::Break;
            }

            for (auto it = view.begin(); it != view.end(); ++it, ++column_index) {
                int advance = font().glyph_or_emoji_width(it) + font().glyph_spacing();
                if ((glyph_x + (advance / 2)) >= position.x())
                    break;

                glyph_x += advance;
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

TextPosition TextEditor::text_position_at(Gfx::IntPoint widget_position) const
{
    auto content_position = widget_position;
    content_position.translate_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    content_position.translate_by(-(m_horizontal_content_padding + fixed_elements_width()), 0);
    content_position.translate_by(-frame_thickness(), -frame_thickness());
    content_position.translate_by(0, -height_occupied_by_banner_widget());
    return text_position_at_content_position(content_position);
}

void TextEditor::highlight_all_occurances_of(ByteString const selected_text)
{
    auto search_result = document().find_all(selected_text, false, true);
    if (search_result.size() > 1) {
        Vector<GUI::TextDocumentSpan> spans;
        for (size_t i = 0; i < search_result.size(); ++i) {
            auto& result = search_result[i];
            GUI::TextDocumentSpan span;
            span.range = result;
            span.attributes.color = Color::from_argb(0xff000000);
            span.attributes.background_color = palette().bright_yellow();
            span.attributes.bold = true;
            span.attributes.underline_style = Gfx::TextAttributes::UnderlineStyle::Solid;
            spans.append(move(span));
        }
        document().set_spans(highlight_selected_text_span_collection_index, spans);
        update();
    }
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
    bool got_selection = false;

    if (m_substitution_code_point.has_value()) {
        // NOTE: If we substitute the code points, we don't want double clicking to only select a single word, since
        //       whitespace isn't visible anymore.
        m_selection = document().range_for_entire_line(position.line());
        got_selection = true;
    } else if (document().has_spans()) {
        for (auto& span : document().spans()) {
            if (span.range.contains(position)) {
                m_selection = span.range;
                got_selection = true;
                break;
            }
        }
    }
    if (!got_selection) {
        m_selection.set_start(document().first_word_break_before(position, false));
        m_selection.set_end(document().first_word_break_after(position));
    }

    auto selection = selected_text();
    if (!selection.is_whitespace())
        highlight_all_occurances_of(selection);

    set_cursor(m_selection.end());
    update();
    did_update_selection();
}

void TextEditor::mousedown_event(MouseEvent& event)
{
    using namespace AK::TimeLiterals;

    if (event.button() != MouseButton::Primary) {
        return;
    }
    document().set_spans(highlight_selected_text_span_collection_index, {});

    auto text_position = text_position_at(event.position());
    if (event.modifiers() == 0 && folding_indicator_rect(text_position.line()).contains(event.position())) {
        if (auto folding_region = document().folding_region_starting_on_line(text_position.line()); folding_region.has_value()) {
            folding_region->is_folded = !folding_region->is_folded;
            dbgln_if(TEXTEDITOR_DEBUG, "TextEditor: {} region {}.", folding_region->is_folded ? "Folding"sv : "Unfolding"sv, folding_region->range);

            if (folding_region->is_folded && folding_region->range.contains(cursor())) {
                // Cursor is now within a hidden range, so move it outside.
                set_cursor(folding_region->range.start());
            }

            recompute_all_visual_lines();
            update();
            return;
        }
    }

    if (gutter_content_rect(text_position.line()).contains(event.position())) {
        auto& gutter_indicators = m_line_data[text_position.line()]->gutter_indicators;
        auto indicator_position = 0;
        for (auto i = 0u; i < m_gutter_indicators.size(); ++i) {
            if ((gutter_indicators & (1 << i)) == 0)
                continue;

            if (gutter_indicator_rect(text_position.line(), indicator_position).contains(event.position())) {
                auto& indicator_data = m_gutter_indicators[i];
                if (indicator_data.on_click)
                    indicator_data.on_click(text_position.line(), event.modifiers());
                return;
            }
            indicator_position++;
        }

        // We didn't click on an indicator
        if (on_gutter_click)
            on_gutter_click(text_position.line(), event.modifiers());
        return;
    }

    if (on_mousedown)
        on_mousedown();

    if (is_displayonly())
        return;

    if (m_triple_click_timer.is_valid() && m_triple_click_timer.elapsed_time() < 250_ms) {
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

    set_cursor_to_text_position(event.position());

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
    if (m_in_drag_select) {
        auto constrained = event.position().constrained(widget_inner_rect());
        set_cursor_to_text_position(constrained);
        m_selection.set_end(m_cursor);
        did_update_selection();
        update();
        return;
    }

    if (m_ruler_visible && ruler_rect_in_inner_coordinates().contains(event.position())) {
        set_override_cursor(Gfx::StandardCursor::None);
    } else if (m_ruler_visible && folding_indicator_rect_in_inner_coordinates().contains(event.position())) {
        auto text_position = text_position_at(event.position());
        if (document().folding_region_starting_on_line(text_position.line()).has_value()
            && folding_indicator_rect(text_position.line()).contains(event.position())) {
            set_override_cursor(Gfx::StandardCursor::Hand);
        } else {
            set_override_cursor(Gfx::StandardCursor::None);
        }
    } else if (m_gutter_visible && gutter_rect_in_inner_coordinates().contains(event.position())) {
        set_override_cursor(Gfx::StandardCursor::None);
    } else {
        set_editing_cursor();
    }
}

void TextEditor::select_current_line()
{
    m_selection = document().range_for_entire_line(m_cursor.line());
    set_cursor(m_selection.end());
    update();
    did_update_selection();
}

void TextEditor::automatic_scrolling_timer_did_fire()
{
    if (!m_in_drag_select) {
        set_automatic_scrolling_timer_active(false);
        return;
    }
    set_cursor_to_text_position(m_last_mousemove_position);
    m_selection.set_end(m_cursor);
    did_update_selection();
    update();
}

int TextEditor::folding_indicator_width() const
{
    return document().has_folding_regions() ? line_height() : 0;
}

int TextEditor::ruler_width() const
{
    if (!m_ruler_visible)
        return 0;
    int line_count_digits = static_cast<int>(log10(line_count())) + 1;
    auto padding = 5 + (font().is_fixed_width() ? 1 : (line_count_digits - (line_count() < 10 ? -1 : 0)));
    auto widest_numeral = font().bold_variant().glyph_width('4');
    return line_count() < 10 ? (line_count_digits + 1) * widest_numeral + padding : line_count_digits * widest_numeral + padding;
}

int TextEditor::gutter_width() const
{
    if (!m_gutter_visible)
        return 0;
    return line_height() * (m_most_gutter_indicators_displayed_on_one_line + 1);
}

Gfx::IntRect TextEditor::gutter_content_rect(size_t line_index) const
{
    if (!m_gutter_visible)
        return {};

    return {
        0,
        line_content_rect(line_index).y() - vertical_scrollbar().value(),
        gutter_width(),
        line_content_rect(line_index).height()
    };
}

Gfx::IntRect TextEditor::ruler_content_rect(size_t line_index) const
{
    if (!m_ruler_visible)
        return {};

    return {
        gutter_width(),
        line_content_rect(line_index).y() - vertical_scrollbar().value(),
        ruler_width(),
        line_content_rect(line_index).height()
    };
}

Gfx::IntRect TextEditor::folding_indicator_rect(size_t line_index) const
{
    if (!m_ruler_visible || !document().has_folding_regions())
        return {};

    return {
        gutter_width() + ruler_width(),
        line_content_rect(line_index).y() - vertical_scrollbar().value(),
        folding_indicator_width(),
        line_content_rect(line_index).height()
    };
}

Gfx::IntRect TextEditor::gutter_indicator_rect(size_t line_number, int indicator_position) const
{
    auto gutter_rect = gutter_content_rect(line_number);
    auto indicator_size = gutter_rect.height();
    return Gfx::IntRect {
        gutter_rect.right() - 1 - static_cast<int>(lroundf(indicator_size * (indicator_position + 1.5f))),
        gutter_rect.top(),
        indicator_size,
        indicator_size
    };
}

Gfx::IntRect TextEditor::gutter_rect_in_inner_coordinates() const
{
    return { 0, 0, gutter_width(), widget_inner_rect().height() };
}

Gfx::IntRect TextEditor::ruler_rect_in_inner_coordinates() const
{
    return { gutter_width(), 0, ruler_width(), widget_inner_rect().height() };
}

Gfx::IntRect TextEditor::folding_indicator_rect_in_inner_coordinates() const
{
    return { gutter_width() + ruler_width(), 0, folding_indicator_width(), widget_inner_rect().height() };
}

Gfx::IntRect TextEditor::visible_text_rect_in_inner_coordinates() const
{
    return {
        m_horizontal_content_padding + fixed_elements_width(),
        0,
        frame_inner_rect().width() - (m_horizontal_content_padding * 2) - width_occupied_by_vertical_scrollbar() - fixed_elements_width(),
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

    Gfx::TextAttributes unspanned_text_attributes;
    if (is_displayonly() && is_focused()) {
        unspanned_text_attributes.color = palette().color(is_enabled() ? Gfx::ColorRole::SelectionText : Gfx::ColorRole::DisabledText);
    } else {
        unspanned_text_attributes.color = palette().color(is_enabled() ? foreground_role() : Gfx::ColorRole::DisabledText);
    }

    auto& folded_region_summary_font = font().bold_variant();
    Gfx::TextAttributes folded_region_summary_attributes { palette().color(Gfx::ColorRole::SyntaxComment) };

    // NOTE: This lambda and TextEditor::text_width_for_font() are used to substitute all glyphs with m_substitution_code_point if necessary.
    //       Painter::draw_text() and Gfx::Font::width() should not be called directly, but using this lambda and TextEditor::text_width_for_font().
    auto draw_text = [&](auto const& rect, auto const& raw_text, Gfx::Font const& font, Gfx::TextAlignment alignment, Gfx::TextAttributes attributes, bool substitute = true) {
        if (m_substitution_code_point.has_value() && substitute) {
            painter.draw_text(rect, substitution_code_point_view(raw_text.length()), font, alignment, attributes.color);
        } else {
            painter.draw_text(rect, raw_text, font, alignment, attributes.color);
        }
        if (attributes.underline_style.has_value()) {
            auto bottom_left = [&]() {
                auto point = rect.bottom_left();

                if constexpr (IsSame<RemoveCVReference<decltype(rect)>, Gfx::IntRect>)
                    return point;
                else
                    return point.template to_type<int>();
            };

            auto bottom_right = [&]() {
                auto point = rect.bottom_right().translated(-1, 0);

                if constexpr (IsSame<RemoveCVReference<decltype(rect)>, Gfx::IntRect>)
                    return point;
                else
                    return point.template to_type<int>();
            };

            if (attributes.underline_style == Gfx::TextAttributes::UnderlineStyle::Solid)
                painter.draw_line(bottom_left(), bottom_right(), attributes.underline_color.value_or(attributes.color));
            if (attributes.underline_style == Gfx::TextAttributes::UnderlineStyle::Wavy)
                painter.draw_triangle_wave(bottom_left(), bottom_right(), attributes.underline_color.value_or(attributes.color), 2);
        }
    };

    if (is_displayonly() && is_focused()) {
        Gfx::IntRect display_rect {
            widget_inner_rect().x() + 1,
            widget_inner_rect().y() + 1,
            widget_inner_rect().width() - 2,
            widget_inner_rect().height() - 2
        };
        painter.fill_rect(display_rect, palette().selection());
    }

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(0, height_occupied_by_banner_widget());

    if (!is_multi_line() && m_icon) {
        Gfx::IntRect icon_rect { icon_padding(), 1, icon_size(), icon_size() };
        painter.draw_scaled_bitmap(icon_rect, *m_icon, m_icon->rect());
    }

    if (m_gutter_visible) {
        auto gutter_rect = gutter_rect_in_inner_coordinates();
        painter.fill_rect(gutter_rect, palette().gutter());
        if (!m_ruler_visible)
            painter.draw_line(gutter_rect.top_right().translated(-1, 0), gutter_rect.bottom_right().translated(-1), palette().gutter_border());
    }

    if (m_ruler_visible) {
        auto ruler_rect = ruler_rect_in_inner_coordinates().inflated(0, folding_indicator_width(), 0, 0);
        painter.fill_rect(ruler_rect, palette().ruler());
        painter.draw_line(ruler_rect.top_right().translated(-1, 0), ruler_rect.bottom_right().translated(-1), palette().ruler_border());

        // Paint +/- buttons for folding regions
        for (auto const& folding_region : document().folding_regions()) {
            auto start_line = folding_region.range.start().line();
            if (!document().line_is_visible(start_line))
                continue;
            auto fold_indicator_rect = folding_indicator_rect(start_line).shrunken(4, 4);
            fold_indicator_rect.set_height(fold_indicator_rect.width());
            painter.draw_rect(fold_indicator_rect, palette().ruler_inactive_text());
            auto fold_symbol = folding_region.is_folded ? "+"sv : "-"sv;
            painter.draw_text(fold_indicator_rect, fold_symbol, font(), Gfx::TextAlignment::Center, palette().ruler_inactive_text());
        }
    }

    size_t first_visible_line = text_position_at(event.rect().top_left()).line();
    size_t last_visible_line = text_position_at(event.rect().bottom_right().translated(-1)).line();

    auto selection = normalized_selection();
    bool has_selection = selection.is_valid();

    if (m_ruler_visible) {
        for (size_t i = first_visible_line; i <= last_visible_line; ++i) {
            if (!document().line_is_visible(i))
                continue;

            bool is_current_line = i == m_cursor.line();
            auto ruler_line_rect = ruler_content_rect(i);
            // NOTE: Shrink the rectangle to be only on the first visual line.
            auto const line_height = font().preferred_line_height();
            if (ruler_line_rect.height() > line_height)
                ruler_line_rect.set_height(line_height);
            // NOTE: Use Painter::draw_text() directly here, as we want to always draw the line numbers in clear text.
            size_t const line_number = is_relative_line_number() && !is_current_line ? max(i, m_cursor.line()) - min(i, m_cursor.line()) : i + 1;
            painter.draw_text(
                ruler_line_rect.shrunken(2, 0),
                ByteString::number(line_number),
                is_current_line ? font().bold_variant() : font(),
                Gfx::TextAlignment::CenterRight,
                is_current_line ? palette().ruler_active_text() : palette().ruler_inactive_text());
        }
    }

    // Draw gutter indicators
    if (m_gutter_visible) {
        for (size_t line_index = first_visible_line; line_index <= last_visible_line; ++line_index) {
            if (!document().line_is_visible(line_index))
                continue;

            auto& gutter_indicators = m_line_data[line_index]->gutter_indicators;
            if (gutter_indicators == 0)
                continue;

            auto indicator_position = 0;
            for (auto i = 0u; i < m_gutter_indicators.size(); ++i) {
                if ((gutter_indicators & (1 << i)) == 0)
                    continue;

                auto rect = gutter_indicator_rect(line_index, indicator_position);
                m_gutter_indicators[i].draw_indicator(painter, rect, line_index);
                indicator_position++;
            }
        }
    }

    auto horizontal_scrollbar_value = horizontal_scrollbar().value();
    painter.translate(-horizontal_scrollbar_value, -vertical_scrollbar().value());
    if (m_icon && horizontal_scrollbar_value > 0)
        painter.translate(min(icon_size() + icon_padding(), horizontal_scrollbar_value), 0);

    auto gutter_ruler_width = fixed_elements_width();
    painter.translate(gutter_ruler_width, 0);
    Gfx::IntRect text_clip_rect { 0, 0, widget_inner_rect().width() - gutter_ruler_width, widget_inner_rect().height() };
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
                last_visual_line_with_selection = m_line_data[line_index]->visual_lines.size();
            else
                last_visual_line_with_selection = visual_line_containing(line_index, selection.end().column());
        }

        size_t selection_start_column_within_line = selection.start().line() == line_index ? selection.start().column() : 0;
        size_t selection_end_column_within_line = selection.end().line() == line_index ? selection.end().column() : line.length();

        size_t visual_line_index = 0;
        size_t last_start_of_visual_line = 0;
        Optional<size_t> multiline_trailing_space_offset {};
        for_each_visual_line(line_index, [&](Gfx::IntRect const& visual_line_rect, auto& visual_line_text, size_t start_of_visual_line, [[maybe_unused]] bool is_last_visual_line) {
            ScopeGuard update_last_start_of_visual_line { [&]() { last_start_of_visual_line = start_of_visual_line; } };

            if (is_focused() && is_multi_line() && line_index == m_cursor.line() && is_cursor_line_highlighted()) {
                Gfx::IntRect visible_content_line_rect {
                    visible_content_rect().x(),
                    visual_line_rect.y(),
                    widget_inner_rect().width() - gutter_ruler_width,
                    line_height()
                };
                painter.fill_rect(visible_content_line_rect, widget_background_color.darkened(0.9f));
            }
            if constexpr (TEXTEDITOR_DEBUG)
                painter.draw_rect(visual_line_rect, Color::Cyan);

            if (!placeholder().is_empty() && document().is_empty() && line_index == 0) {
                auto line_rect = visual_line_rect;
                line_rect.set_width(text_width_for_font(placeholder(), font()));
                draw_text(line_rect, placeholder(), font(), m_text_alignment, { palette().color(Gfx::ColorRole::PlaceholderText) }, false);
            } else if (!document().has_spans()) {
                // Fast-path for plain text
                draw_text(visual_line_rect, visual_line_text, font(), m_text_alignment, unspanned_text_attributes);
            } else {
                size_t next_column = 0;
                Gfx::FloatRect span_rect = { visual_line_rect.location(), { 0, line_height() } };

                auto draw_text_helper = [&](size_t start, size_t end, Gfx::Font const& font, Gfx::TextAttributes text_attributes) {
                    size_t length = end - start;
                    if (length == 0)
                        return;
                    auto text = visual_line_text.substring_view(start, length);
                    span_rect.set_width(font.width(text) + font.glyph_spacing());
                    if (text_attributes.background_color.has_value()) {
                        painter.fill_rect(span_rect.to_type<int>(), text_attributes.background_color.value());
                    }
                    draw_text(span_rect, text, font, m_text_alignment, text_attributes);
                    span_rect.translate_by(span_rect.width(), 0);
                };

                bool started_new_folded_region = false;
                while (span_index < document().spans().size()) {
                    auto& span = document().spans()[span_index];
                    // Skip spans that have ended before this point.
                    // That is, for spans that are for lines inside a folded region.
                    if ((span.range.end().line() < line_index)
                        || (span.range.end().line() == line_index && span.range.end().column() <= start_of_visual_line)) {
                        ++span_index;
                        continue;
                    }
                    if (span.range.start().line() > line_index
                        || (span.range.start().line() == line_index && span.range.start().column() >= start_of_visual_line + visual_line_text.length())) {
                        // no more spans in this line, moving on
                        break;
                    }
                    size_t span_start;
                    if (span.range.start().line() < line_index || span.range.start().column() < start_of_visual_line) {
                        span_start = 0;
                    } else {
                        span_start = span.range.start().column() - start_of_visual_line;
                    }
                    size_t span_end;
                    bool span_consumed;
                    if (span.range.end().line() > line_index || span.range.end().column() > start_of_visual_line + visual_line_text.length()) {
                        span_end = visual_line_text.length();
                        span_consumed = false;
                    } else {
                        span_end = span.range.end().column() - start_of_visual_line;
                        span_consumed = true;
                    }

                    if (span_start != next_column) {
                        // draw unspanned text between spans
                        draw_text_helper(next_column, span_start, font(), unspanned_text_attributes);
                    }
                    auto& span_font = span.attributes.bold ? font().bold_variant() : font();
                    draw_text_helper(span_start, span_end, span_font, span.attributes);
                    next_column = span_end;
                    if (!span_consumed) {
                        // continue with same span on next line
                        break;
                    } else {
                        ++span_index;
                    }
                }
                // draw unspanned text after last span
                if (!started_new_folded_region && next_column < visual_line_text.length()) {
                    draw_text_helper(next_column, visual_line_text.length(), font(), unspanned_text_attributes);
                }

                // Paint "..." at the end of the line if it starts a folded region.
                // FIXME: This doesn't wrap.
                if (is_last_visual_line) {
                    if (auto folded_region = document().folding_region_starting_on_line(line_index); folded_region.has_value() && folded_region->is_folded) {
                        span_rect.set_width(folded_region_summary_font.width(folded_region_summary_text));
                        draw_text(span_rect, folded_region_summary_text, folded_region_summary_font, m_text_alignment, folded_region_summary_attributes);
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
                    physical_column -= multiline_trailing_space_offset.value_or(0);

                    size_t visual_column = physical_column > start_of_visual_line ? (physical_column - start_of_visual_line) : 0;

                    Gfx::IntRect whitespace_rect {
                        content_x_for_position({ line_index, physical_column }),
                        visual_line_rect.y(),
                        text_width_for_font(visual_line_text.substring_view(visual_column, visual_line_text.length() - visual_column), font()),
                        visual_line_rect.height()
                    };
                    painter.fill_rect_with_dither_pattern(whitespace_rect, Color(), Color(255, 192, 192));

                    if (!multiline_trailing_space_offset.has_value())
                        multiline_trailing_space_offset = physical_column - last_start_of_visual_line;
                }
            }
            if (m_visualize_leading_whitespace && line.leading_spaces() > 0) {
                size_t physical_column = line.leading_spaces();
                if (start_of_visual_line < physical_column) {
                    size_t end_of_leading_whitespace = min(physical_column - start_of_visual_line, visual_line_text.length());
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
                size_t const start_of_selection_within_visual_line = (size_t)max(0, (int)selection_start_column_within_line - (int)start_of_visual_line);
                size_t const end_of_selection_within_visual_line = min(selection_end_column_within_line - start_of_visual_line, visual_line_text.length());

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
                        : visual_line_rect.right();

                    Gfx::IntRect selection_rect {
                        selection_left,
                        visual_line_rect.y(),
                        selection_right - selection_left,
                        visual_line_rect.height()
                    };

                    Color background_color = is_focused() ? palette().selection() : palette().inactive_selection();
                    Color text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();

                    painter.fill_rect(selection_rect, background_color);

                    if (visual_line_text.code_points()) {
                        Utf32View visual_selected_text {
                            visual_line_text.code_points() + start_of_selection_within_visual_line,
                            end_of_selection_within_visual_line - start_of_selection_within_visual_line
                        };

                        draw_text(selection_rect, visual_selected_text, font(), Gfx::TextAlignment::CenterLeft, { text_color });
                    }
                }
            }

            ++visual_line_index;
            return IterationDecision::Continue;
        });
    }

    if (is_enabled() && is_focused() && !focus_preempted() && m_cursor_state && !is_displayonly())
        painter.fill_rect(cursor_content_rect(), palette().text_cursor());
}

Optional<UISize> TextEditor::calculated_min_size() const
{
    if (is_multi_line())
        return AbstractScrollableWidget::calculated_min_size();
    auto constexpr cursor_padding = 4;
    auto m = content_margins();
    auto width = max(40, m.horizontal_total());
    auto height = max(22, line_height() + m.vertical_total() + cursor_padding);
    return UISize { width, height };
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

void TextEditor::insert_emoji()
{
    if (!on_emoji_input || window()->blocks_emoji_input())
        return;

    auto emoji_input_dialog = EmojiInputDialog::construct(window());
    if (emoji_input_dialog->exec() != EmojiInputDialog::ExecResult::OK)
        return;

    auto emoji_code_point = emoji_input_dialog->selected_emoji_text();
    insert_at_cursor_or_replace_selection(emoji_code_point);
}

void TextEditor::set_or_clear_emoji_input_callback()
{
    switch (m_mode) {
    case Editable:
        on_emoji_input = [this](auto emoji) {
            insert_at_cursor_or_replace_selection(emoji);
        };
        break;

    case DisplayOnly:
    case ReadOnly:
        on_emoji_input = {};
        break;

    default:
        VERIFY_NOT_REACHED();
    }
}

void TextEditor::keydown_event(KeyEvent& event)
{
    if (!is_editable() && event.key() == KeyCode::Key_Tab)
        return AbstractScrollableWidget::keydown_event(event);

    if (m_autocomplete_box && m_autocomplete_box->is_visible() && (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Tab)) {
        TemporaryChange change { m_should_keep_autocomplete_box, true };
        if (m_autocomplete_box->apply_suggestion() == CodeComprehension::AutocompleteResultEntry::HideAutocompleteAfterApplying::Yes)
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

        if (event.modifiers() == KeyModifier::Mod_Ctrl && event.key() == KeyCode::Key_Return) {
            if (on_ctrl_return_pressed)
                on_ctrl_return_pressed();
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

    if (is_multi_line() && !event.alt() && event.ctrl() && event.key() == KeyCode::Key_Return) {
        if (!is_editable())
            return;

        size_t indent_length = current_line().leading_spaces();
        ByteString indent = current_line().to_utf8().substring(0, indent_length);
        auto insert_pos = event.shift() ? InsertLineCommand::InsertPosition::Above : InsertLineCommand::InsertPosition::Below;
        execute<InsertLineCommand>(m_cursor, move(indent), insert_pos);
        return;
    }

    if (is_multi_line() && !event.shift() && !event.alt() && event.ctrl() && event.key() == KeyCode::Key_Space) {
        if (m_autocomplete_provider) {
            try_show_autocomplete(UserRequestedAutocomplete::Yes);
            update_autocomplete.disarm();
            return;
        }
    }

    if (is_multi_line() && !event.shift() && !event.alt() && event.ctrl() && event.key() == KeyCode::Key_F) {
        m_search_banner->show();
        return;
    }

    if (m_editing_engine->on_key(event))
        return;

    if (event.key() == KeyCode::Key_Escape) {
        if (on_escape_pressed)
            on_escape_pressed();
        else
            event.ignore();
        return;
    }

    if (event.modifiers() == Mod_Shift && event.key() == KeyCode::Key_Delete) {
        if (m_autocomplete_box)
            hide_autocomplete();
        return;
    }

    if (event.key() == KeyCode::Key_Tab) {
        if (has_selection()) {
            if (event.modifiers() == Mod_Shift) {
                unindent_selection();
                return;
            }
            if (is_indenting_selection()) {
                indent_selection();
                return;
            }
        } else {
            if (event.modifiers() == Mod_Shift) {
                unindent_line();
                return;
            }
        }
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
            } else {
                auto grapheme_break_position = document().get_next_grapheme_cluster_boundary(m_cursor);
                erase_count = grapheme_break_position - m_cursor.column();
            }
            TextRange erased_range(m_cursor, { m_cursor.line(), m_cursor.column() + erase_count });
            execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range, erased_range.start());
            return;
        }
        if (m_cursor.column() == current_line().length() && m_cursor.line() != line_count() - 1) {
            // Delete at end of line; merge with next line
            size_t erase_count = 0;
            if (event.modifiers() == Mod_Ctrl) {
                erase_count = document().first_word_break_after({ m_cursor.line() + 1, 0 }).column();
            }
            TextRange erased_range(m_cursor, { m_cursor.line() + 1, erase_count });
            execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range, erased_range.end());
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
            } else {
                auto grapheme_break_position = document().get_previous_grapheme_cluster_boundary(m_cursor);
                erase_count = m_cursor.column() - grapheme_break_position;
            }

            // Backspace within line
            TextRange erased_range({ m_cursor.line(), m_cursor.column() - erase_count }, m_cursor);
            auto erased_text = document().text_in_range(erased_range);
            execute<RemoveTextCommand>(erased_text, erased_range, erased_range.end());
            return;
        }
        if (m_cursor.column() == 0 && m_cursor.line() != 0) {
            // Backspace at column 0; merge with previous line
            size_t previous_length = line(m_cursor.line() - 1).length();
            TextRange erased_range({ m_cursor.line() - 1, previous_length }, m_cursor);
            execute<RemoveTextCommand>("\n", erased_range, erased_range.end());
            return;
        }
        return;
    }

    // AltGr is emulated as Ctrl+Alt; if Ctrl is set check if it's not for AltGr
    if ((!event.ctrl() || event.altgr()) && !event.alt() && event.code_point() != 0) {
        TemporaryChange change { m_should_keep_autocomplete_box, true };
        add_code_point(event.code_point());
        return;
    }

    if (event.ctrl() && event.key() == KeyCode::Key_Slash) {
        if (m_highlighter != nullptr) {
            auto prefix = m_highlighter->comment_prefix().value_or(""sv);
            auto suffix = m_highlighter->comment_suffix().value_or(""sv);
            auto range = has_selection() ? selection().normalized() : TextRange { { m_cursor.line(), m_cursor.column() }, { m_cursor.line(), m_cursor.column() } };

            auto is_already_commented = true;
            for (size_t i = range.start().line(); i <= range.end().line(); i++) {
                auto text = m_document->line(i).to_utf8().trim_whitespace();
                if (!(text.starts_with(prefix) && text.ends_with(suffix))) {
                    is_already_commented = false;
                    break;
                }
            }

            if (is_already_commented)
                execute<UncommentSelection>(prefix, suffix, range);
            else
                execute<CommentSelection>(prefix, suffix, range);

            return;
        }
    }

    event.ignore();
}

bool TextEditor::is_indenting_selection()
{
    auto const selection_start = m_selection.start() > m_selection.end() ? m_selection.end() : m_selection.start();
    auto const selection_end = m_selection.end() > m_selection.start() ? m_selection.end() : m_selection.start();
    auto const whole_line_selected = selection_end.column() - selection_start.column() >= current_line().length() - current_line().first_non_whitespace_column();
    auto const on_same_line = selection_start.line() == selection_end.line();

    if (has_selection() && (whole_line_selected || !on_same_line)) {
        return true;
    }

    return false;
}

void TextEditor::indent_selection()
{
    auto const selection_start = m_selection.start() > m_selection.end() ? m_selection.end() : m_selection.start();
    auto const selection_end = m_selection.end() > m_selection.start() ? m_selection.end() : m_selection.start();

    if (is_indenting_selection()) {
        execute<IndentSelection>(m_soft_tab_width, TextRange(selection_start, selection_end));
        m_selection.set_start({ selection_start.line(), selection_start.column() + m_soft_tab_width });
        m_selection.set_end({ selection_end.line(), selection_end.column() + m_soft_tab_width });
        set_cursor({ m_cursor.line(), m_cursor.column() + m_soft_tab_width });
    }
}

void TextEditor::unindent_selection()
{
    auto const selection_start = m_selection.start() > m_selection.end() ? m_selection.end() : m_selection.start();
    auto const selection_end = m_selection.end() > m_selection.start() ? m_selection.end() : m_selection.start();

    if (current_line().first_non_whitespace_column() != 0) {
        if (current_line().first_non_whitespace_column() > m_soft_tab_width && selection_start.column() != 0) {
            m_selection.set_start({ selection_start.line(), selection_start.column() - m_soft_tab_width });
            m_selection.set_end({ selection_end.line(), selection_end.column() - m_soft_tab_width });
        } else if (selection_start.column() != 0) {
            m_selection.set_start({ selection_start.line(), selection_start.column() - current_line().leading_spaces() });
            m_selection.set_end({ selection_end.line(), selection_end.column() - current_line().leading_spaces() });
        }
        execute<UnindentSelection>(m_soft_tab_width, TextRange(selection_start, selection_end));
    }
}

void TextEditor::unindent_line()
{
    if (current_line().first_non_whitespace_column() != 0) {
        auto const unindent_size = current_line().leading_spaces() < m_soft_tab_width ? current_line().leading_spaces() : m_soft_tab_width;
        auto const temp_column = m_cursor.column();

        execute<UnindentSelection>(unindent_size, TextRange({ m_cursor.line(), 0 }, { m_cursor.line(), line(m_cursor.line()).length() }));

        set_cursor({ m_cursor.line(), temp_column <= unindent_size ? 0 : temp_column - unindent_size });
    }
}

void TextEditor::delete_previous_word()
{
    TextRange to_erase(document().first_word_before(m_cursor, true), m_cursor);
    execute<RemoveTextCommand>(document().text_in_range(to_erase), to_erase, to_erase.end());
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
    execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range, m_cursor);
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

    execute<RemoveTextCommand>(document().text_in_range(to_erase), to_erase, to_erase.end());
}

void TextEditor::delete_from_line_start_to_cursor()
{
    TextPosition start(m_cursor.line(), current_line().first_non_whitespace_column());
    TextRange to_erase(start, m_cursor);
    execute<RemoveTextCommand>(document().text_in_range(to_erase), to_erase, m_cursor);
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
        execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range, erased_range.start());
        return;
    }
    if (m_cursor.column() == current_line().length() && m_cursor.line() != line_count() - 1) {
        // Delete at end of line; merge with next line
        TextRange erased_range(m_cursor, { m_cursor.line() + 1, 0 });
        execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range, erased_range.start());
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
    insert_at_cursor_or_replace_selection(sb.to_byte_string());
}

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
    if (m_substitution_code_point.has_value())
        return font.width(substitution_code_point_view(text.length()));
    else
        return font.width(text);
}

Utf32View TextEditor::substitution_code_point_view(size_t length) const
{
    VERIFY(m_substitution_code_point.has_value());
    if (!m_substitution_string_data)
        m_substitution_string_data = make<Vector<u32>>();
    if (!m_substitution_string_data->is_empty())
        VERIFY(m_substitution_string_data->first() == m_substitution_code_point);
    while (m_substitution_string_data->size() < length)
        m_substitution_string_data->append(m_substitution_code_point.value());
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
    rect.translate_by(0, height_occupied_by_banner_widget());
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
        Gfx::IntRect line_rect = { content_x_for_position({ line_index, 0 }), 0, text_width_for_font(line.view(), font()), font().pixel_size_rounded_up() + 4 };
        line_rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return line_rect;
    }
    return m_line_data[line_index]->visual_rect;
}

void TextEditor::set_cursor_and_focus_line(size_t line, size_t column)
{
    u_int index_max = line_count() - 1;
    set_cursor(line, column);
    if (line > 1 && line < index_max) {
        int headroom = frame_inner_rect().height() / 3;
        do {
            auto const& line_data = m_line_data[line];
            headroom -= line_data->visual_rect.height();
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

    if (position.column() > lines()[position.line()]->length())
        position.set_column(lines()[position.line()]->length());

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
    if (m_relative_line_number)
        update();
}

void TextEditor::set_cursor_to_text_position(Gfx::IntPoint position)
{
    auto visual_position = text_position_at(position);
    size_t physical_column = 0;

    auto const& line = document().line(visual_position.line());
    size_t boundary_index = 0;

    Unicode::for_each_grapheme_segmentation_boundary(line.view(), [&](auto boundary) {
        physical_column = boundary;

        if (boundary_index++ >= visual_position.column())
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });

    set_cursor({ visual_position.line(), physical_column });
}

void TextEditor::set_cursor_to_end_of_visual_line()
{
    for_each_visual_line(m_cursor.line(), [&](auto const&, auto& view, size_t start_of_visual_line, auto) {
        if (m_cursor.column() < start_of_visual_line)
            return IterationDecision::Continue;
        if ((m_cursor.column() - start_of_visual_line) >= view.length())
            return IterationDecision::Continue;

        set_cursor(m_cursor.line(), start_of_visual_line + view.length());
        return IterationDecision::Break;
    });
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

ErrorOr<void> TextEditor::write_to_file(StringView path)
{
    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Write | Core::File::OpenMode::Truncate));
    TRY(write_to_file(*file));
    return {};
}

ErrorOr<void> TextEditor::write_to_file(Core::File& file)
{
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

    TRY(file.truncate(file_size));

    if (file_size == 0) {
        // A size 0 file doesn't need a data copy.
    } else {
        for (size_t i = 0; i < line_count(); ++i) {
            TRY(file.write_until_depleted(line(i).to_utf8()));
            TRY(file.write_until_depleted("\n"sv));
        }
    }
    document().set_unmodified();
    return {};
}

ByteString TextEditor::text() const
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

ByteString TextEditor::selected_text() const
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
    execute<RemoveTextCommand>(selected, selection, selection.end());
    did_update_selection();
    did_change();
    set_cursor(selection.start());
    update();
}

void TextEditor::delete_text_range(TextRange range)
{
    auto normalized_range = range.normalized();
    execute<RemoveTextCommand>(document().text_in_range(normalized_range), normalized_range, normalized_range.end());
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
        execute<RemoveTextCommand>(document().text_in_range(erased_range), erased_range, erased_range.end());
        set_cursor(original_cursor_position);
    }
}

void TextEditor::replace_all_text_without_resetting_undo_stack(StringView text, StringView action_text)
{
    execute<ReplaceAllTextCommand>(text, action_text);
    did_change();
    update();
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
    if (!mime_type.starts_with("text/"sv))
        return;

    if (data.is_empty())
        return;

    dbgln_if(TEXTEDITOR_DEBUG, "Paste: \"{}\"", ByteString::copy(data));

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
            auto position = content_rect_for_position(cursor()).translated(0, -visible_content_rect().y()).bottom_right().translated(screen_relative_rect().top_left().translated(ruler_width(), 0).translated(9, 4));
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
    set_automatic_scrolling_timer_active(false);
}

void TextEditor::leave_event(Core::Event&)
{
    if (m_in_drag_select)
        set_automatic_scrolling_timer_active(true);
}

void TextEditor::did_change(AllowCallback allow_callback)
{
    update_content_size();
    recompute_all_visual_lines();
    hide_autocomplete_if_needed();
    m_needs_rehighlight = true;
    if (on_change && allow_callback == AllowCallback::Yes)
        on_change();
}
void TextEditor::set_mode(Mode const mode)
{
    if (m_mode == mode)
        return;
    m_mode = mode;
    switch (mode) {
    case Editable:
        m_cut_action->set_enabled(has_selection() && !text_is_secret());
        m_paste_action->set_enabled(true);
        m_insert_emoji_action->set_enabled(true);
        break;
    case DisplayOnly:
    case ReadOnly:
        m_cut_action->set_enabled(false);
        m_paste_action->set_enabled(false);
        m_insert_emoji_action->set_enabled(false);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    set_or_clear_emoji_input_callback();
    set_editing_cursor();
}

void TextEditor::set_editing_cursor()
{
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

    m_insert_emoji_action->set_enabled(on_emoji_input && !window()->blocks_emoji_input());

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
        m_context_menu->add_action(insert_emoji_action());
        if (is_multi_line()) {
            m_context_menu->add_separator();
            m_context_menu->add_action(go_to_line_or_column_action());
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
    auto folded_regions = document().currently_folded_regions();
    auto folded_region_iterator = folded_regions.begin();
    for (size_t line_index = 0; line_index < line_count(); ++line_index) {
        recompute_visual_lines(line_index, folded_region_iterator);
        m_line_data[line_index]->visual_rect.set_y(y_offset);
        y_offset += m_line_data[line_index]->visual_rect.height();
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

void TextEditor::recompute_visual_lines(size_t line_index, Vector<TextDocumentFoldingRegion const&>::Iterator& folded_region_iterator)
{
    auto const& line = document().line(line_index);
    size_t line_width_so_far = 0;

    auto& visual_data = m_line_data[line_index];
    visual_data->visual_lines.clear_with_capacity();

    auto available_width = visible_text_rect_in_inner_coordinates().width();
    auto glyph_spacing = font().glyph_spacing();

    while (!folded_region_iterator.is_end() && folded_region_iterator->range.end() < TextPosition { line_index, 0 })
        ++folded_region_iterator;
    bool line_is_visible = true;
    if (!folded_region_iterator.is_end()) {
        if (folded_region_iterator->range.start().line() < line_index) {
            if (folded_region_iterator->range.end().line() > line_index) {
                line_is_visible = false;
            } else if (folded_region_iterator->range.end().line() == line_index) {
                ++folded_region_iterator;
            }
        }
    }

    auto wrap_visual_lines_anywhere = [&]() {
        size_t start_of_visual_line = 0;
        for (auto it = line.view().begin(); it != line.view().end(); ++it) {
            auto it_before_computing_glyph_width = it;
            auto glyph_width = font().glyph_or_emoji_width(it);

            if (line_width_so_far + glyph_width + glyph_spacing > available_width) {
                auto start_of_next_visual_line = line.view().iterator_offset(it_before_computing_glyph_width);
                visual_data->visual_lines.append(line.view().substring_view(start_of_visual_line, start_of_next_visual_line - start_of_visual_line));
                line_width_so_far = 0;
                start_of_visual_line = start_of_next_visual_line;
            }

            line_width_so_far += glyph_width + glyph_spacing;
        }

        visual_data->visual_lines.append(line.view().substring_view(start_of_visual_line, line.view().length() - start_of_visual_line));
    };

    auto wrap_visual_lines_at_words = [&]() {
        size_t last_boundary = 0;
        size_t start_of_visual_line = 0;

        Unicode::for_each_word_segmentation_boundary(line.view(), [&](auto boundary) {
            if (boundary == 0)
                return IterationDecision::Continue;

            auto word = line.view().substring_view(last_boundary, boundary - last_boundary);
            auto word_width = font().width(word);

            if (line_width_so_far + word_width + glyph_spacing > available_width) {
                visual_data->visual_lines.append(line.view().substring_view(start_of_visual_line, last_boundary - start_of_visual_line));
                line_width_so_far = 0;
                start_of_visual_line = last_boundary;
            }

            line_width_so_far += word_width + glyph_spacing;
            last_boundary = boundary;

            return IterationDecision::Continue;
        });

        visual_data->visual_lines.append(line.view().substring_view(start_of_visual_line, line.view().length() - start_of_visual_line));
    };

    if (line_is_visible) {
        switch (wrapping_mode()) {
        case WrappingMode::NoWrap:
            visual_data->visual_lines.append(line.view());
            break;
        case WrappingMode::WrapAnywhere:
            wrap_visual_lines_anywhere();
            break;
        case WrappingMode::WrapAtWords:
            wrap_visual_lines_at_words();
            break;
        }
    }

    auto line_width = is_wrapping_enabled() ? available_width : text_width_for_font(line.view(), font());
    visual_data->visual_rect = { m_horizontal_content_padding, 0, line_width, static_cast<int>(visual_data->visual_lines.size()) * line_height() };
}

template<typename Callback>
void TextEditor::for_each_visual_line(size_t line_index, Callback callback) const
{
    auto editor_visible_text_rect = visible_text_rect_in_inner_coordinates();
    size_t visual_line_index = 0;

    auto& line = document().line(line_index);
    auto& visual_data = m_line_data[line_index];

    for (auto visual_line_view : visual_data->visual_lines) {
        Gfx::IntRect visual_line_rect {
            visual_data->visual_rect.x(),
            visual_data->visual_rect.y() + ((int)visual_line_index * line_height()),
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
        size_t start_of_line = visual_line_view.code_points() - line.code_points();
        if (callback(visual_line_rect, visual_line_view, start_of_line, visual_line_index == visual_data->visual_lines.size() - 1) == IterationDecision::Break)
            break;
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
    m_line_data.append(make<LineData>());
    recompute_all_visual_lines();
    update();
}

void TextEditor::document_did_remove_line(size_t line_index)
{
    m_line_data.remove(line_index);
    recompute_all_visual_lines();
    update();
}

void TextEditor::document_did_remove_all_lines()
{
    m_line_data.clear();
    recompute_all_visual_lines();
    update();
}

void TextEditor::document_did_insert_line(size_t line_index)
{
    m_line_data.insert(line_index, make<LineData>());
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
        return builder.to_byte_string();
    };

    m_undo_action->set_enabled(can_undo() && !text_is_secret());
    m_redo_action->set_enabled(can_redo() && !text_is_secret());

    m_undo_action->set_text(make_action_text("&Undo"sv, document().undo_stack().undo_action_text()));
    m_redo_action->set_text(make_action_text("&Redo"sv, document().undo_stack().redo_action_text()));

    // FIXME: This is currently firing more often than it should.
    //        Ideally we'd only send this out when the undo stack modified state actually changes.
    if (on_modified_change)
        on_modified_change(document().is_modified());
}

void TextEditor::populate_line_data()
{
    m_line_data.clear();
    m_line_data.ensure_capacity(m_document->line_count());

    for (size_t i = 0; i < m_document->line_count(); ++i) {
        m_line_data.unchecked_append(make<LineData>());
    }
}

void TextEditor::document_did_set_text(AllowCallback allow_callback)
{
    populate_line_data();
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

void TextEditor::clipboard_content_did_change(ByteString const& mime_type)
{
    m_paste_action->set_enabled(is_editable() && mime_type.starts_with("text/"sv));
}

void TextEditor::set_document(TextDocument& document)
{
    if (m_document.ptr() == &document)
        return;
    if (m_document)
        m_document->unregister_client(*this);
    m_document = document;
    populate_line_data();
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
    force_rehighlight();
}

void TextEditor::force_rehighlight()
{
    if (m_highlighter)
        m_highlighter->rehighlight(palette());
    m_needs_rehighlight = false;
}

Syntax::Highlighter const* TextEditor::syntax_highlighter() const
{
    return m_highlighter.ptr();
}

Syntax::Highlighter* TextEditor::syntax_highlighter()
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
        document().set_spans(Syntax::HighlighterClient::span_collection_index, {});
    if (on_highlighter_change)
        on_highlighter_change();
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
    return static_cast<int>(ceilf(font().preferred_line_height()));
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

void TextEditor::set_substitution_code_point(Optional<u32> code_point)
{
    if (code_point.has_value())
        VERIFY(is_unicode(code_point.value()));
    m_substitution_string_data.clear();
    m_substitution_code_point = move(code_point);
}

int TextEditor::number_of_visible_lines() const
{
    return visible_content_rect().height() / line_height();
}

void TextEditor::set_relative_line_number(bool relative)
{
    if (m_relative_line_number == relative)
        return;
    m_relative_line_number = relative;
    recompute_all_visual_lines();
    update();
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

TextRange TextEditor::find_text(StringView needle, SearchDirection direction, GUI::TextDocument::SearchShouldWrap should_wrap, bool use_regex, bool match_case)
{
    GUI::TextRange range {};
    if (direction == SearchDirection::Forward) {
        range = document().find_next(needle,
            m_search_result_index.has_value() ? m_search_results[*m_search_result_index].end() : GUI::TextPosition {},
            should_wrap, use_regex, match_case);
    } else {
        range = document().find_previous(needle,
            m_search_result_index.has_value() ? m_search_results[*m_search_result_index].start() : GUI::TextPosition {},
            should_wrap, use_regex, match_case);
    }

    if (!range.is_valid()) {
        reset_search_results();
        return {};
    }

    auto all_results = document().find_all(needle, use_regex, match_case);
    on_search_results(range, all_results);
    return range;
}

void TextEditor::reset_search_results()
{
    m_search_result_index.clear();
    m_search_results.clear();
    document().set_spans(search_results_span_collection_index, {});
    update();
}

void TextEditor::on_search_results(GUI::TextRange current, Vector<GUI::TextRange> all_results)
{
    m_search_result_index.clear();
    m_search_results.clear();

    set_cursor(current.start());
    if (auto it = all_results.find(current); it->is_valid())
        m_search_result_index = it.index();
    m_search_results = move(all_results);

    Vector<GUI::TextDocumentSpan> spans;
    for (size_t i = 0; i < m_search_results.size(); ++i) {
        auto& result = m_search_results[i];
        GUI::TextDocumentSpan span;
        span.range = result;
        span.attributes.background_color = palette().hover_highlight();
        span.attributes.color = Color::from_argb(0xff000000); // So text without spans from a highlighter will have color
        if (i == m_search_result_index) {
            span.attributes.bold = true;
            span.attributes.underline_style = Gfx::TextAttributes::UnderlineStyle::Solid;
        }
        spans.append(move(span));
    }
    document().set_spans(search_results_span_collection_index, move(spans));
    update();
}

void TextEditor::highlighter_did_set_folding_regions(Vector<GUI::TextDocumentFoldingRegion> folding_regions)
{
    document().set_folding_regions(move(folding_regions));
    recompute_all_visual_lines();
}

ErrorOr<TextEditor::GutterIndicatorID> TextEditor::register_gutter_indicator(PaintGutterIndicator draw_indicator, OnGutterIndicatorClick on_click)
{
    // We use a u32 to store a line's active gutter indicators, so that's the limit of how many we can have.
    VERIFY(m_gutter_indicators.size() < 32);

    GutterIndicatorID id = m_gutter_indicators.size();
    TRY(m_gutter_indicators.try_empend(move(draw_indicator), move(on_click)));
    return id;
}

void TextEditor::add_gutter_indicator(GutterIndicatorID id, size_t line)
{
    auto& line_indicators = m_line_data[line]->gutter_indicators;
    if (line_indicators & (1 << id.value()))
        return;
    line_indicators |= (1 << id.value());

    // Ensure the gutter is at least wide enough to display all the indicators on this line.
    if (m_most_gutter_indicators_displayed_on_one_line < m_gutter_indicators.size()) {
        unsigned indicators_on_line = popcount(line_indicators);
        if (indicators_on_line > m_most_gutter_indicators_displayed_on_one_line) {
            m_most_gutter_indicators_displayed_on_one_line = indicators_on_line;
            update();
        }
    }

    update(gutter_content_rect(line));
}

void TextEditor::remove_gutter_indicator(GutterIndicatorID id, size_t line)
{
    m_line_data[line]->gutter_indicators &= ~(1 << id.value());
    update(gutter_content_rect(line));
}

void TextEditor::clear_gutter_indicators(GutterIndicatorID id)
{
    for (auto line = 0u; line < line_count(); ++line)
        remove_gutter_indicator(id, line);
    update();
}

}
