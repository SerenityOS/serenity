#pragma once

#include <AK/CircularQueue.h>
#include <AK/Vector.h>
#include <Kernel/Devices/CharacterDevice.h>

class ConsoleImplementation {
public:
    virtual ~ConsoleImplementation();
    virtual void on_sysconsole_receive(u8) = 0;
};

class Console final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    static Console& the();
    static bool is_initialized();

    Console();
    virtual ~Console() override;

    // ^CharacterDevice
    virtual bool can_read(const FileDescription&) const override;
    virtual bool can_write(const FileDescription&) const override { return true; }
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual const char* class_name() const override { return "Console"; }

    void set_implementation(ConsoleImplementation* implementation) { m_implementation = implementation; }

    void put_char(char);

    const CircularQueue<char, 16384>& logbuffer() const { return m_logbuffer; }

private:
    ConsoleImplementation* m_implementation { nullptr };
    CircularQueue<char, 16384> m_logbuffer;
};
