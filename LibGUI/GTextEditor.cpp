#include <LibGUI/GTextEditor.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GFontDatabase.h>
#include <SharedGraphics/Painter.h>
#include <Kernel/KeyCode.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>

GTextEditor::GTextEditor(GWidget* parent)
    : GWidget(parent)
{
    set_font(GFontDatabase::the().get_by_name("Csilla Thin"));

    set_fill_with_background_color(false);

    m_vertical_scrollbar = new GScrollBar(Orientation::Vertical, this);
    m_vertical_scrollbar->set_step(4);
    m_vertical_scrollbar->on_change = [this] (int) {
        update();
    };

    m_horizontal_scrollbar = new GScrollBar(Orientation::Horizontal, this);
    m_horizontal_scrollbar->set_step(4);
    m_horizontal_scrollbar->set_big_step(30);
    m_horizontal_scrollbar->on_change = [this] (int) {
        update();
    };

    m_lines.append(make<Line>());
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
    set_cursor(0, 0);
    update();
}

void GTextEditor::resize_event(GResizeEvent& event)
{
    update_scrollbar_ranges();
    m_vertical_scrollbar->set_relative_rect(event.size().width() - m_vertical_scrollbar->preferred_size().width(), 0, m_vertical_scrollbar->preferred_size().width(), event.size().height() - m_horizontal_scrollbar->preferred_size().height());
    m_horizontal_scrollbar->set_relative_rect(0, event.size().height() - m_horizontal_scrollbar->preferred_size().height(), event.size().width() - m_vertical_scrollbar->preferred_size().width(), m_horizontal_scrollbar->preferred_size().height());
}

void GTextEditor::update_scrollbar_ranges()
{
    int available_height = height() - m_horizontal_scrollbar->height();
    int excess_height = max(0, (content_height() + padding() * 2) - available_height);
    m_vertical_scrollbar->set_range(0, excess_height);

    int available_width = width() - m_vertical_scrollbar->width() - ruler_width();
    int excess_width = max(0, (content_width() + padding() * 2) - available_width);
    m_horizontal_scrollbar->set_range(0, excess_width);

    m_vertical_scrollbar->set_big_step(visible_content_rect().height());
}

int GTextEditor::content_height() const
{
    return line_count() * line_height();
}

int GTextEditor::content_width() const
{
    // FIXME: Cache this somewhere.
    int max_width = 0;
    for (auto& line : m_lines)
        max_width = max(line->width(font()), max_width);
    return max_width;
}

GTextPosition GTextEditor::text_position_at(const Point& a_position) const
{
    auto position = a_position;
    position.move_by(m_horizontal_scrollbar->value(), m_vertical_scrollbar->value());
    position.move_by(-(padding() + ruler_width()), -padding());
    int line_index = position.y() / line_height();
    int column_index = position.x() / glyph_width();
    line_index = min(line_index, line_count() - 1);
    column_index = min(column_index, m_lines[line_index]->length());
    return { line_index, column_index };
}

void GTextEditor::mousedown_event(GMouseEvent& event)
{
    set_cursor(text_position_at(event.position()));
}

int GTextEditor::ruler_width() const
{
    // FIXME: Resize based on needed space.
    return 5 * font().glyph_width('x') + 4;
}

Rect GTextEditor::ruler_content_rect(int line_index) const
{
    return {
        0 - ruler_width() - padding() + m_horizontal_scrollbar->value(),
        line_index * line_height(),
        ruler_width(),
        line_height()
    };
}

