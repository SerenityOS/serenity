#include "VirtualConsole.h"
#include "IO.h"
#include "StdLib.h"
#include <Kernel/Heap/kmalloc.h>
#include <AK/String.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/KeyboardDevice.h>

static u8* s_vga_buffer;
static VirtualConsole* s_consoles[6];
static int s_active_console;

void VirtualConsole::get_vga_cursor(u8& row, u8& column)
{
    u16 value;
    IO::out8(0x3d4, 0x0e);
    value = IO::in8(0x3d5) << 8;
    IO::out8(0x3d4, 0x0f);
    value |= IO::in8(0x3d5);
    row = value / columns();
    column = value % columns();
}

void VirtualConsole::flush_vga_cursor()
{
    u16 value = m_current_vga_start_address + (m_cursor_row * columns() + m_cursor_column);
    IO::out8(0x3d4, 0x0e);
    IO::out8(0x3d5, MSB(value));
    IO::out8(0x3d4, 0x0f);
    IO::out8(0x3d5, LSB(value));
}

void VirtualConsole::initialize()
{
    s_vga_buffer = (u8*)0xb8000;
    memset(s_consoles, 0, sizeof(s_consoles));
    s_active_console = -1;
}

VirtualConsole::VirtualConsole(unsigned index, InitialContents initial_contents)
    : TTY(4, index)
    , m_index(index)
{
    sprintf(m_tty_name, "/dev/tty%u", m_index);
    set_size(80, 25);
    m_horizontal_tabs = static_cast<u8*>(kmalloc_eternal(columns()));
    for (unsigned i = 0; i < columns(); ++i)
        m_horizontal_tabs[i] = (i % 8) == 0;
    // Rightmost column is always last tab on line.
    m_horizontal_tabs[columns() - 1] = 1;

    s_consoles[index] = this;
    m_buffer = (u8*)kmalloc_eternal(rows() * columns() * 2);
    if (initial_contents == AdoptCurrentVGABuffer) {
        memcpy(m_buffer, s_vga_buffer, rows() * columns() * 2);
        get_vga_cursor(m_cursor_row, m_cursor_column);
    } else {
        u16* line_mem = reinterpret_cast<u16*>(m_buffer);
        for (u16 i = 0; i < rows() * columns(); ++i)
            line_mem[i] = 0x0720;
    }
}

VirtualConsole::~VirtualConsole()
{
    ASSERT_NOT_REACHED();
}

void VirtualConsole::clear()
{
    u16* linemem = m_active ? (u16*)s_vga_buffer : (u16*)m_buffer;
    for (u16 i = 0; i < rows() * columns(); ++i)
        linemem[i] = 0x0720;
    if (m_active)
        set_vga_start_row(0);
    set_cursor(0, 0);
}

void VirtualConsole::switch_to(unsigned index)
{
    if ((int)index == s_active_console)
        return;
    ASSERT(index < 6);
    ASSERT(s_consoles[index]);

    InterruptDisabler disabler;
    if (s_active_console != -1) {
        auto* active_console = s_consoles[s_active_console];
        // We won't know how to switch away from a graphical console until we
        // can set the video mode on our own. Just stop anyone from trying for
        // now.
        if (active_console->is_graphical())
            return;
        active_console->set_active(false);
    }
    dbgprintf("VC: Switch to %u (%p)\n", index, s_consoles[index]);
    s_active_console = index;
    s_consoles[s_active_console]->set_active(true);
    Console::the().set_implementation(s_consoles[s_active_console]);
}

void VirtualConsole::set_active(bool b)
{
    if (b == m_active)
        return;

    InterruptDisabler disabler;

    m_active = b;
    if (!m_active) {
        memcpy(m_buffer, m_current_vga_window, rows() * columns() * 2);
        KeyboardDevice::the().set_client(nullptr);
        return;
    }

    memcpy(s_vga_buffer, m_buffer, rows() * columns() * 2);
    set_vga_start_row(0);
    flush_vga_cursor();

    KeyboardDevice::the().set_client(this);
}

