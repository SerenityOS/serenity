/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/CircularQueue.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBPipe.h>
#include <Kernel/Devices/HID/MouseDevice.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class USBMouseDevice final : public MouseDevice {
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullRefPtr<USBMouseDevice>> try_create_instance(USB::Device const&, size_t max_packet_size, NonnullOwnPtr<USB::InterruptInPipe> pipe);
    virtual ~USBMouseDevice() override {};

    USB::Device const& device() const { return *m_attached_usb_device; }

private:
    ErrorOr<void> create_and_start_polling_process(size_t max_packet_size);

    USBMouseDevice(USB::Device const&, NonnullOwnPtr<USB::InterruptInPipe> pipe);
    NonnullOwnPtr<USB::InterruptInPipe> m_interrupt_in_pipe;
    NonnullRefPtr<USB::Device> m_attached_usb_device;

    IntrusiveListNode<USBMouseDevice, NonnullRefPtr<USBMouseDevice>> m_list_node;

public:
    using List = IntrusiveList<&USBMouseDevice::m_list_node>;
};

}