void GTextEditor::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::White);

    Rect ruler_rect { 0, 0, ruler_width(), height() - m_horizontal_scrollbar->height()};
    painter.fill_rect(ruler_rect, Color::LightGray);
    painter.draw_line(ruler_rect.top_right(), ruler_rect.bottom_right(), Color::DarkGray);

    painter.translate(-m_horizontal_scrollbar->value(), -m_vertical_scrollbar->value());
    painter.translate(padding() + ruler_width(), padding());
    int exposed_width = max(content_width(), width());

    int first_visible_line = text_position_at(event.rect().top_left()).line();
    int last_visible_line = text_position_at(event.rect().bottom_right()).line();

    painter.set_font(Font::default_font());
    for (int i = first_visible_line; i <= last_visible_line; ++i) {
        bool is_current_line = i == m_cursor.line();
        auto ruler_line_rect = ruler_content_rect(i);
        //painter.fill_rect(ruler_line_rect, Color::LightGray);
        Color text_color = Color::MidGray;
        if (is_current_line) {
            painter.set_font(Font::default_bold_font());
            text_color = Color::DarkGray;
        }
        painter.draw_text(ruler_line_rect.shrunken(2, 0), String::format("%u", i), TextAlignment::CenterRight, text_color);
        if (is_current_line)
            painter.set_font(Font::default_font());
    }
    painter.set_font(font());

    painter.set_clip_rect({ ruler_rect.right() + 1, 0, width() - m_vertical_scrollbar->width() - ruler_width(), height() - m_horizontal_scrollbar->height() });

    for (int i = first_visible_line; i <= last_visible_line; ++i) {
        auto& line = *m_lines[i];
        auto line_rect = line_content_rect(i);
        line_rect.set_width(exposed_width);
        if (i == m_cursor.line())
            painter.fill_rect(line_rect, Color(230, 230, 230));
        painter.draw_text(line_rect, line.characters(), line.length(), TextAlignment::CenterLeft, Color::Black);
    }

    if (is_focused() && m_cursor_state)
        painter.fill_rect(cursor_content_rect(), Color::Red);

    painter.clear_clip_rect();
    painter.set_clip_rect(event.rect());

    painter.translate(0 - padding() - ruler_width(), -padding());
    painter.translate(m_horizontal_scrollbar->value(), m_vertical_scrollbar->value());
    painter.fill_rect({ m_horizontal_scrollbar->relative_rect().top_right().translated(1, 0), { m_vertical_scrollbar->preferred_size().width(), m_horizontal_scrollbar->preferred_size().height() } }, Color::LightGray);

    if (is_focused()) {
        Rect item_area_rect { 0, 0, width() - m_vertical_scrollbar->width(), height() - m_horizontal_scrollbar->height() };
        painter.draw_rect(item_area_rect, Color::from_rgb(0x84351a));
    };
}

void GTextEditor::keydown_event(GKeyEvent& event)
{
    if (!event.modifiers() && event.key() == KeyCode::Key_Up) {
        if (m_cursor.line() > 0) {
            int new_line = m_cursor.line() - 1;
            int new_column = min(m_cursor.column(), m_lines[new_line]->length());
            set_cursor(new_line, new_column);
        }
        return;
    }
    if (!event.modifiers() && event.key() == KeyCode::Key_Down) {
        if (m_cursor.line() < (m_lines.size() - 1)) {
            int new_line = m_cursor.line() + 1;
            int new_column = min(m_cursor.column(), m_lines[new_line]->length());
            set_cursor(new_line, new_column);
        }
        return;
    }
    if (!event.modifiers() && event.key() == KeyCode::Key_PageUp) {
        if (m_cursor.line() > 0) {
            int new_line = max(0, m_cursor.line() - visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), m_lines[new_line]->length());
            set_cursor(new_line, new_column);
        }
        return;
    }
    if (!event.modifiers() && event.key() == KeyCode::Key_PageDown) {
        if (m_cursor.line() < (m_lines.size() - 1)) {
            int new_line = min(line_count() - 1, m_cursor.line() + visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), m_lines[new_line]->length());
            set_cursor(new_line, new_column);
        }
        return;
    }
    if (!event.modifiers() && event.key() == KeyCode::Key_Left) {
        if (m_cursor.column() > 0) {
            int new_column = m_cursor.column() - 1;
            set_cursor(m_cursor.line(), new_column);
        }
        return;
    }
    if (!event.modifiers() && event.key() == KeyCode::Key_Right) {
        if (m_cursor.column() < current_line().length()) {
            int new_column = m_cursor.column() + 1;
            set_cursor(m_cursor.line(), new_column);
        }
        return;
    }
    if (!event.modifiers() && event.key() == KeyCode::Key_Home) {
        set_cursor(m_cursor.line(), 0);
        return;
    }
    if (!event.modifiers() && event.key() == KeyCode::Key_End) {
        set_cursor(m_cursor.line(), current_line().length());
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_Home) {
        set_cursor(0, 0);
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        set_cursor(line_count() - 1, m_lines[line_count() - 1]->length());
        return;
    }

    if (!event.modifiers() && event.key() == KeyCode::Key_Backspace) {
        if (m_cursor.column() > 0) {
            // Backspace within line
            current_line().remove(m_cursor.column() - 1);
            update_scrollbar_ranges();
            set_cursor(m_cursor.line(), m_cursor.column() - 1);
            return;
        }
        if (m_cursor.column() == 0 && m_cursor.line() != 0) {
            // Backspace at column 0; merge with previous line
            auto& previous_line = *m_lines[m_cursor.line() - 1];
            int previous_length = previous_line.length();
            previous_line.append(current_line().characters(), current_line().length());
            m_lines.remove(m_cursor.line());
            update_scrollbar_ranges();
            update();
            set_cursor(m_cursor.line() - 1, previous_length);
            return;
        }
        return;
    }

    if (!event.modifiers() && event.key() == KeyCode::Key_Delete) {
        if (m_cursor.column() < current_line().length()) {
            // Delete within line
            current_line().remove(m_cursor.column());
            update_scrollbar_ranges();
            update_cursor();
            return;
        }
        if (m_cursor.column() == current_line().length() && m_cursor.line() != line_count() - 1) {
            // Delete at end of line; merge with next line
            auto& next_line = *m_lines[m_cursor.line() + 1];
            int previous_length = current_line().length();
            current_line().append(next_line.characters(), next_line.length());
            m_lines.remove(m_cursor.line() + 1);
            update_scrollbar_ranges();
            update();
            set_cursor(m_cursor.line(), previous_length);
            return;
        }
        return;
    }

    if (!event.text().is_empty())
        insert_at_cursor(event.text()[0]);

    return GWidget::keydown_event(event);
}

