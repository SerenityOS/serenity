#pragma once

#include <Kernel/CharacterDevice.h>
#include <AK/Lock.h>

class MasterPTY;

class PTYMultiplexer final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    PTYMultiplexer();
    virtual ~PTYMultiplexer() override;

    // ^CharacterDevice
    virtual RetainPtr<FileDescriptor> open(int& error, int options) override;
    virtual ssize_t read(Process&, byte*, size_t) override { return 0; }
    virtual ssize_t write(Process&, const byte*, size_t) override { return 0; }
    virtual bool can_read(Process&) const override { return true; }
    virtual bool can_write(Process&) const override { return true; }

private:
    // ^CharacterDevice
    virtual const char* class_name() const override { return "PTYMultiplexer"; }

    Lock m_lock;
    Vector<RetainPtr<MasterPTY>> m_freelist;
};