inline bool is_valid_parameter_character(u8 ch)
{
    return ch >= 0x30 && ch <= 0x3f;
}

inline bool is_valid_intermediate_character(u8 ch)
{
    return ch >= 0x20 && ch <= 0x2f;
}

inline bool is_valid_final_character(u8 ch)
{
    return ch >= 0x40 && ch <= 0x7e;
}

enum class VGAColor : u8 {
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

enum class ANSIColor : u8 {
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
    case ANSIColor::Black:
        return VGAColor::Black;
    case ANSIColor::Red:
        return VGAColor::Red;
    case ANSIColor::Brown:
        return VGAColor::Brown;
    case ANSIColor::Blue:
        return VGAColor::Blue;
    case ANSIColor::Magenta:
        return VGAColor::Magenta;
    case ANSIColor::Green:
        return VGAColor::Green;
    case ANSIColor::Cyan:
        return VGAColor::Cyan;
    case ANSIColor::LightGray:
        return VGAColor::LightGray;
    case ANSIColor::DarkGray:
        return VGAColor::DarkGray;
    case ANSIColor::BrightRed:
        return VGAColor::BrightRed;
    case ANSIColor::BrightGreen:
        return VGAColor::BrightGreen;
    case ANSIColor::Yellow:
        return VGAColor::Yellow;
    case ANSIColor::BrightBlue:
        return VGAColor::BrightBlue;
    case ANSIColor::BrightMagenta:
        return VGAColor::BrightMagenta;
    case ANSIColor::BrightCyan:
        return VGAColor::BrightCyan;
    case ANSIColor::White:
        return VGAColor::White;
    }
    ASSERT_NOT_REACHED();
    return VGAColor::LightGray;
}

