#pragma once

#include <AK/Compiler.h>
#include <AK/Vector.h>
#include <VirtualFileSystem/CharacterDevice.h>

class ConsoleImplementation {
public:
    virtual ~ConsoleImplementation();
    virtual void on_sysconsole_receive(byte) = 0;
};

class Console final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    static Console& the() PURE;

    Console();
    virtual ~Console() override;

    virtual bool has_data_available_for_reading(Process&) const override;
    virtual bool can_write(Process&) const override { return true; }
    virtual ssize_t read(byte* buffer, size_t size) override;
    virtual ssize_t write(const byte* data, size_t size) override;

    void setImplementation(ConsoleImplementation* implementation) { m_implementation = implementation; }

    void put_char(char);

private:
    ConsoleImplementation* m_implementation { nullptr };
};

