#include <AK/StringBuilder.h>
#include <Kernel/KeyCode.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GClipboard.h>
#include <LibGUI/GFontDatabase.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWindow.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

GTextEditor::GTextEditor(Type type, GWidget* parent)
    : GScrollableWidget(parent)
    , m_type(type)
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);
    set_scrollbars_enabled(is_multi_line());
    set_font(GFontDatabase::the().get_by_name("Csilla Thin"));
    // FIXME: Recompute vertical scrollbar step size on font change.
    vertical_scrollbar().set_step(line_height());
    m_lines.append(make<Line>(*this));
    m_cursor = { 0, 0 };
    create_actions();
}

GTextEditor::~GTextEditor()
{
}

void GTextEditor::create_actions()
{
    m_undo_action = GCommonActions::make_undo_action([&](auto&) {
        // FIXME: Undo
    });
    m_redo_action = GCommonActions::make_redo_action([&](auto&) {
        // FIXME: Undo
    });
    m_cut_action = GCommonActions::make_cut_action([&](auto&) { cut(); }, this);
    m_copy_action = GCommonActions::make_copy_action([&](auto&) { copy(); }, this);
    m_paste_action = GCommonActions::make_paste_action([&](auto&) { paste(); }, this);
    m_delete_action = GCommonActions::make_delete_action([&](auto&) { do_delete(); }, this);
}

void GTextEditor::set_text(const StringView& text)
{
    if (is_single_line() && text.length() == m_lines[0].length() && !memcmp(text.characters_without_null_termination(), m_lines[0].characters(), text.length()))
        return;

    m_selection.clear();
    m_lines.clear();
    int start_of_current_line = 0;

    auto add_line = [&](int current_position) {
        int line_length = current_position - start_of_current_line;
        auto line = make<Line>(*this);
        if (line_length)
            line->set_text(text.substring_view(start_of_current_line, current_position - start_of_current_line));
        m_lines.append(move(line));
        start_of_current_line = current_position + 1;
    };
    int i = 0;
    for (i = 0; i < text.length(); ++i) {
        if (text[i] == '\n')
            add_line(i);
    }
    add_line(i);
    update_content_size();
    recompute_all_visual_lines();
    if (is_single_line())
        set_cursor(0, m_lines[0].length());
    else
        set_cursor(0, 0);
    did_update_selection();
    update();
}

void GTextEditor::update_content_size()
{
    int content_width = 0;
    int content_height = 0;
    for (auto& line : m_lines) {
        content_width = max(line.m_visual_rect.width(), content_width);
        content_height += line.m_visual_rect.height();
    }
    content_width += m_horizontal_content_padding * 2;
    if (is_right_text_alignment(m_text_alignment))
        content_width = max(frame_inner_rect().width(), content_width);

    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ ruler_width(), 0 });
}

