#include <LibGUI/GTextEditor.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GFontDatabase.h>
#include <LibGUI/GClipboard.h>
#include <SharedGraphics/Painter.h>
#include <Kernel/KeyCode.h>
#include <AK/StringBuilder.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>

GTextEditor::GTextEditor(Type type, GWidget* parent)
    : GScrollableWidget(parent)
    , m_type(type)
{
    set_scrollbars_enabled(is_multi_line());
    m_ruler_visible = is_multi_line();
    set_font(GFontDatabase::the().get_by_name("Csilla Thin"));
    m_lines.append(make<Line>());
    m_cursor = { 0, 0 };
}

GTextEditor::~GTextEditor()
{
}

void GTextEditor::set_text(const String& text)
{
    m_lines.clear();
    int start_of_current_line = 0;

    auto add_line = [&] (int current_position) {
        int line_length = current_position - start_of_current_line;
        auto line = make<Line>();
        if (line_length)
            line->set_text(text.substring(start_of_current_line, current_position - start_of_current_line));
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
    set_cursor(0, 0);
    update();
}

void GTextEditor::update_content_size()
{
    int content_width = 0;
    for (auto& line : m_lines)
        content_width = max(line->width(font()), content_width);
    content_width += m_horizontal_content_padding * 2;
    int content_height = line_count() * line_height();
    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ ruler_width(), 0 });
}

GTextPosition GTextEditor::text_position_at(const Point& a_position) const
{
    auto position = a_position;
    position.move_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    position.move_by(-(m_horizontal_content_padding + ruler_width()), 0);
    int line_index = position.y() / line_height();
    int column_index = position.x() / glyph_width();
    line_index = max(0, min(line_index, line_count() - 1));
    column_index = max(0, min(column_index, m_lines[line_index]->length()));
    return { line_index, column_index };
}

void GTextEditor::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        if (event.modifiers() & Mod_Shift) {
            if (!has_selection())
                m_selection.set(m_cursor, { });
        } else {
            m_selection.clear();
        }

        m_in_drag_select = true;
        set_global_cursor_tracking(true);

        set_cursor(text_position_at(event.position()));

        if (!(event.modifiers() & Mod_Shift)) {
            if (!has_selection())
                m_selection.set(m_cursor, { });
        }

        if (m_selection.start().is_valid())
            m_selection.set_end(m_cursor);

        // FIXME: Only update the relevant rects.
        update();
        return;
    }
}

void GTextEditor::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        if (m_in_drag_select) {
            m_in_drag_select = false;
            set_global_cursor_tracking(false);
        }
        return;
    }
}

