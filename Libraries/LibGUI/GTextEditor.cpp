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
    m_lines.append(make<Line>());
    m_cursor = { 0, 0 };
    create_actions();
}

GTextEditor::~GTextEditor()
{
}

void GTextEditor::create_actions()
{
    m_undo_action = GAction::create("Undo", { Mod_Ctrl, Key_Z }, GraphicsBitmap::load_from_file("/res/icons/16x16/undo.png"), [&](const GAction&) {
        // FIXME: Undo
    },
        this);

    m_redo_action = GAction::create("Redo", { Mod_Ctrl, Key_Y }, GraphicsBitmap::load_from_file("/res/icons/16x16/redo.png"), [&](const GAction&) {
        // FIXME: Redo
    },
        this);

    m_cut_action = GAction::create("Cut", { Mod_Ctrl, Key_X }, GraphicsBitmap::load_from_file("/res/icons/cut16.png"), [&](const GAction&) {
        cut();
    },
        this);

    m_copy_action = GAction::create("Copy", { Mod_Ctrl, Key_C }, GraphicsBitmap::load_from_file("/res/icons/16x16/edit-copy.png"), [&](const GAction&) {
        copy();
    },
        this);

    m_paste_action = GAction::create("Paste", { Mod_Ctrl, Key_V }, GraphicsBitmap::load_from_file("/res/icons/paste16.png"), [&](const GAction&) {
        paste();
    },
        this);

    m_delete_action = GAction::create("Delete", { 0, Key_Delete }, GraphicsBitmap::load_from_file("/res/icons/16x16/delete.png"), [&](const GAction&) {
        do_delete();
    },
        this);
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
        auto line = make<Line>();
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
    for (auto& line : m_lines)
        content_width = max(line.width(font()), content_width);
    content_width += m_horizontal_content_padding * 2;
    if (is_right_text_alignment(m_text_alignment))
        content_width = max(frame_inner_rect().width(), content_width);
    int content_height = line_count() * line_height();
    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ ruler_width(), 0 });
}

