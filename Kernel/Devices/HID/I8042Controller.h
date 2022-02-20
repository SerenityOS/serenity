/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/Devices/HID/MouseDevice.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

enum I8042Port : u8 {
    Buffer = 0x60,
    Command = 0x64,
    Status = 0x64,
};

enum I8042Command : u8 {
    ReadConfiguration = 0x20,
    WriteConfiguration = 0x60,
    DisableSecondPS2Port = 0xA7,
    EnableSecondPS2Port = 0xA8,
    TestSecondPS2Port = 0xA9,
    TestPS2Controller = 0xAA,
    TestFirstPS2Port = 0xAB,
    DisableFirstPS2Port = 0xAD,
    EnableFirstPS2Port = 0xAE,
    WriteSecondPS2PortInputBuffer = 0xD4,
    GetDeviceID = 0xF2,
    SetSampleRate = 0xF3,
    EnablePacketStreaming = 0xF4,
    SetDefaults = 0xF6,
    Reset = 0xFF,
};

enum I8042ConfigurationFlag : u8 {
    FirstPS2PortInterrupt = 1 << 0,
    SecondPS2PortInterrupt = 1 << 1,
    SystemFlag = 1 << 2,
    FirstPS2PortClock = 1 << 4,
    SecondPS2PortClock = 1 << 5,
    FirstPS2PortTranslation = 1 << 6,
};

enum I8042StatusFlag : u8 {
    OutputBuffer = 1 << 0,
    InputBuffer = 1 << 1,
    System = 1 << 2,
    InputType = 1 << 3,
    SecondPS2PortOutputBuffer = 1 << 5,
    TimeoutError = 1 << 6,
    ParityError = 1 << 7,
};

enum I8042Response : u8 {
    ControllerTestPassed = 0x55,
    Success = 0xAA,
    Acknowledge = 0xFA,
    Resend = 0xFE,
};

class I8042Controller;
class I8042Device {
public:
    virtual ~I8042Device() = default;

    virtual void irq_handle_byte_read(u8 byte) = 0;

protected:
    explicit I8042Device(const I8042Controller& ps2_controller)
        : m_i8042_controller(ps2_controller)
    {
    }

    NonnullRefPtr<I8042Controller> m_i8042_controller;
};

class PS2KeyboardDevice;
class PS2MouseDevice;
class HIDManagement;
class I8042Controller : public RefCounted<I8042Controller> {
    friend class PS2KeyboardDevice;
    friend class PS2MouseDevice;

public:
    static NonnullRefPtr<I8042Controller> initialize();

    ErrorOr<void> detect_devices();

    bool reset_device(HIDDevice::Type device)
    {
        SpinlockLocker lock(m_lock);
        // FIXME: Propagate errors properly
        if (auto result = do_reset_device(device); result.is_error())
            return false;
        return true;
    }

    u8 send_command(HIDDevice::Type device, u8 command)
    {
        SpinlockLocker lock(m_lock);
        // FIXME: Propagate errors properly
        return MUST(do_send_command(device, command));
    }
    u8 send_command(HIDDevice::Type device, u8 command, u8 data)
    {
        SpinlockLocker lock(m_lock);
        // FIXME: Propagate errors properly
        return MUST(do_send_command(device, command, data));
    }

    u8 read_from_device(HIDDevice::Type device)
    {
        SpinlockLocker lock(m_lock);
        // FIXME: Propagate errors properly
        return MUST(do_read_from_device(device));
    }

    void wait_then_write(u8 port, u8 data)
    {
        SpinlockLocker lock(m_lock);
        // FIXME: Propagate errors properly
        MUST(do_wait_then_write(port, data));
    }

    u8 wait_then_read(u8 port)
    {
        SpinlockLocker lock(m_lock);
        // FIXME: Propagate errors properly
        return MUST(do_wait_then_read(port));
    }

    ErrorOr<void> prepare_for_output();
    ErrorOr<void> prepare_for_input(HIDDevice::Type);

    bool irq_process_input_buffer(HIDDevice::Type);

    RefPtr<MouseDevice> mouse() const;
    RefPtr<KeyboardDevice> keyboard() const;

    // Note: This function exists only for the initialization process of the controller
    bool check_existence_via_probing(Badge<HIDManagement>);

private:
    I8042Controller();
    ErrorOr<void> do_reset_device(HIDDevice::Type);
    ErrorOr<u8> do_send_command(HIDDevice::Type type, u8 data);
    ErrorOr<u8> do_send_command(HIDDevice::Type device, u8 command, u8 data);
    ErrorOr<u8> do_write_to_device(HIDDevice::Type device, u8 data);
    ErrorOr<u8> do_read_from_device(HIDDevice::Type device);
    ErrorOr<void> do_wait_then_write(u8 port, u8 data);
    ErrorOr<u8> do_wait_then_read(u8 port);
    ErrorOr<void> drain_output_buffer();

    // Note: These functions exist only for the initialization process of the controller
    void do_write(u8 port, u8 data);
    u8 do_read(u8 port);

    Spinlock m_lock;
    bool m_first_port_available { false };
    bool m_second_port_available { false };
    bool m_is_dual_channel { false };
    RefPtr<MouseDevice> m_mouse_device;
    RefPtr<KeyboardDevice> m_keyboard_device;
};

}
