#pragma once

#include <AK/Compiler.h>
#include <AK/Vector.h>
#include <VirtualFileSystem/CharacterDevice.h>

class Console final : public CharacterDevice {
public:
    static Console& the() PURE;

    Console();
    virtual ~Console() override;

    virtual bool hasDataAvailableForRead() const override;
    virtual ssize_t read(byte* buffer, size_t size) override;
    virtual ssize_t write(const byte* data, size_t size) override;

    void putChar(char);

private:
    void escape$H(const Vector<unsigned>&);
    void escape$J(const Vector<unsigned>&);
    void escape$m(const Vector<unsigned>&);

    const byte m_rows { 25 };
    const byte m_columns { 80 };
    byte m_cursorRow { 0 };
    byte m_cursorColumn { 0 };

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

    const byte* s_vgaMemory { (const byte*)0xb8000 };
};

