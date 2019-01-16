#pragma once

#include <VirtualFileSystem/CharacterDevice.h>
#include "DoubleBuffer.h"

class SlavePTY;

class MasterPTY final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    explicit MasterPTY(unsigned index);
    virtual ~MasterPTY() override;

    virtual ssize_t read(Process&, byte*, size_t) override;
    virtual ssize_t write(Process&, const byte*, size_t) override;
    virtual bool can_read(Process&) const override;
    virtual bool can_write(Process&) const override;
    virtual bool is_master_pty() const override { return true; }

    String pts_name() const;
    void on_slave_write(const byte*, size_t);

private:
    SlavePTY& m_slave;
    unsigned m_index;
    DoubleBuffer m_buffer;
};
