/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Bus/USB/USBTransfer.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/USB/MouseDevice.h>
#include <Kernel/KString.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<USBMouseDevice>> USBMouseDevice::try_create_instance(USB::Device const& usb_device)
{
    auto device = TRY(DeviceManagement::try_create_device<USBMouseDevice>(usb_device));
    TRY(device->create_and_start_polling_process());
    return *device;
}

ErrorOr<void> USBMouseDevice::create_and_start_polling_process()
{
    VERIFY(m_attached_usb_device->interrupt_pipe());
    TRY(static_cast<USB::InterruptInPipe*>(m_attached_usb_device->interrupt_pipe())->submit_interrupt_in_transfer(6, 10, [this](auto* transfer) {
        if (transfer->transfer_data_size() == 0)
            return;
        char packet_raw[6];
        memcpy(packet_raw, transfer->buffer().as_ptr(), 6);
        MousePacket packet;
        packet.buttons = packet_raw[0] & 0x07;
        packet.x = packet_raw[1];
        packet.y = -packet_raw[2];
        packet.z = packet_raw[3];
        packet.w = 0;
        packet.is_relative = true;

        handle_mouse_packet_input_event(packet);
    }));
    return {};
}

USBMouseDevice::USBMouseDevice(USB::Device const& usb_device)
    : MouseDevice()
    , m_attached_usb_device(usb_device)
{
}

}
