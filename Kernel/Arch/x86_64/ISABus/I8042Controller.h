/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/SerialIO/Controller.h>
#include <Kernel/Bus/SerialIO/Device.h>
#include <Kernel/Bus/SerialIO/PS2Definitions.h>
#include <Kernel/Interrupts/IRQHandler.h>
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

class PS2KeyboardDevice;
class PS2MouseDevice;

class I8042Controller;
class I8042ControllerIRQHandler final
    : public IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<I8042ControllerIRQHandler>> try_create(I8042Controller const&, u8 irq_number);

private:
    I8042ControllerIRQHandler(I8042Controller const& controller, u8 irq_number);

    // ^IRQHandler
    virtual bool handle_irq() override;
    virtual StringView purpose() const override { return "I8042ControllerIRQHandler"sv; }

    NonnullRefPtr<I8042Controller> const m_controller;
};

class InputManagement;
class I8042Controller final : public SerialIOController {
    friend class PS2KeyboardDevice;
    friend class PS2MouseDevice;

public:
    static ErrorOr<NonnullRefPtr<I8042Controller>> create();

    enum class EnableKeyboardFirstPortTranslation {
        Yes,
        No
    };
    ErrorOr<void> detect_devices(EnableKeyboardFirstPortTranslation);

    virtual ErrorOr<void> send_command(PortIndex, DeviceCommand command) override;
    virtual ErrorOr<void> send_command(PortIndex, DeviceCommand command, u8 data) override;

    virtual ErrorOr<void> reset_device(PortIndex port_index) override
    {
        SpinlockLocker lock(m_lock);
        return do_reset_device(port_index);
    }
    virtual ErrorOr<u8> read_from_device(PortIndex port_index) override
    {
        SpinlockLocker lock(m_lock);
        return do_read_from_device(port_index);
    }
    virtual ErrorOr<void> prepare_for_input(PortIndex) override;

    // Note: This function exists only for the initialization process of the controller
    bool check_existence_via_probing(Badge<InputManagement>);

    bool handle_irq(Badge<I8042ControllerIRQHandler>, u8 irq_number);

private:
    I8042Controller();

    bool irq_process_input_buffer(PortIndex);

    ErrorOr<void> prepare_for_any_output();

    ErrorOr<void> prepare_for_any_input();

    ErrorOr<void> do_reset_device(PortIndex);
    ErrorOr<void> do_send_command(PortIndex port_index, u8 data);
    ErrorOr<void> do_send_command(PortIndex port_index, u8 command, u8 data);
    ErrorOr<void> do_write_to_device(PortIndex port_index, u8 data);
    ErrorOr<u8> do_read_from_device(PortIndex port_index);
    ErrorOr<void> do_wait_then_write(u8 port, u8 data);

    // NOTE: The meaning of "any input" here is that this is not attached
    // to any PS2 port, but rather we accept any serial input, which is vital
    // when reading values before initializing any actual PS2 device!
    ErrorOr<u8> do_wait_then_read_any_input(u8 port);

    ErrorOr<void> drain_output_buffer();

    // Note: These functions exist only for the initialization process of the controller
    void do_write(u8 port, u8 data);
    u8 do_read(u8 port);

    Spinlock<LockRank::None> m_lock {};
    bool m_first_port_available { false };
    bool m_second_port_available { false };
    bool m_is_dual_channel { false };

    enum I8042PortIndex {
        FirstPort = 0,
        SecondPort = 1,
    };

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
        OwnPtr<SerialIODevice> device;
        // NOTE: This value is being used as 1:1 map between the I8042 port being handled, to
        // the either the MouseDevice or KeyboardDevice being attached.
        Optional<PS2DeviceType> device_type;
    };

    // NOTE: Each i8042 controller can have at most 2 devices - a mouse and keyboard,
    // mouse and a mouse, or keyboard and a keyboard.
    // NOTE: This is usually used as the ATKBD port.
    PS2Port m_first_ps2_port;
    // NOTE: This is usually used as the AUX port.
    PS2Port m_second_ps2_port;

    Array<OwnPtr<I8042ControllerIRQHandler>, 2> m_irq_handlers;
};

}
