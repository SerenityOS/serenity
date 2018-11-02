#pragma once

#include "TTY.h"
#include "Keyboard.h"
#include "Console.h"

class VirtualConsole final : public TTY, public KeyboardClient, public ConsoleImplementation {
    AK_MAKE_ETERNAL
public:
    enum InitialContents { Cleared, AdoptCurrentVGABuffer };

    VirtualConsole(unsigned index, InitialContents = Cleared);
    virtual ~VirtualConsole() override;

    static void switch_to(unsigned);
    static void initialize();

private:
    // ^KeyboardClient
    virtual void onKeyPress(Keyboard::Key) override;

    // ^ConsoleImplementation
    virtual void onConsoleReceive(byte) override;

    // ^TTY
    virtual void onTTYWrite(byte) override;
    virtual String ttyName() const override;

    void set_active(bool);
    void on_char(byte, bool shouldEmit);

    byte* m_buffer;
    unsigned m_index;
    bool m_active { false };

    void scroll_up();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, byte ch);

    void escape$H(const Vector<unsigned>&);
    void escape$J(const Vector<unsigned>&);
    void escape$m(const Vector<unsigned>&);
    void escape$s(const Vector<unsigned>&);
    void escape$u(const Vector<unsigned>&);

    const byte m_rows { 25 };
    const byte m_columns { 80 };
    byte m_cursor_row { 0 };
    byte m_cursor_column { 0 };
    byte m_saved_cursor_row { 0 };
    byte m_saved_cursor_column { 0 };
    byte m_current_attribute { 0x07 };

    void execute_escape_sequence(byte final);

    enum EscapeState {
        Normal,
        ExpectBracket,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,
    };
    EscapeState m_escape_state { Normal };
    Vector<byte> m_parameters;
    Vector<byte> m_intermediates;
};
