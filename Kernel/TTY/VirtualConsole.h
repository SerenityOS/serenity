#pragma once

#include "Console.h"
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/TTY/TTY.h>

class VirtualConsole final : public TTY
    , public KeyboardClient
    , public ConsoleImplementation {
    AK_MAKE_ETERNAL
public:
    enum InitialContents
    {
        Cleared,
        AdoptCurrentVGABuffer
    };

    VirtualConsole(unsigned index, InitialContents = Cleared);
    virtual ~VirtualConsole() override;

    static void switch_to(unsigned);
    static void initialize();

private:
    // ^KeyboardClient
    virtual void on_key_pressed(KeyboardDevice::Event) override;

    // ^ConsoleImplementation
    virtual void on_sysconsole_receive(byte) override;

    // ^TTY
    virtual ssize_t on_tty_write(const byte*, ssize_t) override;
    virtual String tty_name() const override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "VirtualConsole"; }

    void set_active(bool);
    void on_char(byte);

    void get_vga_cursor(byte& row, byte& column);
    void flush_vga_cursor();

    byte* m_buffer;
    unsigned m_index;
    bool m_active { false };

    void scroll_up();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, byte ch);

    void escape$A(const Vector<unsigned>&);
    void escape$D(const Vector<unsigned>&);
    void escape$H(const Vector<unsigned>&);
    void escape$J(const Vector<unsigned>&);
    void escape$m(const Vector<unsigned>&);
    void escape$s(const Vector<unsigned>&);
    void escape$u(const Vector<unsigned>&);

    void clear();

    byte m_cursor_row { 0 };
    byte m_cursor_column { 0 };
    byte m_saved_cursor_row { 0 };
    byte m_saved_cursor_column { 0 };
    byte m_current_attribute { 0x07 };

    void clear_vga_row(word row);
    void set_vga_start_row(word row);
    word m_vga_start_row { 0 };
    word m_current_vga_start_address { 0 };
    byte* m_current_vga_window { nullptr };

    void execute_escape_sequence(byte final);

    enum EscapeState
    {
        Normal,
        ExpectBracket,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,
    };
    EscapeState m_escape_state { Normal };
    Vector<byte> m_parameters;
    Vector<byte> m_intermediates;
    byte* m_horizontal_tabs { nullptr };
    String m_tty_name;
};
