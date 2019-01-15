#include "Terminal.h"
#include <AK/AKString.h>
#include <Widgets/Font.h>
#include <Widgets/Painter.h>
#include <unistd.h>
#include <stdio.h>
#include <gui.h>

void Terminal::create_window()
{
    auto& font = Font::defaultFont();

    m_pixel_width = m_columns * font.glyphWidth();
    m_pixel_height = m_rows * font.glyphHeight();

    GUI_CreateWindowParameters params;
    params.rect = { { 300, 300 }, { m_pixel_width, m_pixel_height } };
    params.background_color = 0x000000;
    strcpy(params.title, "Terminal");
    m_window_id = gui_create_window(&params);
    ASSERT(m_window_id > 0);
    if (m_window_id < 0) {
        perror("gui_create_window");
        exit(1);
    }

    GUI_WindowBackingStoreInfo info;
    int rc = gui_get_window_backing_store(m_window_id, &info);
    if (rc < 0) {
        perror("gui_get_window_backing_store");
        exit(1);
    }

    m_backing = GraphicsBitmap::create_wrapper(info.size, info.pixels);
    dbgprintf("(Terminal:%d) window backing %ux%u @ %p\n", getpid(), info.size.width, info.size.height, info.pixels);

}

Terminal::Terminal()
{
    set_size(80, 25);
    m_horizontal_tabs = static_cast<byte*>(malloc(columns()));
    for (unsigned i = 0; i < columns(); ++i)
        m_horizontal_tabs[i] = (i % 8) == 0;
    // Rightmost column is always last tab on line.
    m_horizontal_tabs[columns() - 1] = 1;

    m_buffer = (byte*)malloc(rows() * columns() * 2);
    word* line_mem = reinterpret_cast<word*>(m_buffer);
    for (word i = 0; i < rows() * columns(); ++i)
        line_mem[i] = 0x0720;

    inject_string_at(2, 2, "I am text inside the Terminal buffer.");
}

void Terminal::inject_string_at(word row, word column, const String& string)
{
    for (size_t i = 0; i < string.length(); ++i) {
        put_character_at(row, column + i, string[i]);
    }
}

Terminal::~Terminal()
{
    kfree(m_horizontal_tabs);
    m_horizontal_tabs = nullptr;
}

void Terminal::clear()
{
    word* linemem = (word*)m_buffer;
    for (word i = 0; i < rows() * columns(); ++i)
        linemem[i] = 0x0720;
    set_cursor(0, 0);
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

void Terminal::escape$m(const Vector<unsigned>& params)
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

void Terminal::escape$s(const Vector<unsigned>&)
{
    m_saved_cursor_row = m_cursor_row;
    m_saved_cursor_column = m_cursor_column;
}

void Terminal::escape$u(const Vector<unsigned>&)
{
    set_cursor(m_saved_cursor_row, m_saved_cursor_column);
}

void Terminal::escape$H(const Vector<unsigned>& params)
{
    unsigned row = 1;
    unsigned col = 1;
    if (params.size() >= 1)
        row = params[0];
    if (params.size() >= 2)
        col = params[1];
    set_cursor(row - 1, col - 1);
}

void Terminal::escape$A(const Vector<unsigned>& params)
{
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    int new_row = (int)m_cursor_row - num;
    if (new_row < 0)
        new_row = 0;
    set_cursor(new_row, m_cursor_column);
}

void Terminal::escape$D(const Vector<unsigned>& params)
{
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    int new_column = (int)m_cursor_column - num;
    if (new_column < 0)
        new_column = 0;
    set_cursor(m_cursor_row, new_column);
}

void Terminal::escape$J(const Vector<unsigned>& params)
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
        clear();
        break;
    case 3:
        // FIXME: <esc>[3J should also clear the scrollback buffer.
        clear();
        break;
    }
}

void Terminal::execute_escape_sequence(byte final)
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
    case 'A': escape$A(params); break;
    case 'D': escape$D(params); break;
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

void Terminal::scroll_up()
{
    if (m_cursor_row == (rows() - 1)) {
        memcpy(m_buffer, m_buffer + 160, 160 * 24);
        word* linemem = (word*)&m_buffer[24 * 160];
        for (word i = 0; i < columns(); ++i)
            linemem[i] = 0x0720;
    } else {
        ++m_cursor_row;
    }
    m_cursor_column = 0;
}

void Terminal::set_cursor(unsigned row, unsigned column)
{
    ASSERT(row < rows());
    ASSERT(column < columns());
    m_cursor_row = row;
    m_cursor_column = column;
}

void Terminal::put_character_at(unsigned row, unsigned column, byte ch)
{
    ASSERT(row < rows());
    ASSERT(column < columns());
    word cur = (row * 160) + (column * 2);
    m_buffer[cur] = ch;
    m_buffer[cur + 1] = m_current_attribute;
}

void Terminal::on_char(byte ch)
{
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

void Terminal::set_size(word columns, word rows)
{
    m_columns = columns;
    m_rows = rows;
}

void Terminal::paint()
{
    Rect rect { 0, 0, m_pixel_width, m_pixel_height };
    Font& font = Font::defaultFont();
    Painter painter(*m_backing);
    painter.fill_rect(rect, Color::Black);

    for (word row = 0; row < m_rows; ++row) {
        int y = row * font.glyphHeight();
        for (word column = 0; column < m_columns; ++column) {
            char ch = m_buffer[(row * 160) + (column * 2)];
            if (ch == ' ')
                continue;
            int x = column * font.glyphWidth();
            painter.draw_glyph({ x + 2, y + 2 }, ch, Color(0xa0, 0xa0, 0xa0));
        }
    }

    if (m_belling)
        painter.draw_rect(rect, Color::Red);

    int rc = gui_invalidate_window(m_window_id);
    if (rc < 0) {
        perror("gui_invalidate_window");
        exit(1);
    }
}