void GTextEditor::mousemove_event(GMouseEvent& event)
{
    if (m_in_drag_select) {
        set_cursor(text_position_at(event.position()));
        m_selection.set_end(m_cursor);
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
        return { };
    return {
        0 - ruler_width() + horizontal_scrollbar().value(),
        line_index * line_height(),
        ruler_width(),
        line_height()
    };
}

void GTextEditor::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    Rect item_area_rect { 0, 0, width() - width_occupied_by_vertical_scrollbar(), height() - height_occupied_by_horizontal_scrollbar() };
    painter.set_clip_rect(item_area_rect);
    painter.set_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::White);

    Rect ruler_rect { 0, 0, ruler_width(), height() - height_occupied_by_horizontal_scrollbar()};

    if (m_ruler_visible) {
        painter.fill_rect(ruler_rect, Color::LightGray);
        painter.draw_line(ruler_rect.top_right(), ruler_rect.bottom_right(), Color::DarkGray);
    }

    painter.save();

    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    painter.translate(ruler_width(), 0);
    int exposed_width = max(content_width(), width());

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
                String::format("%u", i),
                is_current_line ? Font::default_bold_font() : font(),
                TextAlignment::CenterRight,
                is_current_line ? Color::DarkGray : Color::MidGray
            );
        }
    }

    painter.set_clip_rect({ ruler_rect.right() + 1, 0, width() - width_occupied_by_vertical_scrollbar() - ruler_width(), height() - height_occupied_by_horizontal_scrollbar() });

    for (int i = first_visible_line; i <= last_visible_line; ++i) {
        auto& line = *m_lines[i];
        auto line_rect = line_content_rect(i);
        line_rect.set_width(exposed_width);
        if (is_multi_line() && i == m_cursor.line())
            painter.fill_rect(line_rect, Color(230, 230, 230));
        painter.draw_text(line_rect, line.characters(), line.length(), TextAlignment::CenterLeft, Color::Black);
        bool line_has_selection = has_selection && i >= selection.start().line() && i <= selection.end().line();
        if (line_has_selection) {
            int selection_start_column_on_line = selection.start().line() == i ? selection.start().column() : 0;
            int selection_end_column_on_line = selection.end().line() == i ? selection.end().column() : line.length();
            int selection_left = m_horizontal_content_padding + selection_start_column_on_line * font().glyph_width('x');
            int selection_right = line_rect.left() + selection_end_column_on_line * font().glyph_width('x');
            Rect selection_rect { selection_left, line_rect.y(), selection_right - selection_left, line_rect.height() };
            painter.fill_rect(selection_rect, Color::from_rgb(0x955233));
            painter.draw_text(selection_rect, line.characters() + selection_start_column_on_line, line.length() - selection_start_column_on_line - (line.length() - selection_end_column_on_line), TextAlignment::CenterLeft, Color::White);
        }
    }

    if (is_focused() && m_cursor_state)
        painter.fill_rect(cursor_content_rect(), Color::Red);

    painter.restore();

    if (is_focused())
        painter.draw_rect(item_area_rect, Color::from_rgb(0x84351a));
}

void GTextEditor::toggle_selection_if_needed_for_event(const GKeyEvent& event)
{
    if (event.shift() && !m_selection.is_valid()) {
        m_selection.set(m_cursor, { });
        update();
        return;
    }
    if (!event.shift() && m_selection.is_valid()) {
        m_selection.clear();
        update();
        return;
    }
}

