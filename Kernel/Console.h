#pragma once

#include <AK/CircularQueue.h>
#include <AK/Compiler.h>
#include <AK/Vector.h>
#include <Kernel/CharacterDevice.h>

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

    // ^CharacterDevice
    virtual bool can_read(Process&) const override;
    virtual bool can_write(Process&) const override { return true; }
    virtual ssize_t read(Process&, byte* buffer, size_t size) override;
    virtual ssize_t write(Process&, const byte* data, size_t size) override;
    virtual const char* class_name() const override { return "Console"; }

    void set_implementation(ConsoleImplementation* implementation) { m_implementation = implementation; }

    void put_char(char);

    const CircularQueue<char, 16384>& logbuffer() const { return m_logbuffer; }

private:
    ConsoleImplementation* m_implementation { nullptr };
    CircularQueue<char, 16384> m_logbuffer;
};

