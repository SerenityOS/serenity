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

ssize_t Console::read(byte*, size_t)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
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

enum class VGAColor : byte {
    Black = 0,
    Blue,
    Green,
    Cyan,
    Red,
    Magenta,
    Brown,
    LightGray,
    DarkGray,
    BrightBlue,
    BrightGreen,
    BrightCyan,
    BrightRed,
    BrightMagenta,
    Yellow,
    White,
};

enum class ANSIColor : byte {
    Black = 0,
    Red,
    Green,
    Brown,
    Blue,
    Magenta,
    Cyan,
    LightGray,
    DarkGray,
    BrightRed,
    BrightGreen,
    Yellow,
    BrightBlue,
    BrightMagenta,
    BrightCyan,
    White,
};

static inline VGAColor ansiColorToVGA(ANSIColor color)
{
    switch (color) {
    case ANSIColor::Black: return VGAColor::Black;
    case ANSIColor::Red: return VGAColor::Red;
    case ANSIColor::Brown: return VGAColor::Brown;
    case ANSIColor::Blue: return VGAColor::Blue;
    case ANSIColor::Magenta: return VGAColor::Magenta;
    case ANSIColor::Green: return VGAColor::Green;
    case ANSIColor::Cyan: return VGAColor::Cyan;
    case ANSIColor::LightGray: return VGAColor::LightGray;
    case ANSIColor::DarkGray: return VGAColor::DarkGray;
    case ANSIColor::BrightRed: return VGAColor::BrightRed;
    case ANSIColor::BrightGreen: return VGAColor::BrightGreen;
    case ANSIColor::Yellow: return VGAColor::Yellow;
    case ANSIColor::BrightBlue: return VGAColor::BrightBlue;
    case ANSIColor::BrightMagenta: return VGAColor::BrightMagenta;
    case ANSIColor::BrightCyan: return VGAColor::BrightCyan;
    case ANSIColor::White: return VGAColor::White;
    }
    ASSERT_NOT_REACHED();
    return VGAColor::LightGray;
}

static inline byte ansiColorToVGA(byte color)
{
    return (byte)ansiColorToVGA((ANSIColor)color);
}

void Console::escape$m(const Vector<unsigned>& params)
{
    for (auto param : params) {
        switch (param) {
        case 0:
            // Reset
            m_currentAttribute = 0x07;
            break;
        case 1:
            // Bold
            m_currentAttribute |= 8;
            break;
        case 30:
        case 31:
        case 32:
        case 33:
        case 34:
        case 35:
        case 36:
        case 37:
            // Foreground color
            m_currentAttribute &= ~0x7;
            m_currentAttribute |= ansiColorToVGA(param - 30);
            break;
        case 40:
        case 41:
        case 42:
        case 43:
        case 44:
        case 45:
        case 46:
        case 47:
            // Background color
            m_currentAttribute &= ~0x70;
            m_currentAttribute |= ansiColorToVGA(param - 30) << 8;
            break;
        }
    }
    vga_set_attr(m_currentAttribute);
}

void Console::escape$s(const Vector<unsigned>&)
{
    m_savedCursorRow = m_cursorRow;
    m_savedCursorColumn = m_cursorColumn;
}

void Console::escape$u(const Vector<unsigned>&)
{
    m_cursorRow = m_savedCursorRow;
    m_cursorColumn = m_savedCursorColumn;
    vga_set_cursor(m_cursorRow, m_cursorColumn);
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
    case 'm': escape$m(params); break;
    case 's': escape$s(params); break;
    case 'u': escape$u(params); break;
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
    case 8: // Backspace
        if (m_cursorColumn) {
            --m_cursorColumn;\
            vga_set_cursor(m_cursorRow, m_cursorColumn);
            vga_putch_at(m_cursorRow, m_cursorColumn, ' ');
            return;
        }
        break;
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

