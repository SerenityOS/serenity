#include "Console.h"
#include "VGA.h"
#include "IO.h"
#include "kprintf.h"
#include <AK/String.h>

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

bool Console::hasDataAvailableForRead() const
{
    return false;
}

ssize_t Console::read(byte* buffer, size_t bufferSize)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    //        A generalized ring buffer would probably be useful.
    return 0;
}

inline bool isParameter(byte ch)
{
    return ch >= 0x30 && ch <= 0x3f;
}

inline bool isIntermediate(byte ch)
{
    return ch >= 0x20 && ch <= 0x2f;
}

inline bool isFinal(byte ch)
{
    return ch >= 0x40 && ch <= 0x7e;
}

unsigned parseUInt(const String& str, bool& ok)
{
    unsigned value = 0;
    for (size_t i = 0; i < str.length(); ++i) {
        if (str[i] < '0' || str[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += str[i] - '0';
    }
    ok = true;
    return value;
}

void Console::escape$H(const Vector<unsigned>& params)
{
    unsigned row = 1;
    unsigned col = 1;

    if (params.size() >= 1)
        row = params[0];
    if (params.size() >= 2)
        col = params[1];
    m_cursorRow = row - 1;
    m_cursorColumn = col - 1;
    vga_set_cursor(row - 1, col - 1);
}

void Console::escape$J(const Vector<unsigned>& params)
{
    int mode = 0;
    if (params.size() >= 1)
        mode = params[0];
    switch (mode) {
    case 0:
        // FIXME: Clear from cursor to end of screen.
        notImplemented();
        break;
    case 1:
        // FIXME: Clear from cursor to beginning of screen.
        notImplemented();
        break;
    case 2:
        vga_clear();
        break;
    case 3:
        // FIXME: <esc>[3J should also clear the scrollback buffer.
        vga_clear();
        break;
    }
}

void Console::executeEscapeSequence(byte final)
{
    auto paramparts = String((const char*)m_parameters.data(), m_parameters.size()).split(';');
    Vector<unsigned> params;
    for (auto& parampart : paramparts) {
        bool ok;
        unsigned value = parseUInt(parampart, ok);
        if (!ok) {
            // FIXME: Should we do something else?
            return;
        }
        params.append(value);
    }
    switch (final) {
    case 'H': escape$H(params); break;
    case 'J': escape$J(params); break;
    default: break;
    }

    m_parameters.clear();
    m_intermediates.clear();
}

void Console::putChar(char ch)
{
#ifdef CONSOLE_OUT_TO_E9
    //if (ch != 27)
    IO::out8(0xe9, ch);
#endif
    auto scrollup = [&] {
        if (m_cursorRow == (m_rows - 1)) {
            vga_scroll_up();
        } else {
            ++m_cursorRow;
        }
        m_cursorColumn = 0;
    };

    switch (m_escState) {
    case ExpectBracket:
        if (ch == '[')
            m_escState = ExpectParameter;
        else
            m_escState = Normal;
        return;
    case ExpectParameter:
        if (isParameter(ch)) {
            m_parameters.append(ch);
            return;
        }
        m_escState = ExpectIntermediate;
        // fall through
    case ExpectIntermediate:
        if (isIntermediate(ch)) {
            m_intermediates.append(ch);
            return;
        }
        m_escState = ExpectFinal;
        // fall through
    case ExpectFinal:
        if (isFinal(ch)) {
            m_escState = Normal;
            executeEscapeSequence(ch);
            return;
        }
        m_escState = Normal;
        return;
    case Normal:
        break;
    }

    switch (ch) {
    case '\0':
        return;
    case '\033':
        m_escState = ExpectBracket;
        return;
    case '\n':
        scrollup();
        vga_set_cursor(m_cursorRow, m_cursorColumn);
        return;
    }

    vga_putch_at(m_cursorRow, m_cursorColumn, ch);

    ++m_cursorColumn;
    if (m_cursorColumn >= m_columns)
        scrollup();
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