void GTextEditor::insert_at_cursor(char ch)
{
    bool at_head = m_cursor.column() == 0;
    bool at_tail = m_cursor.column() == current_line().length();
    if (ch == '\n') {
        if (at_tail || at_head) {
            m_lines.insert(m_cursor.line() + (at_tail ? 1 : 0), make<Line>());
            update_scrollbar_ranges();
            update();
            set_cursor(m_cursor.line() + 1, 0);
            return;
        }
        auto new_line = make<Line>();
        new_line->append(current_line().characters() + m_cursor.column(), current_line().length() - m_cursor.column());
        current_line().truncate(m_cursor.column());
        m_lines.insert(m_cursor.line() + 1, move(new_line));
        update_scrollbar_ranges();
        update();
        set_cursor(m_cursor.line() + 1, 0);
        return;
    }
    current_line().insert(m_cursor.column(), ch);
    update_scrollbar_ranges();
    set_cursor(m_cursor.line(), m_cursor.column() + 1);
    update_cursor();
}

Rect GTextEditor::visible_content_rect() const
{
    return {
        m_horizontal_scrollbar->value(),
        m_vertical_scrollbar->value(),
        width() - m_vertical_scrollbar->width() - padding() * 2 - ruler_width(),
        height() - m_horizontal_scrollbar->height() - padding() * 2
    };
}

Rect GTextEditor::cursor_content_rect() const
{
    if (!m_cursor.is_valid())
        return { };
    ASSERT(!m_lines.is_empty());
    ASSERT(m_cursor.column() <= (current_line().length() + 1));
    return { m_cursor.column() * glyph_width(), m_cursor.line() * line_height(), 1, line_height() };
}

Rect GTextEditor::line_widget_rect(int line_index) const
{
    ASSERT(m_horizontal_scrollbar);
    ASSERT(m_vertical_scrollbar);
    auto rect = line_content_rect(line_index);
    rect.move_by(-(m_horizontal_scrollbar->value() - padding()), -(m_vertical_scrollbar->value() - padding()));
    rect.set_width(rect.width() + 1); // Add 1 pixel for when the cursor is on the end.
    rect.intersect(this->rect());
    // This feels rather hackish, but extend the rect to the edge of the content view:
    rect.set_right(m_vertical_scrollbar->relative_rect().left() - 1);
    return rect;
}

void GTextEditor::scroll_cursor_into_view()
{
    auto visible_content_rect = this->visible_content_rect();
    auto rect = cursor_content_rect();

    if (visible_content_rect.is_empty())
        return;

    if (visible_content_rect.contains(rect))
        return;

    if (rect.top() < visible_content_rect.top())
        m_vertical_scrollbar->set_value(rect.top());
    else if (rect.bottom() > visible_content_rect.bottom())
        m_vertical_scrollbar->set_value(rect.bottom() - visible_content_rect.height());

    if (rect.left() < visible_content_rect.left())
        m_horizontal_scrollbar->set_value(rect.left());
    else if (rect.right() > visible_content_rect.right())
        m_horizontal_scrollbar->set_value(rect.right() - visible_content_rect.width());

    update();
}

Rect GTextEditor::line_content_rect(int line_index) const
{
    return {
        0,
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
    if (m_cursor == position)
        return;
    auto old_cursor_line_rect = line_widget_rect(m_cursor.line());
    m_cursor = position;
    m_cursor_state = true;
    scroll_cursor_into_view();
    update(old_cursor_line_rect);
    update_cursor();
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
    m_text.append(0);
}

void GTextEditor::Line::set_text(const String& text)
{
    if (text.length() == length() && !memcmp(text.characters(), characters(), length()))
        return;
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