GTextPosition GTextEditor::text_position_at(const Point& a_position) const
{
    auto position = a_position;
    position.move_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    position.move_by(-(m_horizontal_content_padding + ruler_width()), 0);
    position.move_by(-frame_thickness(), -frame_thickness());

    int line_index = position.y() / line_height();
    line_index = max(0, min(line_index, line_count() - 1));

    int column_index;
    switch (m_text_alignment) {
    case TextAlignment::CenterLeft:
        column_index = (position.x() + glyph_width() / 2) / glyph_width();
        break;
    case TextAlignment::CenterRight:
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
        line_index * line_height(),
        ruler_width(),
        line_height()
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

    Rect ruler_rect { 0, 0, ruler_width(), height() - height_occupied_by_horizontal_scrollbar() };

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
                ruler_line_rect.shrunken(2, 0),
                String::number(i + 1),
                is_current_line ? Font::default_bold_font() : font(),
                TextAlignment::CenterRight,
                is_current_line ? Color::DarkGray : Color::MidGray);
        }
    }

    painter.add_clip_rect({ m_ruler_visible ? (ruler_rect.right() + frame_thickness() + 1) : frame_thickness(), frame_thickness(), width() - width_occupied_by_vertical_scrollbar() - ruler_width(), height() - height_occupied_by_horizontal_scrollbar() });

    for (int i = first_visible_line; i <= last_visible_line; ++i) {
        auto& line = m_lines[i];
        auto line_rect = line_content_rect(i);
        // FIXME: Make sure we always fill the entire line.
        //line_rect.set_width(exposed_width);
        if (is_multi_line() && i == m_cursor.line())
            painter.fill_rect(line_rect, Color(230, 230, 230));
        painter.draw_text(line_rect, StringView(line.characters(), line.length()), m_text_alignment, Color::Black);
        bool line_has_selection = has_selection && i >= selection.start().line() && i <= selection.end().line();
        if (line_has_selection) {
            int selection_start_column_on_line = selection.start().line() == i ? selection.start().column() : 0;
            int selection_end_column_on_line = selection.end().line() == i ? selection.end().column() : line.length();

            int selection_left = content_x_for_position({ i, selection_start_column_on_line });
            int selection_right = content_x_for_position({ i, selection_end_column_on_line });

            Rect selection_rect { selection_left, line_rect.y(), selection_right - selection_left, line_rect.height() };
            painter.fill_rect(selection_rect, Color::from_rgb(0x955233));
            painter.draw_text(selection_rect, StringView(line.characters() + selection_start_column_on_line, line.length() - selection_start_column_on_line - (line.length() - selection_end_column_on_line)), TextAlignment::CenterLeft, Color::White);
        }
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
        if (m_cursor.column() < current_line().length()) {
            int new_column = m_cursor.column() + 1;
            toggle_selection_if_needed_for_event(event);
            set_cursor(m_cursor.line(), new_column);
            if (m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        } else if (m_cursor.line() != line_count() - 1) {
            int new_line = m_cursor.line() + 1;
            int new_column = 0;
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
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
        m_lines.append(make<Line>());

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
            m_lines.insert(m_cursor.line() + (at_tail ? 1 : 0), make<Line>(new_line_contents));
            update();
            did_change();
            set_cursor(m_cursor.line() + 1, m_lines[m_cursor.line() + 1].length());
            return;
        }
        auto new_line = make<Line>();
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
    switch (m_text_alignment) {
    case TextAlignment::CenterLeft:
        return m_horizontal_content_padding + position.column() * glyph_width();
    case TextAlignment::CenterRight:
        return content_width() - m_horizontal_content_padding - (line.length() * glyph_width()) + (position.column() * glyph_width());
    default:
        ASSERT_NOT_REACHED();
    }
}

Rect GTextEditor::cursor_content_rect() const
{
    if (!m_cursor.is_valid())
        return {};
    ASSERT(!m_lines.is_empty());
    ASSERT(m_cursor.column() <= (current_line().length() + 1));

    int cursor_x = content_x_for_position(m_cursor);

    if (is_single_line()) {
        Rect cursor_rect { cursor_x, 0, 1, font().glyph_height() + 2 };
        cursor_rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return cursor_rect;
    }
    return { cursor_x, m_cursor.line() * line_height(), 1, line_height() };
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

void GTextEditor::scroll_cursor_into_view()
{
    auto rect = cursor_content_rect();
    if (m_cursor.column() == 0)
        rect.set_x(content_x_for_position({ m_cursor.line(), 0 }) - 2);
    else if (m_cursor.column() == m_lines[m_cursor.line()].length())
        rect.set_x(content_x_for_position({ m_cursor.line(), m_lines[m_cursor.line()].length() }) + 2);
    scroll_into_view(rect, true, true);
}

Rect GTextEditor::line_content_rect(int line_index) const
{
    auto& line = m_lines[line_index];
    if (is_single_line()) {
        Rect line_rect = { content_x_for_position({ line_index, 0 }), 0, line.length() * glyph_width(), font().glyph_height() + 2 };
        line_rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return line_rect;
    }
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

GTextEditor::Line::Line()
{
    clear();
}

GTextEditor::Line::Line(const StringView& text)
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

int GTextEditor::Line::width(const Font& font) const
{
    return font.glyph_width('x') * length();
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
    m_lines.append(make<Line>());
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
        m_lines.append(make<Line>());

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
}

void GTextEditor::context_menu_event(GContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = make<GMenu>("GTextEditor context menu");
        m_context_menu->add_action(undo_action());
        m_context_menu->add_action(redo_action());
        m_context_menu->add_separator();
        m_context_menu->add_action(cut_action());
        m_context_menu->add_action(copy_action());
        m_context_menu->add_action(paste_action());
        m_context_menu->add_action(delete_action());
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
}
