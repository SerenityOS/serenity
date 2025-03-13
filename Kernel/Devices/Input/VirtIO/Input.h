/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/Input/KeyboardDevice.h>
#include <Kernel/Devices/Input/MouseDevice.h>
#include <Kernel/Memory/RingBuffer.h>
#include <Kernel/Security/Random.h>

namespace Kernel::VirtIO {

struct VirtIOInputEvent;

class Input final
    : public AtomicRefCounted<Input>
    , public VirtIO::Device {
public:
    static NonnullLockRefPtr<Input> must_create_for_pci_instance(PCI::DeviceIdentifier const&);
    ~Input() override = default;

    ErrorOr<void> initialize_virtio_resources() override;

private:
    static constexpr u16 EVENTQ = 0;
    static constexpr u16 STATUSQ = 1;

    StringView class_name() const override { return "VirtIOInput"sv; }
    explicit Input(NonnullOwnPtr<TransportEntity>);
    ErrorOr<void> handle_device_config_change() override;
    void handle_queue_update(u16 queue_index) override;

    void handle_event(VirtIOInputEvent const&);

    OwnPtr<Memory::Region> m_event_buffer_region;

    NonnullRefPtr<MouseDevice> m_mouse_device;
    MousePacket m_current_mouse_packet;

    NonnullRefPtr<KeyboardDevice> m_keyboard_device;

    EntropySource m_entropy_source;

    u32 m_abs_min { 0 };
    u32 m_abs_max { 0xffff };
};

}
