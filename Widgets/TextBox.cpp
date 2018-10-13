#include "TextBox.h"
#include "Painter.h"
#include "Font.h"
#include "CBitmap.h"
#include <AK/StdLib.h>

TextBox::TextBox(Widget* parent)
    : Widget(parent)
{
    startTimer(500);
}

TextBox::~TextBox()
{
}

void TextBox::setText(String&& text)
{
    m_text = std::move(text);
    m_cursorPosition = m_text.length();
    update();
}

void TextBox::paintEvent(PaintEvent&)
{
    Painter painter(*this);

    // FIXME: Reduce overdraw.
    painter.fillRect(rect(), backgroundColor());
    painter.drawRect(rect(), foregroundColor());

    if (isFocused())
        painter.drawFocusRect(rect());

    Rect innerRect = rect();
    innerRect.shrink(6, 6);

    auto& font = Font::defaultFont();

    unsigned maxCharsToPaint = innerRect.width() / font.glyphWidth();

    int firstVisibleChar = max((int)m_cursorPosition - (int)maxCharsToPaint, 0);
    unsigned charsToPaint = min(m_text.length() - firstVisibleChar, maxCharsToPaint);

    int y = innerRect.center().y() - font.glyphHeight() / 2;
    for (unsigned i = 0; i < charsToPaint; ++i) {
        char ch = m_text[firstVisibleChar + i];
        if (ch == ' ')
            continue;
        int x = innerRect.x() + (i * font.glyphWidth());
        auto* bitmap = font.glyphBitmap(ch);
        if (!bitmap) {
            printf("TextBox: glyph missing: %02x\n", ch);
            ASSERT_NOT_REACHED();
        }
        painter.drawBitmap({x, y}, *bitmap, Color::Black);
    }

    if (isFocused() && m_cursorBlinkState) {
        unsigned visibleCursorPosition = m_cursorPosition - firstVisibleChar;
        Rect cursorRect(innerRect.x() + visibleCursorPosition * font.glyphWidth(), innerRect.y(), 1, innerRect.height());
        painter.fillRect(cursorRect, foregroundColor());
    }
}

void TextBox::mouseDownEvent(MouseEvent&)
{
}

void TextBox::handleBackspace()
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
    auto newText = StringImpl::createUninitialized(m_text.length() - 1, buffer);

    memcpy(buffer, m_text.characters(), m_cursorPosition - 1);
    memcpy(buffer + m_cursorPosition - 1, m_text.characters() + m_cursorPosition, m_text.length() - (m_cursorPosition - 1));

    m_text = std::move(newText);
    --m_cursorPosition;
    update();
}

void TextBox::keyDownEvent(KeyEvent& event)
{
    switch (event.key()) {
    case KeyboardKey::LeftArrow:
        if (m_cursorPosition)
            --m_cursorPosition;
        m_cursorBlinkState = true;
        update();
        return;
    case KeyboardKey::RightArrow:
        if (m_cursorPosition < m_text.length())
            ++m_cursorPosition;
        m_cursorBlinkState = true;
        update();
        return;
    case KeyboardKey::Backspace:
        return handleBackspace();
    case KeyboardKey::Return:
        if (onReturnPressed)
            onReturnPressed(*this);
        return;
    }

    if (!event.text().isEmpty()) {
        ASSERT(event.text().length() == 1);

        char* buffer;
        auto newText = StringImpl::createUninitialized(m_text.length() + 1, buffer);

        memcpy(buffer, m_text.characters(), m_cursorPosition);
        buffer[m_cursorPosition] = event.text()[0];
        memcpy(buffer + m_cursorPosition + 1, m_text.characters() + m_cursorPosition, m_text.length() - m_cursorPosition);

        m_text = std::move(newText);
        ++m_cursorPosition;
        update();
        return;
    }
}

void TextBox::timerEvent(TimerEvent&)
{
    // FIXME: Disable the timer when not focused.
    if (!isFocused())
        return;

    m_cursorBlinkState = !m_cursorBlinkState;
    update();
}