void GTextEditor::keydown_event(GKeyEvent& event)
{
    if (event.key() == KeyCode::Key_Escape) {
        if (on_escape_pressed)
            on_escape_pressed(*this);
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        if (m_cursor.line() > 0) {
            int new_line = m_cursor.line() - 1;
            int new_column = min(m_cursor.column(), m_lines[new_line]->length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid())
                m_selection.set_end(m_cursor);
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        if (m_cursor.line() < (m_lines.size() - 1)) {
            int new_line = m_cursor.line() + 1;
            int new_column = min(m_cursor.column(), m_lines[new_line]->length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid())
                m_selection.set_end(m_cursor);
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        if (m_cursor.line() > 0) {
            int new_line = max(0, m_cursor.line() - visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), m_lines[new_line]->length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid())
                m_selection.set_end(m_cursor);
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        if (m_cursor.line() < (m_lines.size() - 1)) {
            int new_line = min(line_count() - 1, m_cursor.line() + visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), m_lines[new_line]->length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid())
                m_selection.set_end(m_cursor);
        }
        return;
    }
    if (event.key() == KeyCode::Key_Left) {
        if (m_cursor.column() > 0) {
            int new_column = m_cursor.column() - 1;
            toggle_selection_if_needed_for_event(event);
            set_cursor(m_cursor.line(), new_column);
            if (m_selection.start().is_valid())
                m_selection.set_end(m_cursor);
        } else if (m_cursor.line() > 0) {
            int new_line = m_cursor.line() - 1;
            int new_column = m_lines[new_line]->length();
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid())
                m_selection.set_end(m_cursor);
        }
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
        if (m_cursor.column() < current_line().length()) {
            int new_column = m_cursor.column() + 1;
            toggle_selection_if_needed_for_event(event);
            set_cursor(m_cursor.line(), new_column);
            if (m_selection.start().is_valid())
                m_selection.set_end(m_cursor);
        } else if (m_cursor.line() != line_count() - 1) {
            int new_line = m_cursor.line() + 1;
            int new_column = 0;
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (m_selection.start().is_valid())
                m_selection.set_end(m_cursor);
        }
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_Home) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(m_cursor.line(), 0);
        if (m_selection.start().is_valid())
            m_selection.set_end(m_cursor);
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_End) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(m_cursor.line(), current_line().length());
        if (m_selection.start().is_valid())
            m_selection.set_end(m_cursor);
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_Home) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(0, 0);
        if (m_selection.start().is_valid())
            m_selection.set_end(m_cursor);
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(line_count() - 1, m_lines[line_count() - 1]->length());
        if (m_selection.start().is_valid())
            m_selection.set_end(m_cursor);
        return;
    }
    if (event.modifiers() == Mod_Ctrl && event.key() == KeyCode::Key_A) {
        GTextPosition start_of_document { 0, 0 };
        GTextPosition end_of_document { line_count() - 1, m_lines[line_count() - 1]->length() };
        m_selection.set(start_of_document, end_of_document);
        set_cursor(end_of_document);
        update();
        return;
    }

    if (event.key() == KeyCode::Key_Backspace) {
        if (has_selection()) {
            delete_selection();
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
            auto& previous_line = *m_lines[m_cursor.line() - 1];
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

    if (event.key() == KeyCode::Key_Delete) {
        if (has_selection()) {
            delete_selection();
            return;
        }
        if (m_cursor.column() < current_line().length()) {
            // Delete within line
            current_line().remove(m_cursor.column());
            update_content_size();
            update_cursor();
            return;
        }
        if (m_cursor.column() == current_line().length() && m_cursor.line() != line_count() - 1) {
            // Delete at end of line; merge with next line
            auto& next_line = *m_lines[m_cursor.line() + 1];
            int previous_length = current_line().length();
            current_line().append(next_line.characters(), next_line.length());
            m_lines.remove(m_cursor.line() + 1);
            update_content_size();
            update();
            set_cursor(m_cursor.line(), previous_length);
            return;
        }
        return;
    }

    if (!event.ctrl() && !event.alt() && !event.text().is_empty())
        insert_at_cursor_or_replace_selection(event.text());

    return GWidget::keydown_event(event);
}

void GTextEditor::insert_at_cursor(const String& text)
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
        if (is_single_line()) {
            if (on_return_pressed)
                on_return_pressed(*this);
            return;
        }
        if (at_tail || at_head) {
            m_lines.insert(m_cursor.line() + (at_tail ? 1 : 0), make<Line>());
            update_content_size();
            update();
            set_cursor(m_cursor.line() + 1, 0);
            return;
        }
        auto new_line = make<Line>();
        new_line->append(current_line().characters() + m_cursor.column(), current_line().length() - m_cursor.column());
        current_line().truncate(m_cursor.column());
        m_lines.insert(m_cursor.line() + 1, move(new_line));
        update_content_size();
        update();
        set_cursor(m_cursor.line() + 1, 0);
        return;
    }
    if (ch == '\t') {
        int next_soft_tab_stop = ((m_cursor.column() + m_soft_tab_width) / m_soft_tab_width) * m_soft_tab_width;
        int spaces_to_insert = next_soft_tab_stop - m_cursor.column();
        for (int i = 0; i < spaces_to_insert; ++i) {
            current_line().insert(m_cursor.column(), ' ');
        }
        update_content_size();
        set_cursor(m_cursor.line(), next_soft_tab_stop);
        update_cursor();
        return;
    }
    current_line().insert(m_cursor.column(), ch);
    update_content_size();
    set_cursor(m_cursor.line(), m_cursor.column() + 1);
    update_cursor();
}

