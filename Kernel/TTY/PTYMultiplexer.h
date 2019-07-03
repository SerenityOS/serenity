#pragma once

#include <AK/Badge.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Lock.h>

class MasterPTY;

class PTYMultiplexer final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    PTYMultiplexer();
    virtual ~PTYMultiplexer() override;

    static PTYMultiplexer& the();

    // ^CharacterDevice
    virtual KResultOr<NonnullRefPtr<FileDescription>> open(int options) override;
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override { return 0; }
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override { return 0; }
    virtual bool can_read(FileDescription&) const override { return true; }
    virtual bool can_write(FileDescription&) const override { return true; }

    void notify_master_destroyed(Badge<MasterPTY>, unsigned index);

private:
    // ^CharacterDevice
    virtual const char* class_name() const override { return "PTYMultiplexer"; }

    Lock m_lock { "PTYMultiplexer" };
    Vector<unsigned> m_freelist;
};
