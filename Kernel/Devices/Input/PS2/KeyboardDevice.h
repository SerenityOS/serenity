/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Bus/SerialIO/Device.h>
#include <Kernel/Devices/Input/Definitions.h>
#include <Kernel/Devices/Input/KeyboardDevice.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class PS2KeyboardDevice final : public SerialIODevice {
    friend class Device;

public:
    static ErrorOr<NonnullOwnPtr<PS2KeyboardDevice>> try_to_initialize(SerialIOController const&, SerialIOController::PortIndex port_index, ScanCodeSet scan_code_set, KeyboardDevice const&);
    virtual ~PS2KeyboardDevice() override;
    ErrorOr<void> initialize();

    // ^SerialIODevice
    virtual void handle_byte_read_from_serial_input(u8 byte) override;

private:
    PS2KeyboardDevice(SerialIOController const&, SerialIOController::PortIndex port_index, ScanCodeSet scan_code_set, KeyboardDevice const&);

    RawKeyEvent generate_raw_key_event_input_from_set1(ScanCodeEvent);
    Optional<RawKeyEvent> generate_raw_key_event_input_from_set2(ScanCodeEvent);

    void handle_scan_code_input_event(ScanCodeEvent event);

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

    bool m_left_shift_pressed { false };
    bool m_right_shift_pressed { false };
    bool m_left_super_pressed { false };
    bool m_right_super_pressed { false };

    NonnullRefPtr<KeyboardDevice> const m_keyboard_device;
    ScanCodeSet const m_scan_code_set { ScanCodeSet::Set1 };

    EntropySource m_entropy_source;
};

}
