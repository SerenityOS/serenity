/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Bus/SerialIO/PS2/Controller.h>
#include <Kernel/Bus/SerialIO/PS2/Device.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/Random.h>

namespace Kernel {

class PS2KeyboardDevice final : public PS2Device {
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullOwnPtr<PS2KeyboardDevice>> try_to_initialize(PS2Controller const&, PS2PortIndex port_index, ScanCodeSet scan_code_set, KeyboardDevice const&);
    virtual ~PS2KeyboardDevice() override;
    ErrorOr<void> initialize();

    // ^PS2Device
    virtual void handle_byte_read_from_serial_input(u8 byte) override;

private:
    PS2KeyboardDevice(PS2Controller const&, PS2PortIndex port_index, ScanCodeSet scan_code_set, KeyboardDevice const&);

    void handle_byte_read_for_scan_code_set1(u8 byte);
    void handle_byte_read_for_scan_code_set2(u8 byte);

    // NOTE: This boolean variable is only used with ScanCodeSet::Set1
    // because it only has one prefix defined in the scan code set.
    bool m_has_e0_prefix { false };

    // NOTE: This array and its counter are used only when m_scan_code_set
    // is set to ScanCodeSet::Set2, because that scan code requires us to
    // manage scan codes with multiple bytes.
    // According to the scan code set 2 table, a key press (or release)
    // can generate up to 8 bytes.
    Array<u8, 8> m_received_bytes;
    size_t m_received_bytes_count { 0 };

    NonnullRefPtr<KeyboardDevice> const m_keyboard_device;
    ScanCodeSet const m_scan_code_set { ScanCodeSet::Set1 };
};

}