static inline u8 ansi_color_to_vga(u8 color)
{
    return (u8)ansi_color_to_vga((ANSIColor)color);
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

void VirtualConsole::escape$A(const Vector<unsigned>& params)
{
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    int new_row = (int)m_cursor_row - num;
    if (new_row < 0)
        new_row = 0;
    set_cursor(new_row, m_cursor_column);
}

void VirtualConsole::escape$D(const Vector<unsigned>& params)
{
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    int new_column = (int)m_cursor_column - num;
    if (new_column < 0)
        new_column = 0;
    set_cursor(m_cursor_row, new_column);
}

void VirtualConsole::escape$J(const Vector<unsigned>& params)
{
    int mode = 0;
    if (params.size() >= 1)
        mode = params[0];
    switch (mode) {
    case 0:
        // FIXME: Clear from cursor to end of screen.
        ASSERT_NOT_REACHED();
        break;
    case 1:
        // FIXME: Clear from cursor to beginning of screen.
        ASSERT_NOT_REACHED();
        break;
    case 2:
        clear();
        break;
    case 3:
        // FIXME: <esc>[3J should also clear the scrollback buffer.
        clear();
        break;
    }
}

void VirtualConsole::execute_escape_sequence(u8 final)
{
    auto paramparts = String::copy(m_parameters).split(';');
    Vector<unsigned> params;
    for (auto& parampart : paramparts) {
        bool ok;
        unsigned value = parampart.to_uint(ok);
        if (!ok) {
            // FIXME: Should we do something else?
            return;
        }
        params.append(value);
    }
    switch (final) {
    case 'A':
        escape$A(params);
        break;
    case 'D':
        escape$D(params);
        break;
    case 'H':
        escape$H(params);
        break;
    case 'J':
        escape$J(params);
        break;
    case 'm':
        escape$m(params);
        break;
    case 's':
        escape$s(params);
        break;
    case 'u':
        escape$u(params);
        break;
    default:
        break;
    }

    m_parameters.clear();
    m_intermediates.clear();
}

void VirtualConsole::clear_vga_row(u16 row)
{
    u16* linemem = (u16*)&m_current_vga_window[row * 160];
    for (u16 i = 0; i < columns(); ++i)
        linemem[i] = 0x0720;
}

void VirtualConsole::scroll_up()
{
    if (m_cursor_row == (rows() - 1)) {
        if (m_active) {
            if (m_vga_start_row >= 160) {
                memcpy(s_vga_buffer, m_current_vga_window + 160, (rows() - 1) * columns() * 2);
                set_vga_start_row(0);
                clear_vga_row(24);
            } else {
                set_vga_start_row(m_vga_start_row + 1);
                clear_vga_row(24);
            }
        } else {
            memmove(m_buffer, m_buffer + 160, 160 * 24);
            u16* linemem = (u16*)&m_buffer[24 * 160];
            for (u16 i = 0; i < columns(); ++i)
                linemem[i] = 0x0720;
        }
    } else {
        ++m_cursor_row;
    }
    m_cursor_column = 0;
}

void VirtualConsole::set_cursor(unsigned row, unsigned column)
{
    ASSERT(row < rows());
    ASSERT(column < columns());
    m_cursor_row = row;
    m_cursor_column = column;
    if (m_active)
        flush_vga_cursor();
}

void VirtualConsole::put_character_at(unsigned row, unsigned column, u8 ch)
{
    ASSERT(row < rows());
    ASSERT(column < columns());
    u16 cur = (row * 160) + (column * 2);
    if (m_active) {
        u16 cur = (row * 160) + (column * 2);
        m_current_vga_window[cur] = ch;
        m_current_vga_window[cur + 1] = m_current_attribute;
    } else {
        m_buffer[cur] = ch;
        m_buffer[cur + 1] = m_current_attribute;
    }
}

void VirtualConsole::on_char(u8 ch)
{
    // ignore writes in graphical mode
    if (m_graphical)
        return;

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
        [[fallthrough]];
    case ExpectIntermediate:
        if (is_valid_intermediate_character(ch)) {
            m_intermediates.append(ch);
            return;
        }
        m_escape_state = ExpectFinal;
        [[fallthrough]];
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
    case '\a':
        // FIXME: Bell!
        return;
    case '\t': {
        for (unsigned i = m_cursor_column; i < columns(); ++i) {
            if (m_horizontal_tabs[i]) {
                set_cursor(m_cursor_row, i);
                return;
            }
        }
        return;
    }
    case '\n':
        scroll_up();
        set_cursor(m_cursor_row, m_cursor_column);
        return;
    }

    put_character_at(m_cursor_row, m_cursor_column, ch);

    ++m_cursor_column;
    if (m_cursor_column >= columns())
        scroll_up();
    set_cursor(m_cursor_row, m_cursor_column);
}

void VirtualConsole::on_key_pressed(KeyboardDevice::Event key)
{
    // ignore keyboard in graphical mode
    if (m_graphical)
        return;

    if (!key.is_press())
        return;
    if (key.ctrl()) {
        if (key.character >= 'a' && key.character <= 'z') {
            emit(key.character - 'a' + 1);
            return;
        } else if (key.character == '\\') {
            emit(0x1c);
            return;
        }
    }
    emit(key.character);
}

void VirtualConsole::on_sysconsole_receive(u8 ch)
{
    InterruptDisabler disabler;
    auto old_attribute = m_current_attribute;
    m_current_attribute = 0x03;
    on_char(ch);
    m_current_attribute = old_attribute;
}

ssize_t VirtualConsole::on_tty_write(const u8* data, ssize_t size)
{
    InterruptDisabler disabler;
    for (ssize_t i = 0; i < size; ++i)
        on_char(data[i]);
    return size;
}

StringView VirtualConsole::tty_name() const
{
    return m_tty_name;
}

void VirtualConsole::echo(u8 ch)
{
    if (should_echo_input()) {
        on_tty_write(&ch, 1);
    }
}

void VirtualConsole::set_vga_start_row(u16 row)
{
    m_vga_start_row = row;
    m_current_vga_start_address = row * columns();
    m_current_vga_window = s_vga_buffer + row * 160;
    IO::out8(0x3d4, 0x0c);
    IO::out8(0x3d5, MSB(m_current_vga_start_address));
    IO::out8(0x3d4, 0x0d);
    IO::out8(0x3d5, LSB(m_current_vga_start_address));
}