Rect GTextEditor::cursor_content_rect() const
{
    if (!m_cursor.is_valid())
        return { };
    ASSERT(!m_lines.is_empty());
    ASSERT(m_cursor.column() <= (current_line().length() + 1));
    return { m_horizontal_content_padding + m_cursor.column() * glyph_width(), m_cursor.line() * line_height(), 1, line_height() };
}

Rect GTextEditor::line_widget_rect(int line_index) const
{
    auto rect = line_content_rect(line_index);
    rect.move_by(-(horizontal_scrollbar().value() - m_horizontal_content_padding), -(vertical_scrollbar().value()));
    rect.set_width(rect.width() + 1); // Add 1 pixel for when the cursor is on the end.
    rect.intersect(this->rect());
    // This feels rather hackish, but extend the rect to the edge of the content view:
    rect.set_right(vertical_scrollbar().relative_rect().left() - 1);
    return rect;
}

void GTextEditor::scroll_cursor_into_view()
{
    auto rect = cursor_content_rect();
    if (m_cursor.column() == 0)
        rect.set_x(0);
    else if (m_cursor.column() >= m_lines[m_cursor.line()]->length())
        rect.set_x(m_lines[m_cursor.line()]->width(font()) + m_horizontal_content_padding * 2);
    scroll_into_view(rect, true, true);
}

Rect GTextEditor::line_content_rect(int line_index) const
{
    return {
        m_horizontal_content_padding,
        line_index * line_height(),
        content_width(),
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
    ASSERT(position.column() <= m_lines[position.line()]->length());
    if (m_cursor != position) {
        auto old_cursor_line_rect = line_widget_rect(m_cursor.line());
        m_cursor = position;
        m_cursor_state = true;
        scroll_cursor_into_view();
        update(old_cursor_line_rect);
        update_cursor();
    }
    if (on_cursor_change)
        on_cursor_change(*this);
}

void GTextEditor::focusin_event(GEvent&)
{
    update_cursor();
    start_timer(500);
}

void GTextEditor::focusout_event(GEvent&)
{
    stop_timer();
}

void GTextEditor::timer_event(GTimerEvent&)
{
    m_cursor_state = !m_cursor_state;
    if (is_focused())
        update_cursor();
}

GTextEditor::Line::Line()
{
    clear();
}

void GTextEditor::Line::clear()
{
    m_text.clear();
    m_text.append(0);
}

void GTextEditor::Line::set_text(const String& text)
{
    if (text.length() == length() && !memcmp(text.characters(), characters(), length()))
        return;
    if (text.is_empty()) {
        clear();
        return;
    }
    m_text.resize(text.length() + 1);
    memcpy(m_text.data(), text.characters(), text.length() + 1);
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

bool GTextEditor::write_to_file(const String& path)
{
    int fd = open(path.characters(), O_WRONLY | O_CREAT, 0666);
    if (fd < 0) {
        perror("open");
        return false;
    }
    for (int i = 0; i < m_lines.size(); ++i) {
        auto& line = *m_lines[i];
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
        auto& line = *m_lines[i];
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
    set_cursor(0, 0);
    update();
}

String GTextEditor::selected_text() const
{
    if (!has_selection())
        return { };

    auto selection = normalized_selection();
    StringBuilder builder;
    for (int i = selection.start().line(); i <= selection.end().line(); ++i) {
        auto& line = *m_lines[i];
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
        auto& line = *m_lines[selection.start().line()];
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
        auto& first_line = *m_lines[selection.start().line()];
        auto& second_line = *m_lines[selection.end().line()];
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
    set_cursor(selection.start());
    update();
}

void GTextEditor::insert_at_cursor_or_replace_selection(const String& text)
{
    if (has_selection())
        delete_selection();
    insert_at_cursor(text);
}

void GTextEditor::cut()
{
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
    auto paste_text = GClipboard::the().data();
    printf("Paste: \"%s\"\n", paste_text.characters());
    insert_at_cursor_or_replace_selection(paste_text);
}
