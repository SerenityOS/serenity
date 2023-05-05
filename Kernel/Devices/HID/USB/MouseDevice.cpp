/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <AK/TypedTransfer.h>
#include <Kernel/Bus/USB/Drivers/HID/Codes.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBDescriptors.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Bus/USB/USBTransfer.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/USB/MouseDevice.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<USBMouseDevice>> USBMouseDevice::try_create_instance(USB::Device const& usb_device, size_t max_packet_size, NonnullOwnPtr<USB::InterruptInPipe> pipe)
{
    if (max_packet_size < 4)
        return Error::from_errno(ENOTSUP);
    auto device = TRY(DeviceManagement::try_create_device<USBMouseDevice>(usb_device, move(pipe)));
    TRY(device->create_and_start_polling_process(max_packet_size));
    return *device;
}

ErrorOr<void> USBMouseDevice::create_and_start_polling_process(size_t max_packet_size)
{
    VERIFY(max_packet_size >= 4);
    [[maybe_unused]] auto interrupt_in_transfer = TRY(m_interrupt_in_pipe->submit_interrupt_in_transfer(max_packet_size, 10, [this](auto* transfer) {
        USB::HID::MouseBootProtocolPacket packet_raw;
        memcpy(&packet_raw, transfer->buffer().as_ptr(), 4);
        MousePacket packet;
        packet.buttons = packet_raw.buttons & 0x07;
        packet.x = packet_raw.x;
        packet.y = -packet_raw.y;
        packet.z = -packet_raw.z;
        packet.w = 0;
        packet.is_relative = true;

        handle_mouse_packet_input_event(packet);
    }));
    return {};
}

USBMouseDevice::USBMouseDevice(USB::Device const& usb_device, NonnullOwnPtr<USB::InterruptInPipe> pipe)
    : MouseDevice()
    , m_interrupt_in_pipe(move(pipe))
    , m_attached_usb_device(usb_device)
{
}

}
