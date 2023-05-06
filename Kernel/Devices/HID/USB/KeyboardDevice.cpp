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
#include <Kernel/Devices/HID/USB/KeyboardDevice.h>
#include <Kernel/KString.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<USBKeyboardDevice>> USBKeyboardDevice::try_create_instance(USB::Device const& usb_device)
{
    auto device = TRY(DeviceManagement::try_create_device<USBKeyboardDevice>(usb_device));
    TRY(device->create_and_start_polling_process());
    return *device;
}

ErrorOr<void> USBKeyboardDevice::create_and_start_polling_process()
{
    auto kernel_process_name = TRY(KString::try_create("USB keyboard handler"sv));
    VERIFY(m_attached_usb_device->interrupt_pipe());
    TRY(static_cast<USB::InterruptInPipe*>(m_attached_usb_device->interrupt_pipe())->submit_interrupt_in_transfer(8, 10, [this](auto* transfer) {
        if (!transfer)
            return;
        if (transfer->transfer_data_size() == 0)
            return;
        u8 packet_raw[8];
        memcpy(packet_raw, transfer->buffer().as_ptr(), 8);
        dbgln("{:x} {:x} {:x} {:x} {:x} {:x} {:x} {:x}",
            (u32)packet_raw[0], (u32)packet_raw[1],
            (u32)packet_raw[2], (u32)packet_raw[3],
            (u32)packet_raw[4], (u32)packet_raw[5],
            (u32)packet_raw[6], (u32)packet_raw[7]);
        ScanCodeEvent event {};
        event.bytes_count = 1;
        event.scan_code_bytes[0] = packet_raw[2];
        event.sent_scan_code_set = ScanCodeSet::USBBootSet;
        if (packet_raw[2] != 0) {
            m_previous_key = packet_raw[2] | 0x80;
            event.scan_code_bytes[0] = m_previous_key;
            handle_scan_code_input_event(event);
        } else {
            event.scan_code_bytes[0] = m_previous_key & 0x7f;
            handle_scan_code_input_event(event);
        }
    }));

    return {};
}

USBKeyboardDevice::USBKeyboardDevice(USB::Device const& usb_device)
    : KeyboardDevice()
    , m_attached_usb_device(usb_device)
{
}

}
