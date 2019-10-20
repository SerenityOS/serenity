#pragma once

#include "Console.h"
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/TTY/TTY.h>

class VirtualConsole final : public TTY
    , public KeyboardClient
    , public ConsoleImplementation {
    AK_MAKE_ETERNAL
public:
    enum InitialContents {
        Cleared,
        AdoptCurrentVGABuffer
    };

    VirtualConsole(unsigned index, InitialContents = Cleared);
    virtual ~VirtualConsole() override;

    static void switch_to(unsigned);
    static void initialize();

    bool is_graphical() { return m_graphical; }
    void set_graphical(bool graphical) { m_graphical = graphical; }

private:
    // ^KeyboardClient
    virtual void on_key_pressed(KeyboardDevice::Event) override;

    // ^ConsoleImplementation
    virtual void on_sysconsole_receive(u8) override;

    // ^TTY
    virtual ssize_t on_tty_write(const u8*, ssize_t) override;
    virtual StringView tty_name() const override;
    virtual void echo(u8) override { return; }

    // ^CharacterDevice
    virtual const char* class_name() const override { return "VirtualConsole"; }

    void set_active(bool);
    void on_char(u8);

    void get_vga_cursor(u8& row, u8& column);
    void flush_vga_cursor();

    u8* m_buffer;
    unsigned m_index;
    bool m_active { false };
    bool m_graphical { false };

    void scroll_up();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, u8 ch);

    void escape$A(const Vector<unsigned>&);
    void escape$D(const Vector<unsigned>&);
    void escape$H(const Vector<unsigned>&);
    void escape$J(const Vector<unsigned>&);
    void escape$m(const Vector<unsigned>&);
    void escape$s(const Vector<unsigned>&);
    void escape$u(const Vector<unsigned>&);

    void clear();

    u8 m_cursor_row { 0 };
    u8 m_cursor_column { 0 };
    u8 m_saved_cursor_row { 0 };
    u8 m_saved_cursor_column { 0 };
    u8 m_current_attribute { 0x07 };

    void clear_vga_row(u16 row);
    void set_vga_start_row(u16 row);
    u16 m_vga_start_row { 0 };
    u16 m_current_vga_start_address { 0 };
    u8* m_current_vga_window { nullptr };

    void execute_escape_sequence(u8 final);

    enum EscapeState {
        Normal,
        ExpectBracket,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,
    };
    EscapeState m_escape_state { Normal };
    Vector<u8> m_parameters;
    Vector<u8> m_intermediates;
    u8* m_horizontal_tabs { nullptr };
    char m_tty_name[32];
};
