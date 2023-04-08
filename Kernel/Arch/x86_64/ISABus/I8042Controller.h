/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <Kernel/Devices/HID/Controller.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/Devices/HID/MouseDevice.h>
#include <Kernel/Devices/HID/ScanCodeEvent.h>
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
    SetScanCodeSet = 0xF0,
    GetDeviceID = 0xF2,
    SetSampleRate = 0xF3,
    EnablePacketStreaming = 0xF4,
    DisablePacketStreaming = 0xF5,
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
class PS2KeyboardDevice;
class PS2MouseDevice;
class PS2Device {
public:
    virtual ~PS2Device() = default;

    virtual void irq_handle_byte_read(u8 byte) = 0;
    virtual void enable_interrupts() = 0;

    enum class Type {
        Unknown = 0,
        Keyboard,
        Mouse,
    };

    virtual Type instrument_type() const = 0;

protected:
    explicit PS2Device(I8042Controller const& ps2_controller)
        : m_i8042_controller(ps2_controller)
    {
    }

    NonnullLockRefPtr<I8042Controller> m_i8042_controller;
};

class PS2KeyboardDevice;
class PS2MouseDevice;
class HIDManagement;
class I8042Controller final : public HIDController {
    friend class PS2KeyboardDevice;
    friend class PS2MouseDevice;

public:
    static NonnullLockRefPtr<I8042Controller> initialize();

    ErrorOr<void> detect_devices();

    ErrorOr<void> reset_device(PS2Device::Type device)
    {
        SpinlockLocker lock(m_lock);
        return do_reset_device(device);
    }

    ErrorOr<u8> send_command(PS2Device::Type device, u8 command)
    {
        SpinlockLocker lock(m_lock);
        return do_send_command(device, command);
    }
    ErrorOr<u8> send_command(PS2Device::Type device, u8 command, u8 data)
    {
        SpinlockLocker lock(m_lock);
        return do_send_command(device, command, data);
    }

    ErrorOr<u8> read_from_device(PS2Device::Type device)
    {
        SpinlockLocker lock(m_lock);
        return do_read_from_device(device);
    }

    ErrorOr<void> wait_then_write(u8 port, u8 data)
    {
        SpinlockLocker lock(m_lock);
        return do_wait_then_write(port, data);
    }

    ErrorOr<u8> wait_then_read(u8 port)
    {
        SpinlockLocker lock(m_lock);
        return do_wait_then_read(port);
    }

    ErrorOr<void> prepare_for_output();
    ErrorOr<void> prepare_for_input(PS2Device::Type);

    bool irq_process_input_buffer(PS2Device::Type);

    // Note: This function exists only for the initialization process of the controller
    bool check_existence_via_probing(Badge<HIDManagement>);

private:
    I8042Controller();
    ErrorOr<void> do_reset_device(PS2Device::Type);
    ErrorOr<u8> do_send_command(PS2Device::Type type, u8 data);
    ErrorOr<u8> do_send_command(PS2Device::Type device, u8 command, u8 data);
    ErrorOr<u8> do_write_to_device(PS2Device::Type device, u8 data);
    ErrorOr<u8> do_read_from_device(PS2Device::Type device);
    ErrorOr<void> do_wait_then_write(u8 port, u8 data);
    ErrorOr<u8> do_wait_then_read(u8 port);
    ErrorOr<void> drain_output_buffer();

    // Note: These functions exist only for the initialization process of the controller
    void do_write(u8 port, u8 data);
    u8 do_read(u8 port);

    Spinlock<LockRank::None> m_lock {};
    bool m_first_port_available { false };
    bool m_second_port_available { false };
    bool m_is_dual_channel { false };

    // NOTE: Each i8042 controller can have at most 2 ports - a regular (traditional
    // ATKBD) port and AUX port (for mouse devices mostly).
    // However, the specification for i8042 controller, as well as decent hardware
    // implementations and software drivers actually allow a user to still operate
    // a keyboard and mouse even if they were connected in reverse (i.e. keyboard
    // was connected to AUX port, and mouse was connected to the traditional ATKBD port).
    //
    // Please note, that if the keyboard and mouse devices are connected in reverse, then ATKBD translation mode
    // cannot be sanely enabled due to obvious peripheral devices' protocol differences, and will result
    // in misproper data being sent back.
    struct PS2Port {
        OwnPtr<PS2Device> device;
        // NOTE: This value is being used as 1:1 map between the I8042 port being handled, to
        // the either the MouseDevice or KeyboardDevice being attached.
        Optional<PS2Device::Type> device_type;
    };

    // NOTE: Each i8042 controller can have at most 2 devices - a mouse and keyboard,
    // mouse and a mouse, or keyboard and a keyboard.
    // NOTE: This is usually used as the ATKBD port.
    PS2Port m_first_ps2_port;
    // NOTE: This is usually used as the AUX port.
    PS2Port m_second_ps2_port;
};

}
