#include "VirtualConsole.h"
#include "VGA.h"
#include "kmalloc.h"
#include "i386.h"
#include "StdLib.h"
#include "Keyboard.h"
#include <AK/String.h>

static byte* s_vga_buffer;
static VirtualConsole* s_consoles[6];
static int s_active_console;

void VirtualConsole::initialize()
{
    s_vga_buffer = (byte*)0xb8000;
    memset(s_consoles, 0, sizeof(s_consoles));
    s_active_console = -1;
}

VirtualConsole::VirtualConsole(unsigned index, InitialContents initial_contents)
    : TTY(4, index)
    , m_index(index)
{
    s_consoles[index] = this;
    m_buffer = (byte*)kmalloc_eternal(80 * 25 * 2);
    if (initial_contents == AdoptCurrentVGABuffer) {
        memcpy(m_buffer, s_vga_buffer, 80 * 25 * 2);
        auto vgaCursor = vga_get_cursor();
        m_cursor_row = vgaCursor / 80;
        m_cursor_column = vgaCursor % 80;
    } else {
        word* line_mem = reinterpret_cast<word*>(m_buffer);
        for (word i = 0; i < 80 * 25; ++i)
            line_mem[i] = 0x0720;
    }
}

VirtualConsole::~VirtualConsole()
{
}

void VirtualConsole::switch_to(unsigned index)
{
    if ((int)index == s_active_console)
        return;
    dbgprintf("VC: Switch to %u (%p)\n", index, s_consoles[index]);
    ASSERT(index < 6);
    ASSERT(s_consoles[index]);
    InterruptDisabler disabler;
    if (s_active_console != -1)
        s_consoles[s_active_console]->set_active(false);
    s_active_console = index;
    s_consoles[s_active_console]->set_active(true);
    Console::the().setImplementation(s_consoles[s_active_console]);
}

void VirtualConsole::set_active(bool b)
{
    if (b == m_active)
        return;

    InterruptDisabler disabler;

    m_active = b;
    if (!m_active) {
        memcpy(m_buffer, s_vga_buffer, 80 * 25 * 2);
        return;
    }

    memcpy(s_vga_buffer, m_buffer, 80 * 25 * 2);
    vga_set_cursor(m_cursor_row, m_cursor_column);

    Keyboard::the().setClient(this);
}

inline bool is_valid_parameter_character(byte ch)
{
    return ch >= 0x30 && ch <= 0x3f;
}

inline bool is_valid_intermediate_character(byte ch)
{
    return ch >= 0x20 && ch <= 0x2f;
}

inline bool is_valid_final_character(byte ch)
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

static inline VGAColor ansi_color_to_vga(ANSIColor color)
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

static inline byte ansi_color_to_vga(byte color)
{
    return (byte)ansi_color_to_vga((ANSIColor)color);
}

void VirtualConsole::escape$m(const Vector<unsigned>& params)
{
    for (auto param : params) {
        switch (param) {
        case 0:
            // Reset
            m_current_attribute = 0x07;
            break;
        case 1:
            // Bold
            m_current_attribute |= 8;
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
            m_current_attribute &= ~0x7;
            m_current_attribute |= ansi_color_to_vga(param - 30);
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
            m_current_attribute &= ~0x70;
            m_current_attribute |= ansi_color_to_vga(param - 30) << 8;
            break;
        }
    }
}

void VirtualConsole::escape$s(const Vector<unsigned>&)
{
    m_saved_cursor_row = m_cursor_row;
    m_saved_cursor_column = m_cursor_column;
}

void VirtualConsole::escape$u(const Vector<unsigned>&)
{
    set_cursor(m_saved_cursor_row, m_saved_cursor_column);
}

void VirtualConsole::escape$H(const Vector<unsigned>& params)
{
    unsigned row = 1;
    unsigned col = 1;
    if (params.size() >= 1)
        row = params[0];
    if (params.size() >= 2)
        col = params[1];
    set_cursor(row - 1, col - 1);
}

