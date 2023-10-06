/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/Types.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class AllMiceDevice final : public CharacterDevice {
    friend class DeviceManagement;

public:
    static NonnullRefPtr<AllMiceDevice> must_create();

    virtual ~AllMiceDevice() override;

    void enqueue_mouse_packet(MousePacket packet);

private:
    AllMiceDevice();

    // ^CharacterDevice
    virtual StringView class_name() const override { return "AllMiceDevice"sv; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return EINVAL; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }

    mutable Spinlock<LockRank::None> m_queue_lock {};
    CircularQueue<MousePacket, 1000> m_queue;
};
}