GTextPosition GTextEditor::text_position_at(const Point& a_position) const
{
    auto position = a_position;
    position.move_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    position.move_by(-(m_horizontal_content_padding + ruler_width()), 0);
    position.move_by(-frame_thickness(), -frame_thickness());

    int line_index = -1;

    if (is_line_wrapping_enabled()) {
        for (int i = 0; i < m_lines.size(); ++i) {
            auto& rect = m_lines[i].m_visual_rect;
            if (position.y() >= rect.top() && position.y() <= rect.bottom()) {
                line_index = i;
                break;
            } else if (position.y() > rect.bottom())
                line_index = m_lines.size() - 1;
        }
    } else {
        line_index = position.y() / line_height();
    }

    line_index = max(0, min(line_index, line_count() - 1));

    auto& line = m_lines[line_index];

    int column_index;
    switch (m_text_alignment) {
    case TextAlignment::CenterLeft:
        column_index = (position.x() + glyph_width() / 2) / glyph_width();
        if (is_line_wrapping_enabled()) {
            line.for_each_visual_line([&](const Rect& rect, const StringView&, int start_of_line) {
                if (rect.contains_vertically(position.y())) {
                    column_index += start_of_line;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
        }
        break;
    case TextAlignment::CenterRight:
        // FIXME: Support right-aligned line wrapping, I guess.
        ASSERT(!is_line_wrapping_enabled());
        column_index = (position.x() - content_x_for_position({ line_index, 0 }) + glyph_width() / 2) / glyph_width();
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    column_index = max(0, min(column_index, m_lines[line_index].length()));
    return { line_index, column_index };
}

void GTextEditor::doubleclick_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;

    m_triple_click_timer.start();
    m_in_drag_select = false;

    auto start = text_position_at(event.position());
    auto end = start;
    auto& line = m_lines[start.line()];
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

    m_selection.set(start, end);
    set_cursor(end);
    update();
    did_update_selection();
}

void GTextEditor::mousedown_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left) {
        return;
    }

    if (m_triple_click_timer.is_valid() && m_triple_click_timer.elapsed() < 250) {
        m_triple_click_timer = CElapsedTimer();

        GTextPosition start;
        GTextPosition end;

        if (is_multi_line()) {
            // select *current* line
            start = GTextPosition(m_cursor.line(), 0);
            end = GTextPosition(m_cursor.line(), m_lines[m_cursor.line()].length());
        } else {
            // select *whole* line
            start = GTextPosition(0, 0);
            end = GTextPosition(line_count() - 1, m_lines[line_count() - 1].length());
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

void GTextEditor::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        if (m_in_drag_select) {
            m_in_drag_select = false;
        }
        return;
    }
}

void GTextEditor::mousemove_event(GMouseEvent& event)
{
    if (m_in_drag_select) {
        set_cursor(text_position_at(event.position()));
        m_selection.set_end(m_cursor);
        did_update_selection();
        update();
        return;
    }
}

int GTextEditor::ruler_width() const
{
    if (!m_ruler_visible)
        return 0;
    // FIXME: Resize based on needed space.
    return 5 * font().glyph_width('x') + 4;
}

Rect GTextEditor::ruler_content_rect(int line_index) const
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

Rect GTextEditor::ruler_rect_in_inner_coordinates() const
{
    return { 0, 0, ruler_width(), height() - height_occupied_by_horizontal_scrollbar() };
}

Rect GTextEditor::visible_text_rect_in_inner_coordinates() const
{
    return {
        m_horizontal_content_padding + (m_ruler_visible ? (ruler_rect_in_inner_coordinates().right() + 1) : 0),
        0,
        frame_inner_rect().width() - (m_horizontal_content_padding * 2) - width_occupied_by_vertical_scrollbar() - ruler_width(),
        frame_inner_rect().height() - height_occupied_by_horizontal_scrollbar()
    };
}

void GTextEditor::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::White);

    painter.translate(frame_thickness(), frame_thickness());

    auto ruler_rect = ruler_rect_in_inner_coordinates();

    if (m_ruler_visible) {
        painter.fill_rect(ruler_rect, Color::WarmGray);
        painter.draw_line(ruler_rect.top_right(), ruler_rect.bottom_right(), Color::DarkGray);
    }

    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    if (m_ruler_visible)
        painter.translate(ruler_width(), 0);

    int first_visible_line = text_position_at(event.rect().top_left()).line();
    int last_visible_line = text_position_at(event.rect().bottom_right()).line();

    auto selection = normalized_selection();
    bool has_selection = selection.is_valid();

    if (m_ruler_visible) {
        for (int i = first_visible_line; i <= last_visible_line; ++i) {
            bool is_current_line = i == m_cursor.line();
            auto ruler_line_rect = ruler_content_rect(i);
            painter.draw_text(
                ruler_line_rect.shrunken(2, 0).translated(0, m_line_spacing / 2),
                String::number(i + 1),
                is_current_line ? Font::default_bold_font() : font(),
                TextAlignment::TopRight,
                is_current_line ? Color::DarkGray : Color::MidGray);
        }
    }

    Rect text_clip_rect {
        (m_ruler_visible ? (ruler_rect_in_inner_coordinates().right() + frame_thickness() + 1) : frame_thickness()),
        frame_thickness(),
        width() - width_occupied_by_vertical_scrollbar() - ruler_width(),
        height() - height_occupied_by_horizontal_scrollbar()
    };
    painter.add_clip_rect(text_clip_rect);

    for (int line_index = first_visible_line; line_index <= last_visible_line; ++line_index) {
        auto& line = m_lines[line_index];

        bool physical_line_has_selection = has_selection && line_index >= selection.start().line() && line_index <= selection.end().line();
        int first_visual_line_with_selection = -1;
        int last_visual_line_with_selection = -1;
        if (physical_line_has_selection) {
            if (selection.start().line() < line_index)
                first_visual_line_with_selection = 0;
            else
                first_visual_line_with_selection = line.visual_line_containing(selection.start().column());

            if (selection.end().line() > line_index)
                last_visual_line_with_selection = line.m_visual_line_breaks.size();
            else
                last_visual_line_with_selection = line.visual_line_containing(selection.end().column());
        }

        int selection_start_column_within_line = selection.start().line() == line_index ? selection.start().column() : 0;
        int selection_end_column_within_line = selection.end().line() == line_index ? selection.end().column() : line.length();

        int visual_line_index = 0;
        line.for_each_visual_line([&](const Rect& visual_line_rect, const StringView& visual_line_text, int start_of_visual_line) {
            // FIXME: Make sure we always fill the entire line.
            //line_rect.set_width(exposed_width);
            if (is_multi_line() && line_index == m_cursor.line())
                painter.fill_rect(visual_line_rect, Color(230, 230, 230));
            painter.draw_text(visual_line_rect, visual_line_text, m_text_alignment, Color::Black);
            bool physical_line_has_selection = has_selection && line_index >= selection.start().line() && line_index <= selection.end().line();
            if (physical_line_has_selection) {

                bool current_visual_line_has_selection = (line_index != selection.start().line() && line_index != selection.end().line())
                    || (visual_line_index >= first_visual_line_with_selection && visual_line_index <= last_visual_line_with_selection);
                if (current_visual_line_has_selection) {
                    bool selection_begins_on_current_visual_line = visual_line_index == first_visual_line_with_selection;
                    bool selection_ends_on_current_visual_line = visual_line_index == last_visual_line_with_selection;

                    int selection_left = selection_begins_on_current_visual_line
                        ? content_x_for_position({ line_index, selection_start_column_within_line })
                        : m_horizontal_content_padding;

                    int selection_right = selection_ends_on_current_visual_line
                        ? content_x_for_position({ line_index, selection_end_column_within_line })
                        : visual_line_rect.right() + 1;

                    Rect selection_rect {
                        selection_left,
                        visual_line_rect.y(),
                        selection_right - selection_left,
                        visual_line_rect.height()
                    };

                    painter.fill_rect(selection_rect, Color::from_rgb(0x955233));

                    int start_of_selection_within_visual_line = max(0, selection_start_column_within_line - start_of_visual_line);
                    int end_of_selection_within_visual_line = selection_end_column_within_line - start_of_visual_line;

                    StringView visual_selected_text {
                        visual_line_text.characters_without_null_termination() + start_of_selection_within_visual_line,
                        end_of_selection_within_visual_line - start_of_selection_within_visual_line
                    };

                    painter.draw_text(selection_rect, visual_selected_text, TextAlignment::CenterLeft, Color::White);
                }
            }
            ++visual_line_index;
            return IterationDecision::Continue;
        });
    }

    if (is_focused() && m_cursor_state)
        painter.fill_rect(cursor_content_rect(), Color::Red);
}

