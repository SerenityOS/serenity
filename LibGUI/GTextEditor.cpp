#include <LibGUI/GTextEditor.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GFontDatabase.h>
#include <SharedGraphics/Painter.h>
#include <Kernel/KeyCode.h>

GTextEditor::GTextEditor(GWidget* parent)
    : GWidget(parent)
{
    set_font(GFontDatabase::the().get_by_name("Liza Thin"));

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
        Line line;
        if (line_length)
            line.set_text(text.substring(start_of_current_line, current_position - start_of_current_line));
        m_lines.append(move(line));
        start_of_current_line = current_position + 1;
    };
    int i = 0;
    for (i = 0; i < text.length(); ++i) {
        if (text[i] == '\n')
            add_line(i);
    }
    add_line(i);
    m_cursor = GTextPosition(0, 0);
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
    int excess_height = max(0, (line_count() * font().glyph_height()) - available_height);
    m_vertical_scrollbar->set_range(0, excess_height);

    int available_width = width() - m_vertical_scrollbar->width();
    int excess_width = max(0, content_width() - available_width);
    m_horizontal_scrollbar->set_range(0, excess_width);

    m_vertical_scrollbar->set_big_step(visible_content_rect().height());
}

int GTextEditor::content_width() const
{
    int max_width = 0;
    for (auto& line : m_lines)
        max_width = max(line.width(font()), max_width);
    return max_width;
}

void GTextEditor::mousedown_event(GMouseEvent& event)
{
    (void)event;
}

void GTextEditor::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::White);
    painter.translate(-m_horizontal_scrollbar->value(), -m_vertical_scrollbar->value());
    painter.translate(padding(), padding());
    int exposed_width = max(content_width(), width());

    for (int i = 0; i < line_count(); ++i) {
        auto& line = m_lines[i];
        auto line_rect = line_content_rect(i);
        painter.draw_text(line_rect, line.text(), TextAlignment::TopLeft, Color::Black);
    }

    painter.fill_rect(cursor_content_rect(), Color::Red);

    painter.translate(-padding(), -padding());
    painter.translate(m_horizontal_scrollbar->value(), m_vertical_scrollbar->value());
    painter.fill_rect({ m_horizontal_scrollbar->relative_rect().top_right().translated(1, 0), { m_vertical_scrollbar->preferred_size().width(), m_horizontal_scrollbar->preferred_size().height() } }, Color::LightGray);

    if (is_focused()) {
        Rect item_area_rect { 0, 0, width() - m_vertical_scrollbar->width(), height() - m_horizontal_scrollbar->height() };
        painter.draw_rect(item_area_rect, Color::from_rgb(0x84351a));
    };
}

void GTextEditor::keydown_event(GKeyEvent& event)
{
    if (event.key() == KeyCode::Key_Up) {
        if (m_cursor.line() > 0) {
            update_cursor();
            m_cursor.set_line(m_cursor.line() - 1);
            m_cursor.set_column(min(m_cursor.column(), m_lines[m_cursor.line()].length()));
            update_cursor();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        if (m_cursor.line() < (m_lines.size() - 1)) {
            update_cursor();
            m_cursor.set_line(m_cursor.line() + 1);
            m_cursor.set_column(min(m_cursor.column(), m_lines[m_cursor.line()].length()));
            update_cursor();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Left) {
        if (m_cursor.column() > 0) {
            update_cursor();
            m_cursor.set_column(m_cursor.column() - 1);
            update_cursor();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
        if (m_cursor.column() < (m_lines[m_cursor.line()].length())) {
            update_cursor();
            m_cursor.set_column(m_cursor.column() + 1);
            update_cursor();
        }
        return;
    }
    return GWidget::keydown_event(event);
}

Rect GTextEditor::visible_content_rect() const
{
    return {
        m_horizontal_scrollbar->value(),
        m_vertical_scrollbar->value(),
        width() - m_vertical_scrollbar->width() - padding() * 2,
        height() - m_horizontal_scrollbar->height() - padding() * 2
    };
}

Rect GTextEditor::cursor_content_rect() const
{
    ASSERT(m_cursor.is_valid());
    ASSERT(!m_lines.is_empty());
    auto& line = m_lines[m_cursor.line()];
    ASSERT(m_cursor.column() <= (line.text().length() + 1));
    int x = 0;
    for (int i = 0; i < m_cursor.column(); ++i)
        x += font().glyph_width(line.text()[i]);
    return { x, m_cursor.line() * font().glyph_height(), 1, font().glyph_height() };
}

void GTextEditor::scroll_into_view(const GTextPosition& position, Orientation orientation)
{
    auto visible_content_rect = this->visible_content_rect();
    auto rect = line_content_rect(position.line());

    if (visible_content_rect.contains(rect))
        return;

    if (orientation == Orientation::Vertical) {
        if (rect.top() < visible_content_rect.top())
            m_vertical_scrollbar->set_value(rect.top());
        else if (rect.bottom() > visible_content_rect.bottom())
            m_vertical_scrollbar->set_value(rect.bottom() - visible_content_rect.height());
    } else {
        if (rect.left() < visible_content_rect.left())
            m_horizontal_scrollbar->set_value(rect.left());
        else if (rect.right() > visible_content_rect.right())
            m_horizontal_scrollbar->set_value(rect.right() - visible_content_rect.width());
    }
}

Rect GTextEditor::line_content_rect(int line_index) const
{
    auto& line = m_lines[line_index];
    return {
        0,
        line_index * font().glyph_height(),
        line.width(font()),
        font().glyph_height()
    };
}

void GTextEditor::update_cursor()
{
    update();
}

void GTextEditor::Line::set_text(const String& text)
{
    if (text == m_text)
        return;
    m_text = text;
    m_cached_width = -1;
}

int GTextEditor::Line::width(const Font& font) const
{
    if (m_cached_width < 0)
        m_cached_width = font.width(m_text);
    return m_cached_width;
}
