#include "TerminalWidget.h"
#include "Painter.h"

TerminalWidget::TerminalWidget(Widget* parent)
    : Widget(parent)
{
    setRect({ 100, 300, columns() * 8, rows() * 8 });
    m_screen = new CharacterWithAttributes[rows() * columns() * 2]; 
    for (unsigned row = 0; row < m_rows; ++row) {
        for (unsigned column = 0; column < m_columns; ++column) {
            at(row, column).character = ' ';
            at(row, column).attribute = 0x07;
        }
    }
    onReceive(String("Serenity/OS").toByteBuffer());
}

TerminalWidget::~TerminalWidget()
{
}

CharacterWithAttributes& TerminalWidget::at(unsigned row, unsigned column)
{
    ASSERT(m_screen);
    ASSERT(row < m_rows);
    ASSERT(column < m_columns);
    return m_screen[row * columns() + column];
}

void TerminalWidget::onPaint(PaintEvent&)
{
    Painter painter(*this);

    painter.fillRect({ 0, 0, width(), height() }, Color(0, 0, 0));

    char buf[2] = { 0, 0 };
    for (unsigned row = 0; row < m_rows; ++row) {
        int y = row * 8;
        for (unsigned column = 0; column < m_columns; ++column) {
            int x = column * 8;
            buf[0] = at(row, column).character;
            painter.drawText({ x, y, width(), 8 }, buf, Painter::TextAlignment::TopLeft, Color(0xa0, 0xa0, 0xa0));
        }
    }
}

void TerminalWidget::onReceive(const ByteBuffer& buffer)
{
    for (unsigned i = 0; i < buffer.size(); ++i) {
        onReceive(buffer[i]);
    }
}

void TerminalWidget::onReceive(byte ch)
{
    at(m_cursorRow, m_cursorColumn).character = ch;
    printf("%2u,%2u -> ", m_cursorRow, m_cursorColumn);
    if (++m_cursorColumn > m_columns) {
        m_cursorColumn = 0;
        if (m_cursorRow < (m_rows - 1)) {
            ++m_cursorRow;
        } else {
            // FIXME: Scroll it!
            ASSERT_NOT_REACHED();
        }
    }
    printf("%2u,%2u\n", m_cursorRow, m_cursorColumn);
}