void GTextEditor::toggle_selection_if_needed_for_event(const GKeyEvent& event)
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

void GTextEditor::select_all()
{
    GTextPosition start_of_document { 0, 0 };
    GTextPosition end_of_document { line_count() - 1, m_lines[line_count() - 1].length() };
    m_selection.set(start_of_document, end_of_document);
    did_update_selection();
    set_cursor(end_of_document);
    update();
}

void GTextEditor::keydown_event(GKeyEvent& event)
{
    if (is_single_line() && event.key() == KeyCode::Key_Tab)
        return GWidget::keydown_event(event);

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
    if (event.key() == KeyCode::Key_Up) {
        if (m_cursor.line() > 0) {
            int new_line = m_cursor.line() - 1;
            int new_column = min(m_cursor.column(), m_lines[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        if (m_cursor.line() < (m_lines.size() - 1)) {
            int new_line = m_cursor.line() + 1;
            int new_column = min(m_cursor.column(), m_lines[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        if (m_cursor.line() > 0) {
            int new_line = max(0, m_cursor.line() - visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), m_lines[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        if (m_cursor.line() < (m_lines.size() - 1)) {
            int new_line = min(line_count() - 1, m_cursor.line() + visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), m_lines[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_Left) {
        if (m_cursor.column() > 0) {
            int new_column = m_cursor.column() - 1;
            toggle_selection_if_needed_for_event(event);
            set_cursor(m_cursor.line(), new_column);
            if (m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        } else if (m_cursor.line() > 0) {
            int new_line = m_cursor.line() - 1;
            int new_column = m_lines[new_line].length();
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
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
        if (m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_Home) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(m_cursor.line(), 0);
        if (m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_End) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(m_cursor.line(), current_line().length());
        if (m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_Home) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(0, 0);
        if (m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(line_count() - 1, m_lines[line_count() - 1].length());
        if (m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.modifiers() == Mod_Ctrl && event.key() == KeyCode::Key_A) {
        select_all();
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
            // Backspace within line
            current_line().remove(m_cursor.column() - 1);
            update_content_size();
            set_cursor(m_cursor.line(), m_cursor.column() - 1);
            did_change();
            return;
        }
        if (m_cursor.column() == 0 && m_cursor.line() != 0) {
            // Backspace at column 0; merge with previous line
            auto& previous_line = m_lines[m_cursor.line() - 1];
            int previous_length = previous_line.length();
            previous_line.append(current_line().characters(), current_line().length());
            m_lines.remove(m_cursor.line());
            update_content_size();
            update();
            set_cursor(m_cursor.line() - 1, previous_length);
            did_change();
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

    if (!is_readonly() && !event.ctrl() && !event.alt() && !event.text().is_empty())
        insert_at_cursor_or_replace_selection(event.text());
}

void GTextEditor::delete_current_line()
{
    if (has_selection())
        return delete_selection();

    m_lines.remove(m_cursor.line());
    if (m_lines.is_empty())
        m_lines.append(make<Line>(*this));

    update_content_size();
    update();
}

void GTextEditor::do_delete()
{
    if (is_readonly())
        return;

    if (has_selection())
        return delete_selection();

    if (m_cursor.column() < current_line().length()) {
        // Delete within line
        current_line().remove(m_cursor.column());
        did_change();
        update_cursor();
        return;
    }
    if (m_cursor.column() == current_line().length() && m_cursor.line() != line_count() - 1) {
        // Delete at end of line; merge with next line
        auto& next_line = m_lines[m_cursor.line() + 1];
        int previous_length = current_line().length();
        current_line().append(next_line.characters(), next_line.length());
        m_lines.remove(m_cursor.line() + 1);
        update();
        did_change();
        set_cursor(m_cursor.line(), previous_length);
        return;
    }
}

void GTextEditor::insert_at_cursor(const StringView& text)
{
    // FIXME: This should obviously not be implemented this way.
    for (int i = 0; i < text.length(); ++i) {
        insert_at_cursor(text[i]);
    }
}

void GTextEditor::insert_at_cursor(char ch)
{
    bool at_head = m_cursor.column() == 0;
    bool at_tail = m_cursor.column() == current_line().length();
    if (ch == '\n') {
        if (at_tail || at_head) {
            String new_line_contents;
            if (m_automatic_indentation_enabled && at_tail) {
                int leading_spaces = 0;
                auto& old_line = m_lines[m_cursor.line()];
                for (int i = 0; i < old_line.length(); ++i) {
                    if (old_line.characters()[i] == ' ')
                        ++leading_spaces;
                    else
                        break;
                }
                if (leading_spaces)
                    new_line_contents = String::repeated(' ', leading_spaces);
            }
            m_lines.insert(m_cursor.line() + (at_tail ? 1 : 0), make<Line>(*this, new_line_contents));
            update();
            did_change();
            set_cursor(m_cursor.line() + 1, m_lines[m_cursor.line() + 1].length());
            return;
        }
        auto new_line = make<Line>(*this);
        new_line->append(current_line().characters() + m_cursor.column(), current_line().length() - m_cursor.column());
        current_line().truncate(m_cursor.column());
        m_lines.insert(m_cursor.line() + 1, move(new_line));
        update();
        did_change();
        set_cursor(m_cursor.line() + 1, 0);
        return;
    }
    if (ch == '\t') {
        int next_soft_tab_stop = ((m_cursor.column() + m_soft_tab_width) / m_soft_tab_width) * m_soft_tab_width;
        int spaces_to_insert = next_soft_tab_stop - m_cursor.column();
        for (int i = 0; i < spaces_to_insert; ++i) {
            current_line().insert(m_cursor.column(), ' ');
        }
        did_change();
        set_cursor(m_cursor.line(), next_soft_tab_stop);
        return;
    }
    current_line().insert(m_cursor.column(), ch);
    did_change();
    set_cursor(m_cursor.line(), m_cursor.column() + 1);
}

int GTextEditor::content_x_for_position(const GTextPosition& position) const
{
    auto& line = m_lines[position.line()];
    int x_offset = -1;
    switch (m_text_alignment) {
    case TextAlignment::CenterLeft:
        line.for_each_visual_line([&](const Rect&, const StringView& view, int start_of_visual_line) {
            if (position.column() >= start_of_visual_line && ((position.column() - start_of_visual_line) <= view.length())) {
                x_offset = (position.column() - start_of_visual_line) * glyph_width();
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return m_horizontal_content_padding + x_offset;
    case TextAlignment::CenterRight:
        // FIXME
        ASSERT(!is_line_wrapping_enabled());
        return content_width() - m_horizontal_content_padding - (line.length() * glyph_width()) + (position.column() * glyph_width());
    default:
        ASSERT_NOT_REACHED();
    }
}

Rect GTextEditor::content_rect_for_position(const GTextPosition& position) const
{
    if (!position.is_valid())
        return {};
    ASSERT(!m_lines.is_empty());
    ASSERT(position.column() <= (current_line().length() + 1));

    int x = content_x_for_position(position);

    if (is_single_line()) {
        Rect rect { x, 0, 1, font().glyph_height() + 2 };
        rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return rect;
    }

    auto& line = m_lines[position.line()];
    Rect rect;
    line.for_each_visual_line([&](const Rect& visual_line_rect, const StringView& view, int start_of_visual_line) {
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

Rect GTextEditor::cursor_content_rect() const
{
    return content_rect_for_position(m_cursor);
}

Rect GTextEditor::line_widget_rect(int line_index) const
{
    auto rect = line_content_rect(line_index);
    rect.set_x(frame_thickness());
    rect.set_width(frame_inner_rect().width());
    rect.move_by(0, -(vertical_scrollbar().value()));
    rect.move_by(0, frame_thickness());
    rect.intersect(frame_inner_rect());
    return rect;
}

void GTextEditor::scroll_position_into_view(const GTextPosition& position)
{
    auto rect = content_rect_for_position(position);
    if (position.column() == 0)
        rect.set_x(content_x_for_position({ position.line(), 0 }) - 2);
    else if (position.column() == m_lines[position.line()].length())
        rect.set_x(content_x_for_position({ position.line(), m_lines[position.line()].length() }) + 2);
    scroll_into_view(rect, true, true);
}

void GTextEditor::scroll_cursor_into_view()
{
    scroll_position_into_view(m_cursor);
}

Rect GTextEditor::line_content_rect(int line_index) const
{
    auto& line = m_lines[line_index];
    if (is_single_line()) {
        Rect line_rect = { content_x_for_position({ line_index, 0 }), 0, line.length() * glyph_width(), font().glyph_height() + 2 };
        line_rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return line_rect;
    }
    if (is_line_wrapping_enabled())
        return line.m_visual_rect;
    return {
        content_x_for_position({ line_index, 0 }),
        line_index * line_height(),
        line.length() * glyph_width(),
        line_height()
    };
}

void GTextEditor::update_cursor()
{
    update(line_widget_rect(m_cursor.line()));
}

void GTextEditor::set_cursor(int line, int column)
{
    set_cursor({ line, column });
}

void GTextEditor::set_cursor(const GTextPosition& position)
{
    ASSERT(!m_lines.is_empty());
    ASSERT(position.line() < m_lines.size());
    ASSERT(position.column() <= m_lines[position.line()].length());
    if (m_cursor != position) {
        // NOTE: If the old cursor is no longer valid, repaint everything just in case.
        auto old_cursor_line_rect = m_cursor.line() < m_lines.size()
            ? line_widget_rect(m_cursor.line())
            : rect();
        m_cursor = position;
        m_cursor_state = true;
        scroll_cursor_into_view();
        update(old_cursor_line_rect);
        update_cursor();
    }
    if (on_cursor_change)
        on_cursor_change();
}

void GTextEditor::focusin_event(CEvent&)
{
    update_cursor();
    start_timer(500);
}

void GTextEditor::focusout_event(CEvent&)
{
    stop_timer();
}

void GTextEditor::timer_event(CTimerEvent&)
{
    m_cursor_state = !m_cursor_state;
    if (is_focused())
        update_cursor();
}

GTextEditor::Line::Line(GTextEditor& editor)
    : m_editor(editor)
{
    clear();
}

GTextEditor::Line::Line(GTextEditor& editor, const StringView& text)
    : m_editor(editor)
{
    set_text(text);
}

void GTextEditor::Line::clear()
{
    m_text.clear();
    m_text.append(0);
}

void GTextEditor::Line::set_text(const StringView& text)
{
    if (text.length() == length() && !memcmp(text.characters_without_null_termination(), characters(), length()))
        return;
    if (text.is_empty()) {
        clear();
        return;
    }
    m_text.resize(text.length() + 1);
    memcpy(m_text.data(), text.characters_without_null_termination(), text.length() + 1);
}

void GTextEditor::Line::append(const char* characters, int length)
{
    int old_length = m_text.size() - 1;
    m_text.resize(m_text.size() + length);
    memcpy(m_text.data() + old_length, characters, length);
    m_text.last() = 0;
}

void GTextEditor::Line::append(char ch)
{
    insert(length(), ch);
}

void GTextEditor::Line::prepend(char ch)
{
    insert(0, ch);
}

void GTextEditor::Line::insert(int index, char ch)
{
    if (index == length()) {
        m_text.last() = ch;
        m_text.append(0);
    } else {
        m_text.insert(index, move(ch));
    }
}

void GTextEditor::Line::remove(int index)
{
    if (index == length()) {
        m_text.take_last();
        m_text.last() = 0;
    } else {
        m_text.remove(index);
    }
}

void GTextEditor::Line::truncate(int length)
{
    m_text.resize(length + 1);
    m_text.last() = 0;
}

bool GTextEditor::write_to_file(const StringView& path)
{
    int fd = open_with_path_length(path.characters_without_null_termination(), path.length(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
    if (fd < 0) {
        perror("open");
        return false;
    }

    // Compute the final file size and ftruncate() to make writing fast.
    // FIXME: Remove this once the kernel is smart enough to do this instead.
    off_t file_size = 0;
    for (int i = 0; i < m_lines.size(); ++i)
        file_size += m_lines[i].length();
    file_size += m_lines.size() - 1;

    int rc = ftruncate(fd, file_size);
    if (rc < 0) {
        perror("ftruncate");
        return false;
    }

    for (int i = 0; i < m_lines.size(); ++i) {
        auto& line = m_lines[i];
        if (line.length()) {
            ssize_t nwritten = write(fd, line.characters(), line.length());
            if (nwritten < 0) {
                perror("write");
                close(fd);
                return false;
            }
        }
        if (i != m_lines.size() - 1) {
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

String GTextEditor::text() const
{
    StringBuilder builder;
    for (int i = 0; i < line_count(); ++i) {
        auto& line = m_lines[i];
        builder.append(line.characters(), line.length());
        if (i != line_count() - 1)
            builder.append('\n');
    }
    return builder.to_string();
}

void GTextEditor::clear()
{
    m_lines.clear();
    m_lines.append(make<Line>(*this));
    m_selection.clear();
    did_update_selection();
    set_cursor(0, 0);
    update();
}

String GTextEditor::selected_text() const
{
    if (!has_selection())
        return {};

    auto selection = normalized_selection();
    StringBuilder builder;
    for (int i = selection.start().line(); i <= selection.end().line(); ++i) {
        auto& line = m_lines[i];
        int selection_start_column_on_line = selection.start().line() == i ? selection.start().column() : 0;
        int selection_end_column_on_line = selection.end().line() == i ? selection.end().column() : line.length();
        builder.append(line.characters() + selection_start_column_on_line, selection_end_column_on_line - selection_start_column_on_line);
        if (i != selection.end().line())
            builder.append('\n');
    }

    return builder.to_string();
}

void GTextEditor::delete_selection()
{
    if (!has_selection())
        return;

    auto selection = normalized_selection();

    // First delete all the lines in between the first and last one.
    for (int i = selection.start().line() + 1; i < selection.end().line();) {
        m_lines.remove(i);
        selection.end().set_line(selection.end().line() - 1);
    }

    if (selection.start().line() == selection.end().line()) {
        // Delete within same line.
        auto& line = m_lines[selection.start().line()];
        bool whole_line_is_selected = selection.start().column() == 0 && selection.end().column() == line.length();
        if (whole_line_is_selected) {
            line.clear();
        } else {
            auto before_selection = String(line.characters(), line.length()).substring(0, selection.start().column());
            auto after_selection = String(line.characters(), line.length()).substring(selection.end().column(), line.length() - selection.end().column());
            StringBuilder builder(before_selection.length() + after_selection.length());
            builder.append(before_selection);
            builder.append(after_selection);
            line.set_text(builder.to_string());
        }
    } else {
        // Delete across a newline, merging lines.
        ASSERT(selection.start().line() == selection.end().line() - 1);
        auto& first_line = m_lines[selection.start().line()];
        auto& second_line = m_lines[selection.end().line()];
        auto before_selection = String(first_line.characters(), first_line.length()).substring(0, selection.start().column());
        auto after_selection = String(second_line.characters(), second_line.length()).substring(selection.end().column(), second_line.length() - selection.end().column());
        StringBuilder builder(before_selection.length() + after_selection.length());
        builder.append(before_selection);
        builder.append(after_selection);
        first_line.set_text(builder.to_string());
        m_lines.remove(selection.end().line());
    }

    if (m_lines.is_empty())
        m_lines.append(make<Line>(*this));

    m_selection.clear();
    did_update_selection();
    did_change();
    set_cursor(selection.start());
    update();
}

void GTextEditor::insert_at_cursor_or_replace_selection(const StringView& text)
{
    ASSERT(!is_readonly());
    if (has_selection())
        delete_selection();
    insert_at_cursor(text);
}

void GTextEditor::cut()
{
    if (is_readonly())
        return;
    auto selected_text = this->selected_text();
    printf("Cut: \"%s\"\n", selected_text.characters());
    GClipboard::the().set_data(selected_text);
    delete_selection();
}

void GTextEditor::copy()
{
    auto selected_text = this->selected_text();
    printf("Copy: \"%s\"\n", selected_text.characters());
    GClipboard::the().set_data(selected_text);
}

void GTextEditor::paste()
{
    if (is_readonly())
        return;
    auto paste_text = GClipboard::the().data();
    printf("Paste: \"%s\"\n", paste_text.characters());
    insert_at_cursor_or_replace_selection(paste_text);
}

void GTextEditor::enter_event(CEvent&)
{
    ASSERT(window());
    window()->set_override_cursor(GStandardCursor::IBeam);
}

void GTextEditor::leave_event(CEvent&)
{
    ASSERT(window());
    window()->set_override_cursor(GStandardCursor::None);
}

void GTextEditor::did_change()
{
    ASSERT(!is_readonly());
    update_content_size();
    recompute_all_visual_lines();
    if (!m_have_pending_change_notification) {
        m_have_pending_change_notification = true;
        deferred_invoke([this](auto&) {
            if (on_change)
                on_change();
            m_have_pending_change_notification = false;
        });
    }
}

void GTextEditor::set_readonly(bool readonly)
{
    if (m_readonly == readonly)
        return;
    m_readonly = readonly;
    m_cut_action->set_enabled(!is_readonly() && has_selection());
    m_delete_action->set_enabled(!is_readonly());
    m_paste_action->set_enabled(!is_readonly());
}

void GTextEditor::did_update_selection()
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

void GTextEditor::context_menu_event(GContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = make<GMenu>();
        m_context_menu->add_action(undo_action());
        m_context_menu->add_action(redo_action());
        m_context_menu->add_separator();
        m_context_menu->add_action(cut_action());
        m_context_menu->add_action(copy_action());
        m_context_menu->add_action(paste_action());
        m_context_menu->add_action(delete_action());
        if (!m_custom_context_menu_actions.is_empty()) {
            m_context_menu->add_separator();
            for (auto& action : m_custom_context_menu_actions) {
                m_context_menu->add_action(action);
            }
        }
    }
    m_context_menu->popup(event.screen_position());
}

void GTextEditor::set_text_alignment(TextAlignment alignment)
{
    if (m_text_alignment == alignment)
        return;
    m_text_alignment = alignment;
    update();
}

void GTextEditor::resize_event(GResizeEvent& event)
{
    GScrollableWidget::resize_event(event);
    update_content_size();
    recompute_all_visual_lines();
}

GTextPosition GTextEditor::next_position_after(const GTextPosition& position, ShouldWrapAtEndOfDocument should_wrap)
{
    auto& line = m_lines[position.line()];
    if (position.column() == line.length()) {
        if (position.line() == line_count() - 1) {
            if (should_wrap == ShouldWrapAtEndOfDocument::Yes)
                return { 0, 0 };
            return {};
        }
        return { position.line() + 1, 0 };
    }
    return { position.line(), position.column() + 1 };
}

GTextPosition GTextEditor::prev_position_before(const GTextPosition& position, ShouldWrapAtStartOfDocument should_wrap)
{
    if (position.column() == 0) {
        if (position.line() == 0) {
            if (should_wrap == ShouldWrapAtStartOfDocument::Yes) {
                auto& last_line = m_lines[line_count() - 1];
                return { line_count() - 1, last_line.length() };
            }
            return {};
        }
        auto& prev_line = m_lines[position.line() - 1];
        return { position.line() - 1, prev_line.length() };
    }
    return { position.line(), position.column() - 1 };
}

GTextRange GTextEditor::find_next(const StringView& needle, const GTextPosition& start)
{
    if (needle.is_empty())
        return {};

    GTextPosition position = start.is_valid() ? start : GTextPosition(0, 0);
    GTextPosition original_position = position;

    GTextPosition start_of_potential_match;
    int needle_index = 0;

    do {
        auto ch = character_at(position);
        if (ch == needle[needle_index]) {
            if (needle_index == 0)
                start_of_potential_match = position;
            ++needle_index;
            if (needle_index >= needle.length())
                return { start_of_potential_match, next_position_after(position) };
        } else {
            if (needle_index > 0)
                position = start_of_potential_match;
            needle_index = 0;
        }
        position = next_position_after(position);
    } while (position.is_valid() && position != original_position);

    return {};
}

GTextRange GTextEditor::find_prev(const StringView& needle, const GTextPosition& start)
{
    if (needle.is_empty())
        return {};

    GTextPosition position = start.is_valid() ? start : GTextPosition(0, 0);
    position = prev_position_before(position);
    GTextPosition original_position = position;

    GTextPosition end_of_potential_match;
    int needle_index = needle.length() - 1;

    do {
        auto ch = character_at(position);
        if (ch == needle[needle_index]) {
            if (needle_index == needle.length() - 1)
                end_of_potential_match = position;
            --needle_index;
            if (needle_index < 0)
                return { position, next_position_after(end_of_potential_match) };
        } else {
            if (needle_index < needle.length() - 1)
                position = end_of_potential_match;
            needle_index = needle.length() - 1;
        }
        position = prev_position_before(position);
    } while (position.is_valid() && position != original_position);

    return {};
}

void GTextEditor::set_selection(const GTextRange& selection)
{
    if (m_selection == selection)
        return;
    m_selection = selection;
    set_cursor(m_selection.end());
    scroll_position_into_view(normalized_selection().start());
    update();
}

char GTextEditor::character_at(const GTextPosition& position) const
{
    ASSERT(position.line() < line_count());
    auto& line = m_lines[position.line()];
    if (position.column() == line.length())
        return '\n';
    return line.characters()[position.column()];
}

void GTextEditor::recompute_all_visual_lines()
{
    int y_offset = 0;
    for (auto& line : m_lines) {
        line.recompute_visual_lines();
        line.m_visual_rect.set_y(y_offset);
        y_offset += line.m_visual_rect.height();
    }

    update_content_size();
}

void GTextEditor::Line::recompute_visual_lines()
{
    m_visual_line_breaks.clear_with_capacity();

    int available_width = m_editor.visible_text_rect_in_inner_coordinates().width();

    if (m_editor.is_line_wrapping_enabled()) {
        int line_width_so_far = 0;

        for (int i = 0; i < length(); ++i) {
            auto ch = characters()[i];
            auto glyph_width = m_editor.font().glyph_width(ch);
            if ((line_width_so_far + glyph_width) > available_width) {
                m_visual_line_breaks.append(i);
                line_width_so_far = glyph_width;
                continue;
            }
            line_width_so_far += glyph_width;
        }
    }

    m_visual_line_breaks.append(length());

    if (m_editor.is_line_wrapping_enabled())
        m_visual_rect = { m_editor.m_horizontal_content_padding, 0, available_width, m_visual_line_breaks.size() * m_editor.line_height() };
    else
        m_visual_rect = { m_editor.m_horizontal_content_padding, 0, m_editor.font().width(view()), m_editor.line_height() };
}

template<typename Callback>
void GTextEditor::Line::for_each_visual_line(Callback callback) const
{
    int start_of_line = 0;
    int line_index = 0;
    for (auto visual_line_break : m_visual_line_breaks) {
        auto visual_line_view = StringView(characters() + start_of_line, visual_line_break - start_of_line);
        Rect visual_line_rect {
            m_visual_rect.x(),
            m_visual_rect.y() + (line_index * m_editor.line_height()),
            m_editor.font().width(visual_line_view),
            m_editor.line_height()
        };
        if (callback(visual_line_rect, visual_line_view, start_of_line) == IterationDecision::Break)
            break;
        start_of_line = visual_line_break;
        ++line_index;
    }
}

void GTextEditor::set_line_wrapping_enabled(bool enabled)
{
    if (m_line_wrapping_enabled == enabled)
        return;

    m_line_wrapping_enabled = enabled;
    horizontal_scrollbar().set_visible(!m_line_wrapping_enabled);
    update_content_size();
    recompute_all_visual_lines();
    update();
}

int GTextEditor::Line::visual_line_containing(int column) const
{
    int visual_line_index = 0;
    for_each_visual_line([&](const Rect&, const StringView& view, int start_of_visual_line) {
        if (column >= start_of_visual_line && ((column - start_of_visual_line) < view.length()))
            return IterationDecision::Break;
        ++visual_line_index;
        return IterationDecision::Continue;
    });
    return visual_line_index;
}

void GTextEditor::add_custom_context_menu_action(GAction& action)
{
    m_custom_context_menu_actions.append(action);
}

void GTextEditor::did_change_font()
{
    vertical_scrollbar().set_step(line_height());
    GWidget::did_change_font();
}
