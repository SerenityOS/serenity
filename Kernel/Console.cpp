#include "Console.h"
#include "VGA.h"
#include "IO.h"

// Bytes output to 0xE9 end up on the Bochs console. It's very handy.
#define CONSOLE_OUT_TO_E9

static Console* s_the;

Console& Console::the()
{
    return *s_the;
}

Console::Console()
{
    s_the = this;
}

Console::~Console()
{
}

ssize_t Console::read(byte* buffer, size_t bufferSize)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    //        A generalized ring buffer would probably be useful.
    return 0;
}

void Console::putChar(char ch)
{
#ifdef CONSOLE_OUT_TO_E9
    IO::out8(0xe9, ch);
#endif
    switch (ch) {
    case '\n':
        m_cursorColumn = 0;
        if (m_cursorRow == (m_rows - 2)) {
            vga_scroll_up();
        } else {
            ++m_cursorRow;
        }
        vga_set_cursor(m_cursorRow, m_cursorColumn);
        return;
    }

    vga_putch_at(m_cursorRow, m_cursorColumn, ch);

    ++m_cursorColumn;
    if (m_cursorColumn >= m_columns) {
        if (m_cursorRow == (m_rows - 2)) {
            vga_scroll_up();
        } else {
            ++m_cursorRow;
        }
        m_cursorColumn = 0;
    }
    vga_set_cursor(m_cursorRow, m_cursorColumn);
}

ssize_t Console::write(const byte* data, size_t size)
{
    if (!size)
        return 0;
    
    for (size_t i = 0; i < size; ++i)
        putChar(data[i]);
    return 0;
}

