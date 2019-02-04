#include "GTextBox.h"
#include <AK/StdLibExtras.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <SharedGraphics/Font.h>
#include <SharedGraphics/Painter.h>
#include <Kernel/KeyCode.h>

GTextBox::GTextBox(GWidget* parent)
    : GWidget(parent)
{
    set_background_color(Color::White);
}

GTextBox::~GTextBox()
{
}

void GTextBox::set_text(String&& text)
{
    m_text = move(text);
    m_cursor_position = m_text.length();
    update();
}

void GTextBox::paint_event(GPaintEvent&)
{
    Painter painter(*this);

    painter.fill_rect({ rect().x() + 1, rect().y() + 1, rect().width() - 2, rect().height() - 2 }, background_color());
    painter.draw_rect(rect(), foreground_color());

    if (is_focused())
        painter.draw_focus_rect(rect());

    Rect inner_rect = rect();
    inner_rect.shrink(6, 6);

    size_t max_chars_to_paint = inner_rect.width() / font().glyph_width();

    int first_visible_char = max((int)m_cursor_position - (int)max_chars_to_paint, 0);
    size_t chars_to_paint = min(m_text.length() - first_visible_char, max_chars_to_paint);

    int y = inner_rect.center().y() - font().glyph_height() / 2;
    for (size_t i = 0; i < chars_to_paint; ++i) {
        char ch = m_text[first_visible_char + i];
        if (ch == ' ')
            continue;
        int x = inner_rect.x() + (i * font().glyph_width());
        painter.draw_bitmap({x, y}, font().glyph_bitmap(ch), Color::Black);
    }

    if (is_focused() && m_cursor_blink_state) {
        unsigned visible_cursor_position = m_cursor_position - first_visible_char;
        Rect cursor_rect(inner_rect.x() + visible_cursor_position * font().glyph_width(), inner_rect.y(), 1, inner_rect.height());
        painter.fill_rect(cursor_rect, foreground_color());
    }
}

void GTextBox::mousedown_event(GMouseEvent&)
{
}

void GTextBox::handle_backspace()
{
    if (m_cursor_position == 0)
        return;

    if (m_text.length() == 1) {
        m_text = String::empty();
        m_cursor_position = 0;
        if (on_change)
            on_change(*this);
        update();
        return;
    }

    char* buffer;
    auto new_text = StringImpl::create_uninitialized(m_text.length() - 1, buffer);

    memcpy(buffer, m_text.characters(), m_cursor_position - 1);
    memcpy(buffer + m_cursor_position - 1, m_text.characters() + m_cursor_position, m_text.length() - (m_cursor_position - 1));

    m_text = move(new_text);
    --m_cursor_position;
    if (on_change)
        on_change(*this);
    update();
}

void GTextBox::keydown_event(GKeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_Left:
        if (m_cursor_position)
            --m_cursor_position;
        m_cursor_blink_state = true;
        update();
        return;
    case KeyCode::Key_Right:
        if (m_cursor_position < m_text.length())
            ++m_cursor_position;
        m_cursor_blink_state = true;
        update();
        return;
    case KeyCode::Key_Backspace:
        return handle_backspace();
    case KeyCode::Key_Return:
        if (on_return_pressed)
            on_return_pressed(*this);
        return;
    }

    if (!event.text().is_empty()) {
        ASSERT(event.text().length() == 1);

        char* buffer;
        auto new_text = StringImpl::create_uninitialized(m_text.length() + 1, buffer);

        memcpy(buffer, m_text.characters(), m_cursor_position);
        buffer[m_cursor_position] = event.text()[0];
        memcpy(buffer + m_cursor_position + 1, m_text.characters() + m_cursor_position, m_text.length() - m_cursor_position);

        m_text = move(new_text);
        ++m_cursor_position;
        if (on_change)
            on_change(*this);
        update();
        return;
    }
}

void GTextBox::timer_event(GTimerEvent&)
{
    // FIXME: Disable the timer when not focused.
    if (!is_focused())
        return;

    m_cursor_blink_state = !m_cursor_blink_state;
    update();
}

void GTextBox::focusin_event(GEvent&)
{
    start_timer(500);
}

void GTextBox::focusout_event(GEvent&)
{
    stop_timer();
}
