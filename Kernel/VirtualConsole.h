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

    void adoptCurrentVGABuffer();
    void setActive(bool);

    static void switchTo(unsigned);
    static void initialize();

private:
    // ^KeyboardClient
    virtual void onKeyPress(byte) override;

    // ^ConsoleImplementation
    virtual void onConsoleReceive(byte) override;

    // ^TTY
    virtual void onTTYWrite(byte) override;
    virtual String ttyName() const override;

    void onChar(byte, bool shouldEmit);

    byte* m_buffer;
    unsigned m_index;
    bool m_active { false };

    void scrollUp();
    void setCursor(unsigned row, unsigned column);
    void putCharacterAt(unsigned row, unsigned column, byte ch);

    void escape$H(const Vector<unsigned>&);
    void escape$J(const Vector<unsigned>&);
    void escape$m(const Vector<unsigned>&);
    void escape$s(const Vector<unsigned>&);
    void escape$u(const Vector<unsigned>&);

    const byte m_rows { 25 };
    const byte m_columns { 80 };
    byte m_cursorRow { 0 };
    byte m_cursorColumn { 0 };
    byte m_savedCursorRow { 0 };
    byte m_savedCursorColumn { 0 };
    byte m_currentAttribute { 0x07 };

    void executeEscapeSequence(byte final);

    enum EscapeState {
        Normal,
        ExpectBracket,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,
    };
    EscapeState m_escState { Normal };
    Vector<byte> m_parameters;
    Vector<byte> m_intermediates;
};
