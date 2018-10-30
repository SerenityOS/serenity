#pragma once

#include <AK/Compiler.h>
#include <AK/Vector.h>
#include <VirtualFileSystem/CharacterDevice.h>

class ConsoleImplementation {
public:
    virtual ~ConsoleImplementation();
    virtual void onConsoleReceive(byte) = 0;
};

class Console final : public CharacterDevice {
public:
    static Console& the() PURE;

    Console();
    virtual ~Console() override;

    virtual bool hasDataAvailableForRead() const override;
    virtual ssize_t read(byte* buffer, size_t size) override;
    virtual ssize_t write(const byte* data, size_t size) override;

    void setImplementation(ConsoleImplementation* implementation) { m_implementation = implementation; }

    void putChar(char);

private:
    ConsoleImplementation* m_implementation { nullptr };
};

