#pragma once

#include <VirtualFileSystem/CharacterDevice.h>

class Console final : public CharacterDevice {
public:
    static Console& the();

    Console();
    virtual ~Console() override;

    virtual ssize_t read(byte* buffer, size_t size) override;
    virtual ssize_t write(const byte* data, size_t size) override;

private:
    byte m_rows { 25 };
    byte m_columns { 80 };
    byte m_cursorRow { 0 };
    byte m_cursorColumn { 0 };

    byte m_currentAttribute { 0x07 };

    const byte* s_vgaMemory { (const byte*)0xb8000 };
};