void VirtualConsole::escape$J(const Vector<unsigned>& params)
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

void VirtualConsole::execute_escape_sequence(byte final)
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

void VirtualConsole::scroll_up()
{
    if (m_cursor_row == (m_rows - 1)) {
        memcpy(m_buffer, m_buffer + 160, 160 * 24);
        word* linemem = (word*)&m_buffer[24 * 160];
        for (word i = 0; i < 80; ++i)
            linemem[i] = 0x0720;
        if (m_active)
            vga_scroll_up();
    } else {
        ++m_cursor_row;
    }
    m_cursor_column = 0;
}

void VirtualConsole::set_cursor(unsigned row, unsigned column)
{
    ASSERT(row < m_rows);
    ASSERT(column < m_columns);
    m_cursor_row = row;
    m_cursor_column = column;
    if (m_active)
        vga_set_cursor(m_cursor_row, m_cursor_column);
}

void VirtualConsole::put_character_at(unsigned row, unsigned column, byte ch)
{
    ASSERT(row < m_rows);
    ASSERT(column < m_columns);
    word cur = (row * 160) + (column * 2);
    m_buffer[cur] = ch;
    m_buffer[cur + 1] = m_current_attribute;
    if (m_active)
        vga_putch_at(row, column, ch, m_current_attribute);
}

void VirtualConsole::on_char(byte ch, bool shouldEmit)
{
    if (shouldEmit)
        emit(ch);

    switch (m_escape_state) {
    case ExpectBracket:
        if (ch == '[')
            m_escape_state = ExpectParameter;
        else
            m_escape_state = Normal;
        return;
    case ExpectParameter:
        if (is_valid_parameter_character(ch)) {
            m_parameters.append(ch);
            return;
        }
        m_escape_state = ExpectIntermediate;
        // fall through
    case ExpectIntermediate:
        if (is_valid_intermediate_character(ch)) {
            m_intermediates.append(ch);
            return;
        }
        m_escape_state = ExpectFinal;
        // fall through
    case ExpectFinal:
        if (is_valid_final_character(ch)) {
            m_escape_state = Normal;
            execute_escape_sequence(ch);
            return;
        }
        m_escape_state = Normal;
        return;
    case Normal:
        break;
    }

    switch (ch) {
    case '\0':
        return;
    case '\033':
        m_escape_state = ExpectBracket;
        return;
    case 8: // Backspace
        if (m_cursor_column) {
            set_cursor(m_cursor_row, m_cursor_column - 1);
            put_character_at(m_cursor_row, m_cursor_column, ' ');
            return;
        }
        break;
    case '\n':
        scroll_up();
        set_cursor(m_cursor_row, m_cursor_column);
        return;
    }

    put_character_at(m_cursor_row, m_cursor_column, ch);

    ++m_cursor_column;
    if (m_cursor_column >= m_columns)
        scroll_up();
    set_cursor(m_cursor_row, m_cursor_column);
}

void VirtualConsole::onKeyPress(Keyboard::Key key)
{
    if (key.ctrl() && key.character == 'C') {
        interrupt();
        return;
    }
    if (key.ctrl())
        emit('^');
    emit(key.character);
}

void VirtualConsole::onConsoleReceive(byte ch)
{
    InterruptDisabler disabler;
    auto old_attribute = m_current_attribute;
    m_current_attribute = 0x03;
    on_char(ch, false);
    m_current_attribute = old_attribute;
}

void VirtualConsole::onTTYWrite(const byte* data, size_t size)
{
    InterruptDisabler disabler;
    for (size_t i = 0; i < size; ++i)
        on_char(data[i], false);
}

String VirtualConsole::ttyName() const
{
    char buf[16];
    ksprintf(buf, "/dev/tty%u", m_index);
    return String(buf);
}
