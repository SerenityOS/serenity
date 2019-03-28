#include "GTextBox.h"
#include <AK/StdLibExtras.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <SharedGraphics/Font.h>
#include <LibGUI/GPainter.h>
#include <Kernel/KeyCode.h>

GTextBox::GTextBox(GWidget* parent)
    : GWidget(parent)
{
    set_background_color(Color::White);
}

GTextBox::~GTextBox()
{
}

void GTextBox::set_text(const String& text)
{
    if (m_text == text)
        return;
    m_text = text;
    m_cursor_position = m_text.length();
    scroll_cursor_into_view(HorizontalDirection::Right);
    update();
}

void GTextBox::scroll_cursor_into_view(HorizontalDirection direction)
{
    if (visible_content_rect().contains(cursor_content_position()))
        return;
    int total_text_width = font().width(m_text);
    dbgprintf("total_text_width = %d, visible_content width = %d\n", total_text_width, visible_content_rect().width());
    if (total_text_width < visible_content_rect().width()) {
        m_scroll_offset = 0;
        return;
    }
    if (direction == HorizontalDirection::Left) {
        dbgprintf("Left, orig offset = %d\n", m_scroll_offset);
        m_scroll_offset = cursor_content_position().x();
        int offset_into_visible = m_scroll_offset - visible_content_rect().x();
        if (offset_into_visible < font().glyph_width(' '))
            m_scroll_offset -= width() / 2;
    } else {
        m_scroll_offset = cursor_content_position().x() - visible_content_rect().width();
        dbgprintf("Right, orig offset = %d\n", m_scroll_offset);
        int offset_into_visible = m_scroll_offset - visible_content_rect().x();
        if (offset_into_visible > width() / 4) {
            dbgprintf("Right, adjust offset = %d\n", m_scroll_offset);
            m_scroll_offset += width() / 2;
        }
    }
    if (m_scroll_offset < 0)
        m_scroll_offset = 0;
    if (m_scroll_offset > total_text_width)dbgprintf("Right, adjust offset = %d\n", m_scroll_offset);
        m_scroll_offset = total_text_width - width();
}

Rect GTextBox::visible_content_rect() const
{
    if (rect().is_empty())
        return { };
    return { m_scroll_offset, 0, rect().shrunken(6, 6).width(), rect().shrunken(6, 6).height() };
}

Point GTextBox::cursor_content_position() const
{
    int x = 0;
    for (int i = 0; i < m_cursor_position; ++i)
        x += font().glyph_width(m_text[i]) + font().glyph_spacing();
    return { x, 0 };
}

void GTextBox::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.set_clip_rect(event.rect());

    painter.fill_rect(rect().shrunken(2, 2), background_color());
    painter.draw_rect(rect(), foreground_color());

    if (is_focused())
        painter.draw_focus_rect(rect());

    Rect inner_rect = rect();
    inner_rect.shrink(6, 6);

    painter.set_clip_rect(inner_rect);
    painter.translate(-m_scroll_offset, 0);

    int space_width = font().glyph_width(' ') + font().glyph_spacing();
    int x = inner_rect.x();
    int y = inner_rect.center().y() - font().glyph_height() / 2;

    for (int i = 0; i < m_text.length(); ++i) {
        char ch = m_text[i];
        if (ch == ' ') {
            x += space_width;
            continue;
        }
        painter.draw_glyph({x, y}, ch, Color::Black);
        x += font().glyph_width(ch) + font().glyph_spacing();
    }

    if (is_focused() && m_cursor_blink_state) {
        Rect cursor_rect {
            inner_rect.x() + cursor_content_position().x(),
            inner_rect.y(),
            1,
            inner_rect.height()
        };
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
        m_scroll_offset = 0;
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
    scroll_cursor_into_view(HorizontalDirection::Left);
    if (on_change)
        on_change(*this);
    update();
}

void GTextBox::keydown_event(GKeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_Left:
        if (m_cursor_position) {
            --m_cursor_position;
            scroll_cursor_into_view(HorizontalDirection::Left);
        }
        m_cursor_blink_state = true;
        update();
        return;
    case KeyCode::Key_Right:
        if (m_cursor_position < m_text.length()) {
            ++m_cursor_position;
            scroll_cursor_into_view(HorizontalDirection::Right);
        }
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
        scroll_cursor_into_view(HorizontalDirection::Right);
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

void GTextBox::resize_event(GResizeEvent&)
{
    scroll_cursor_into_view(HorizontalDirection::Right);
}
