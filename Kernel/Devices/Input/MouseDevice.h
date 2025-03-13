/*
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Input/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class MouseDevice : public InputDevice {
    friend class Device;

public:
    static ErrorOr<NonnullRefPtr<MouseDevice>> try_to_initialize();
    virtual ~MouseDevice() override;

    // ^CharacterDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return EINVAL; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }

    void handle_mouse_packet_input_event(MousePacket);

protected:
    MouseDevice();
    // ^CharacterDevice
    virtual StringView class_name() const override { return "MouseDevice"sv; }

    mutable Spinlock<LockRank::None> m_queue_lock {};
    CircularQueue<MousePacket, 100> m_queue;
};

}
