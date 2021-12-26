/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/DoubleBuffer.h>

namespace Kernel {

class SlavePTY;

class MasterPTY final : public CharacterDevice {
public:
    explicit MasterPTY(unsigned index);
    virtual ~MasterPTY() override;

    unsigned index() const { return m_index; }
    String pts_name() const;
    KResultOr<size_t> on_slave_write(const UserOrKernelBuffer&, size_t);
    bool can_write_from_slave() const;
    void notify_slave_closed(Badge<SlavePTY>);
    bool is_closed() const { return m_closed; }

    virtual String absolute_path(const FileDescription&) const override;

    // ^Device
    virtual mode_t required_mode() const override { return 0640; }
    virtual String device_name() const override;

private:
    // ^CharacterDevice
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResult close() override;
    virtual bool is_master_pty() const override { return true; }
    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;
    virtual StringView class_name() const override { return "MasterPTY"; }

    RefPtr<SlavePTY> m_slave;
    unsigned m_index;
    bool m_closed { false };
    DoubleBuffer m_buffer;
    String m_pts_name;
};

}
