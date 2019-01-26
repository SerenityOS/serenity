#include "GTextBox.h"
#include <AK/StdLibExtras.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <SharedGraphics/Font.h>
#include <SharedGraphics/Painter.h>
#include <Kernel/KeyCode.h>

GTextBox::GTextBox(GWidget* parent)
    : GWidget(parent)
{
    startTimer(500);
}

GTextBox::~GTextBox()
{
}

void GTextBox::set_text(String&& text)
{
    m_text = move(text);
    m_cursorPosition = m_text.length();
    update();
}

void GTextBox::paint_event(GPaintEvent&)
{
    Painter painter(*this);

    // FIXME: Reduce overdraw.
    painter.fill_rect(rect(), background_color());
    painter.draw_rect(rect(), foreground_color());

    if (is_focused())
        painter.draw_focus_rect(rect());

    Rect innerRect = rect();
    innerRect.shrink(6, 6);

    size_t maxCharsToPaint = innerRect.width() / font().glyph_width();

    int firstVisibleChar = max((int)m_cursorPosition - (int)maxCharsToPaint, 0);
    size_t charsToPaint = min(m_text.length() - firstVisibleChar, maxCharsToPaint);

    int y = innerRect.center().y() - font().glyph_height() / 2;
    for (size_t i = 0; i < charsToPaint; ++i) {
        char ch = m_text[firstVisibleChar + i];
        if (ch == ' ')
            continue;
        int x = innerRect.x() + (i * font().glyph_width());
        auto* bitmap = font().glyph_bitmap(ch);
        if (!bitmap) {
            dbgprintf("GTextBox: glyph missing: %02x\n", ch);
            ASSERT_NOT_REACHED();
        }
        painter.draw_bitmap({x, y}, *bitmap, Color::Black);
    }

    if (is_focused() && m_cursorBlinkState) {
        unsigned visibleCursorPosition = m_cursorPosition - firstVisibleChar;
        Rect cursorRect(innerRect.x() + visibleCursorPosition * font().glyph_width(), innerRect.y(), 1, innerRect.height());
        painter.fill_rect(cursorRect, foreground_color());
    }
}

void GTextBox::mousedown_event(GMouseEvent&)
{
}

void GTextBox::handle_backspace()
{
    if (m_cursorPosition == 0)
        return;

    if (m_text.length() == 1) {
        m_text = String::empty();
        m_cursorPosition = 0;
        update();
        return;
    }

    char* buffer;
    auto newText = StringImpl::create_uninitialized(m_text.length() - 1, buffer);

    memcpy(buffer, m_text.characters(), m_cursorPosition - 1);
    memcpy(buffer + m_cursorPosition - 1, m_text.characters() + m_cursorPosition, m_text.length() - (m_cursorPosition - 1));

    m_text = move(newText);
    --m_cursorPosition;
    update();
}

void GTextBox::keydown_event(GKeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_Left:
        if (m_cursorPosition)
            --m_cursorPosition;
        m_cursorBlinkState = true;
        update();
        return;
    case KeyCode::Key_Right:
        if (m_cursorPosition < m_text.length())
            ++m_cursorPosition;
        m_cursorBlinkState = true;
        update();
        return;
    case KeyCode::Key_Backspace:
        return handle_backspace();
    case KeyCode::Key_Return:
        if (onReturnPressed)
            onReturnPressed(*this);
        return;
    }

    if (!event.text().is_empty()) {
        ASSERT(event.text().length() == 1);

        char* buffer;
        auto newText = StringImpl::create_uninitialized(m_text.length() + 1, buffer);

        memcpy(buffer, m_text.characters(), m_cursorPosition);
        buffer[m_cursorPosition] = event.text()[0];
        memcpy(buffer + m_cursorPosition + 1, m_text.characters() + m_cursorPosition, m_text.length() - m_cursorPosition);

        m_text = move(newText);
        ++m_cursorPosition;
        update();
        return;
    }
}

void GTextBox::timerEvent(GTimerEvent&)
{
    // FIXME: Disable the timer when not focused.
    if (!is_focused())
        return;

    m_cursorBlinkState = !m_cursorBlinkState;
    update();
}
