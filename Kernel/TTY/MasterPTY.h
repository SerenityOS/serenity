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
    static KResultOr<NonnullRefPtr<MasterPTY>> try_create(unsigned index);
    virtual ~MasterPTY() override;

    unsigned index() const { return m_index; }
    String pts_name() const;
    KResultOr<size_t> on_slave_write(const UserOrKernelBuffer&, size_t);
    bool can_write_from_slave() const;
    void notify_slave_closed(Badge<SlavePTY>);
    bool is_closed() const { return m_closed; }

    virtual String absolute_path(const OpenFileDescription&) const override;

private:
    explicit MasterPTY(unsigned index, NonnullOwnPtr<DoubleBuffer> buffer);
    // ^CharacterDevice
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const OpenFileDescription&, size_t) const override;
    virtual bool can_write(const OpenFileDescription&, size_t) const override;
    virtual KResult close() override;
    virtual bool is_master_pty() const override { return true; }
    virtual KResult ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual StringView class_name() const override { return "MasterPTY"sv; }

    RefPtr<SlavePTY> m_slave;
    unsigned m_index;
    bool m_closed { false };
    NonnullOwnPtr<DoubleBuffer> m_buffer;
    String m_pts_name;
};

}
