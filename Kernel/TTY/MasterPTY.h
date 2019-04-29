#pragma once

#include <AK/Badge.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/DoubleBuffer.h>

class SlavePTY;

class MasterPTY final : public CharacterDevice {
public:
    explicit MasterPTY(unsigned index);
    virtual ~MasterPTY() override;

    unsigned index() const { return m_index; }
    String pts_name() const;
    ssize_t on_slave_write(const byte*, ssize_t);
    bool can_write_from_slave() const;
    void notify_slave_closed(Badge<SlavePTY>);
    bool is_closed() const { return m_closed; }

private:
    // ^CharacterDevice
    virtual ssize_t read(FileDescriptor&, byte*, ssize_t) override;
    virtual ssize_t write(FileDescriptor&, const byte*, ssize_t) override;
    virtual bool can_read(FileDescriptor&) const override;
    virtual bool can_write(FileDescriptor&) const override;
    virtual void close() override;
    virtual bool is_master_pty() const override { return true; }
    virtual int ioctl(FileDescriptor&, unsigned request, unsigned arg) override;
    virtual const char* class_name() const override { return "MasterPTY"; }

    RetainPtr<SlavePTY> m_slave;
    unsigned m_index;
    bool m_closed { false };
    DoubleBuffer m_buffer;
    String m_pts_name;
};
