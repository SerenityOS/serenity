/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/x86_64/IO.h>
#include <Kernel/Arch/x86_64/ISABus/I8042Controller.h>
#include <Kernel/Arch/x86_64/ISABus/Input/VMWareMouseDevice.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/SerialIO/Device.h>
#include <Kernel/Devices/Input/KeyboardDevice.h>
#include <Kernel/Devices/Input/MouseDevice.h>
#include <Kernel/Devices/Input/PS2/KeyboardDevice.h>
#include <Kernel/Devices/Input/PS2/MouseDevice.h>
#include <Kernel/Devices/Input/ScanCodeEvent.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define IRQ_FIRST_PORT 1
#define IRQ_SECOND_PORT 12

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<I8042ControllerIRQHandler>> I8042ControllerIRQHandler::try_create(I8042Controller const& controller, u8 irq_number)
{
    return adopt_nonnull_own_or_enomem(new I8042ControllerIRQHandler(controller, irq_number));
}

bool I8042ControllerIRQHandler::handle_irq()
{
    return m_controller->handle_irq({}, interrupt_number());
}

UNMAP_AFTER_INIT I8042ControllerIRQHandler::I8042ControllerIRQHandler(I8042Controller const& controller, u8 irq_number)
    : IRQHandler(irq_number)
    , m_controller(controller)
{
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<I8042Controller>> I8042Controller::create()
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) I8042Controller()));
}

UNMAP_AFTER_INIT I8042Controller::I8042Controller()
{
}

bool I8042Controller::handle_irq(Badge<I8042ControllerIRQHandler>, u8 irq_number)
{
    // NOTE: The controller will read the data and call handle_byte_read_from_serial_input
    // for the appropriate device.
    VERIFY(irq_number == IRQ_FIRST_PORT || irq_number == IRQ_SECOND_PORT);
    PortIndex port_index = I8042PortIndex::FirstPort;
    if (irq_number == IRQ_SECOND_PORT) {
        port_index = I8042PortIndex::SecondPort;
    }
    return irq_process_input_buffer(port_index);
}

UNMAP_AFTER_INIT bool I8042Controller::check_existence_via_probing(Badge<InputManagement>)
{
    {
        u8 configuration = 0;
        SpinlockLocker lock(m_lock);

        // This drains the output buffer and serves as an existence test.
        if (auto result = drain_output_buffer(); result.is_error()) {
            dbgln("I8042: Trying to flush output buffer as an existence test failed, error {}", result.error());
            return false;
        }

        // Note: Perform controller self-test before touching the controller
        // Try to probe the controller for 10 times and give up if nothing
        // responded.
        // Some controllers will reset and behave abnormally on this, so let's ensure
        // we keep the configuration before initiating this command.

        if (auto result = do_wait_then_write(I8042Port::Command, I8042Command::ReadConfiguration); result.is_error()) {
            dbgln("I8042: Trying to read configuration failed during the existence test, error {}", result.error());
            return false;
        }

        {
            auto result = do_wait_then_read_any_input(I8042Port::Buffer);
            if (result.is_error()) {
                dbgln("I8042: Trying to read configuration failed during the existence test, error {}", result.error());
                return false;
            }
            configuration = result.release_value();
        }

        bool successful_self_test = false;
        for (int attempt = 0; attempt < 20; attempt++) {
            do_write(I8042Port::Command, I8042Command::TestPS2Controller);
            if (do_read(I8042Port::Buffer) == I8042Response::ControllerTestPassed) {
                successful_self_test = true;
                break;
            }
            // Note: Wait 500 microseconds in case the controller couldn't respond
            microseconds_delay(500);
        }
        if (!successful_self_test) {
            dbgln("I8042: Trying to probe for existence of controller failed");
            return false;
        }

        if (auto result = do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration); result.is_error()) {
            dbgln("I8042: Trying to restore configuration after self-test failed with error {}", result.error());
            return false;
        }

        if (auto result = do_wait_then_write(I8042Port::Buffer, configuration); result.is_error()) {
            dbgln("I8042: Trying to write restored configuration after self-test failed with error {}", result.error());
            return false;
        }

        return true;
    }
}

UNMAP_AFTER_INIT ErrorOr<void> I8042Controller::detect_devices(EnableKeyboardFirstPortTranslation enable_first_port_translation)
{
    u8 configuration;
    {
        SpinlockLocker lock(m_lock);
        // Note: This flushes all the garbage left in the controller registers
        TRY(drain_output_buffer());

        TRY(do_wait_then_write(I8042Port::Command, I8042Command::DisableFirstPS2Port));
        TRY(do_wait_then_write(I8042Port::Command, I8042Command::DisableSecondPS2Port)); // ignored if it doesn't exist

        TRY(do_wait_then_write(I8042Port::Command, I8042Command::ReadConfiguration));
        configuration = TRY(do_wait_then_read_any_input(I8042Port::Buffer));
        configuration &= ~I8042ConfigurationFlag::FirstPS2PortInterrupt;
        configuration &= ~I8042ConfigurationFlag::SecondPS2PortInterrupt;

        // FIXME: Don't enable translation for the first i8042 port if nothing is connected
        // or even worse - a mouse device, because we will get garbage data.
        if (enable_first_port_translation == EnableKeyboardFirstPortTranslation::Yes)
            configuration |= I8042ConfigurationFlag::FirstPS2PortTranslation;
        else
            configuration &= ~I8042ConfigurationFlag::FirstPS2PortTranslation;

        TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
        TRY(do_wait_then_write(I8042Port::Buffer, configuration));

        // Perform controller self-test
        TRY(do_wait_then_write(I8042Port::Command, I8042Command::TestPS2Controller));
        auto self_test_result = TRY(do_wait_then_read_any_input(I8042Port::Buffer));
        if (self_test_result == I8042Response::ControllerTestPassed) {
            // Restore configuration in case the controller reset
            TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
            TRY(do_wait_then_write(I8042Port::Buffer, configuration));
        } else {
            dbgln("I8042: Controller self test failed");
            return Error::from_errno(EIO);
        }

        m_is_dual_channel = (configuration & I8042ConfigurationFlag::SecondPS2PortClock) != 0;
        dbgln("I8042: {} channel controller", m_is_dual_channel ? "Dual" : "Single");

        // Test ports and enable them if available
        TRY(do_wait_then_write(I8042Port::Command, I8042Command::TestFirstPS2Port));
        auto first_port_test_result = TRY(do_wait_then_read_any_input(I8042Port::Buffer));
        m_first_port_available = (first_port_test_result == 0);

        if (m_first_port_available) {
            TRY(do_wait_then_write(I8042Port::Command, I8042Command::EnableFirstPS2Port));
            configuration |= I8042ConfigurationFlag::FirstPS2PortInterrupt;
            configuration &= ~I8042ConfigurationFlag::FirstPS2PortClock;
        } else {
            dbgln("I8042: Keyboard port not available");
        }

        TRY(drain_output_buffer());

        if (m_is_dual_channel) {
            TRY(do_wait_then_write(I8042Port::Command, I8042Command::TestSecondPS2Port));
            auto test_second_port_result = TRY(do_wait_then_read_any_input(I8042Port::Buffer));
            m_second_port_available = (test_second_port_result == 0);
            if (m_second_port_available) {
                TRY(do_wait_then_write(I8042Port::Command, I8042Command::EnableSecondPS2Port));
                configuration |= I8042ConfigurationFlag::SecondPS2PortInterrupt;
                configuration &= ~I8042ConfigurationFlag::SecondPS2PortClock;
            } else {
                dbgln("I8042: Mouse port not available");
            }
        }

        // Enable IRQs for the ports that are usable
        if (m_first_port_available || m_second_port_available) {
            configuration &= ~I8042ConfigurationFlag::FirstPS2PortClock;
            configuration &= ~I8042ConfigurationFlag::SecondPS2PortClock;
            TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
            TRY(do_wait_then_write(I8042Port::Buffer, configuration));
        }
    }

    // Try to detect and initialize the devices
    if (m_first_port_available) {
        // FIXME: Actually figure out the connected PS2 device type
        m_first_ps2_port.device_type = PS2DeviceType::StandardKeyboard;
        auto keyboard_device = TRY(KeyboardDevice::try_to_initialize());
        // FIXME: Determine if the user wants to operate in scan code set 3.
        auto keyboard_device_scan_code_set = enable_first_port_translation == EnableKeyboardFirstPortTranslation::Yes ? ScanCodeSet::Set1 : ScanCodeSet::Set2;
        auto error_or_device = PS2KeyboardDevice::try_to_initialize(*this, I8042PortIndex::FirstPort, keyboard_device_scan_code_set, *keyboard_device);
        if (error_or_device.is_error()) {
            dbgln("I8042: Keyboard device failed to initialize, disable");
            m_first_port_available = false;
            configuration &= ~I8042ConfigurationFlag::FirstPS2PortInterrupt;
            configuration |= I8042ConfigurationFlag::FirstPS2PortClock;
            m_first_ps2_port.device = nullptr;
            m_first_ps2_port.device_type = {};
            SpinlockLocker lock(m_lock);
            // NOTE: Before setting the actual scan code set, stop packet streaming entirely.
            TRY(do_send_command(I8042PortIndex::FirstPort, I8042Command::DisablePacketStreaming));
            TRY(do_wait_then_write(I8042Port::Buffer, I8042Command::SetScanCodeSet));
            TRY(do_wait_then_write(I8042Port::Buffer, 0x2));

            TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
            TRY(do_wait_then_write(I8042Port::Buffer, configuration));
        } else {
            m_first_ps2_port.device = error_or_device.release_value();
        }
    }
    if (m_second_port_available && !kernel_command_line().disable_ps2_mouse()) {
        // FIXME: Actually figure out the connected PS2 device type
        m_second_ps2_port.device_type = PS2DeviceType::StandardMouse;
        auto mouse_device = TRY(MouseDevice::try_to_initialize());
        auto vmmouse_device_or_error = VMWareMouseDevice::try_to_initialize(*this, I8042PortIndex::SecondPort, *mouse_device);
        if (vmmouse_device_or_error.is_error()) {
            // FIXME: is there something to do with the VMWare errors?
            auto mouse_device_or_error = PS2MouseDevice::try_to_initialize(*this, I8042PortIndex::SecondPort, *mouse_device);
            if (mouse_device_or_error.is_error()) {
                dbgln("I8042: Mouse device failed to initialize, disable");
                m_second_port_available = false;
                configuration |= I8042ConfigurationFlag::SecondPS2PortClock;
                m_second_ps2_port.device = nullptr;
                m_second_ps2_port.device_type = {};
                SpinlockLocker lock(m_lock);
                TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
                TRY(do_wait_then_write(I8042Port::Buffer, configuration));
            } else {
                m_second_ps2_port.device = mouse_device_or_error.release_value();
            }
        } else {
            m_second_ps2_port.device = vmmouse_device_or_error.release_value();
        }
    }

    m_irq_handlers[0] = TRY(I8042ControllerIRQHandler::try_create(*this, IRQ_FIRST_PORT));
    m_irq_handlers[1] = TRY(I8042ControllerIRQHandler::try_create(*this, IRQ_SECOND_PORT));

    // Enable IRQs after both are detected and initialized
    if (m_first_ps2_port.device)
        m_irq_handlers[0]->enable_irq();
    if (m_second_ps2_port.device)
        m_irq_handlers[1]->enable_irq();
    return {};
}

ErrorOr<void> I8042Controller::send_command(PortIndex port_index, DeviceCommand command)
{
    switch (command) {
    case DeviceCommand::GetDeviceID: {
        SpinlockLocker lock(m_lock);
        return do_send_command(port_index, I8042Command::GetDeviceID);
    }
    case DeviceCommand::EnablePacketStreaming: {
        SpinlockLocker lock(m_lock);
        return do_send_command(port_index, I8042Command::EnablePacketStreaming);
    }
    case DeviceCommand::DisablePacketStreaming: {
        SpinlockLocker lock(m_lock);
        return do_send_command(port_index, I8042Command::DisablePacketStreaming);
    }
    case DeviceCommand::SetDefaults: {
        SpinlockLocker lock(m_lock);
        return do_send_command(port_index, I8042Command::SetDefaults);
    }

    // NOTE: The sample rate command is supported only with sending data byte with it!
    case DeviceCommand::SetSampleRate:
        return EOPNOTSUPP;

    default:
        return EINVAL;
    }
}

ErrorOr<void> I8042Controller::send_command(PortIndex port_index, DeviceCommand command, u8 data)
{
    switch (command) {
    // NOTE: Only the sample rate command supports sending data byte with it!
    case DeviceCommand::SetSampleRate: {
        SpinlockLocker lock(m_lock);
        return do_send_command(port_index, I8042Command::SetSampleRate, data);
    }

    case DeviceCommand::GetDeviceID:
        return EOPNOTSUPP;
    case DeviceCommand::EnablePacketStreaming:
        return EOPNOTSUPP;
    case DeviceCommand::DisablePacketStreaming:
        return EOPNOTSUPP;
    case DeviceCommand::SetDefaults:
        return EOPNOTSUPP;

    default:
        return EINVAL;
    }
}

bool I8042Controller::irq_process_input_buffer(PortIndex port_index)
{
    VERIFY(Processor::current_in_irq());

    u8 status = IO::in8(I8042Port::Status);
    if (!(status & I8042StatusFlag::OutputBuffer))
        return false;
    u8 byte = IO::in8(I8042Port::Buffer);

    PS2Port* selected_port = nullptr;
    if (port_index == I8042PortIndex::FirstPort)
        selected_port = &m_first_ps2_port;
    else if (port_index == I8042PortIndex::SecondPort)
        selected_port = &m_second_ps2_port;
    else
        return false;

    if (!selected_port->device)
        return false;
    selected_port->device->handle_byte_read_from_serial_input(byte);
    return true;
}

ErrorOr<void> I8042Controller::drain_output_buffer()
{
    for (int attempt = 0; attempt < 50; attempt++) {
        u8 status = IO::in8(I8042Port::Status);
        if (!(status & I8042StatusFlag::OutputBuffer))
            return {};
        IO::in8(I8042Port::Buffer);

        microseconds_delay(100);
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> I8042Controller::do_reset_device(PortIndex port_index)
{
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current_in_irq());
    TRY(do_send_command(port_index, I8042Command::Reset));
    // Wait until we get the self-test result
    auto self_test_result = TRY(do_wait_then_read_any_input(I8042Port::Buffer));

    // Acknowledge means that reset is still in progress.
    // Consume it and wait a bit longer
    if (self_test_result == I8042Response::Acknowledge)
        self_test_result = TRY(do_wait_then_read_any_input(I8042Port::Buffer));

    // FIXME: Is this the correct errno value for this?
    if (self_test_result != I8042Response::Success)
        return Error::from_errno(EIO);
    return {};
}

ErrorOr<void> I8042Controller::do_send_command(PortIndex port_index, u8 command)
{
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current_in_irq());
    TRY(do_write_to_device(port_index, command));
    return {};
}

ErrorOr<void> I8042Controller::do_send_command(PortIndex port_index, u8 command, u8 data)
{
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current_in_irq());

    TRY(do_write_to_device(port_index, command));
    TRY(do_write_to_device(port_index, data));
    return {};
}

ErrorOr<void> I8042Controller::do_write_to_device(PortIndex port_index, u8 data)
{
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current_in_irq());

    int attempts = 0;
    u8 response;
    do {
        if (port_index != I8042PortIndex::FirstPort) {
            VERIFY(port_index == I8042PortIndex::SecondPort);
            TRY(prepare_for_any_output());
            IO::out8(I8042Port::Command, I8042Command::WriteSecondPS2PortInputBuffer);
        }
        TRY(prepare_for_any_output());
        IO::out8(I8042Port::Buffer, data);

        response = TRY(do_wait_then_read_any_input(I8042Port::Buffer));
    } while (response == I8042Response::Resend && ++attempts < 250);
    if (attempts >= 250 || response == I8042Response::Resend) {
        dbgln("I8042: Failed to write byte to device, gave up");
        return Error::from_errno(EBUSY);
    }
    return {};
}

ErrorOr<u8> I8042Controller::do_read_from_device(PortIndex port_index)
{
    TRY(prepare_for_input(port_index));
    return IO::in8(I8042Port::Buffer);
}

ErrorOr<void> I8042Controller::prepare_for_any_input()
{
    VERIFY(m_lock.is_locked());
    for (int attempt = 0; attempt < 1000; attempt++) {
        u8 status = IO::in8(I8042Port::Status);
        if (!(status & I8042StatusFlag::OutputBuffer)) {
            microseconds_delay(1000);
            continue;
        }
        return {};
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> I8042Controller::prepare_for_input(PortIndex port_index)
{
    VERIFY(m_lock.is_locked());
    u8 const second_port_flag = port_index == I8042PortIndex::FirstPort ? 0 : I8042StatusFlag::SecondPS2PortOutputBuffer;
    PS2Port* port = nullptr;
    if (port_index == I8042PortIndex::FirstPort)
        port = &m_first_ps2_port;
    else if (port_index == I8042PortIndex::SecondPort)
        port = &m_second_ps2_port;
    else
        return Error::from_errno(ENODEV);
    for (int attempt = 0; attempt < 1000; attempt++) {
        u8 status = IO::in8(I8042Port::Status);
        if (!(status & I8042StatusFlag::OutputBuffer)) {
            microseconds_delay(1000);
            continue;
        }
        if (!port->device_type.has_value() || port->device_type.value() == PS2DeviceType::Unknown)
            return {};
        if ((status & I8042StatusFlag::SecondPS2PortOutputBuffer) == second_port_flag)
            return {};
        microseconds_delay(1000);
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> I8042Controller::prepare_for_any_output()
{
    VERIFY(m_lock.is_locked());
    for (int attempt = 0; attempt < 250; attempt++) {
        u8 status = IO::in8(I8042Port::Status);
        if (!(status & I8042StatusFlag::InputBuffer))
            return {};
        microseconds_delay(1000);
    }
    return Error::from_errno(EBUSY);
}

UNMAP_AFTER_INIT void I8042Controller::do_write(u8 port, u8 data)
{
    VERIFY(m_lock.is_locked());
    IO::out8(port, data);
}

UNMAP_AFTER_INIT u8 I8042Controller::do_read(u8 port)
{
    VERIFY(m_lock.is_locked());
    return IO::in8(port);
}

ErrorOr<void> I8042Controller::do_wait_then_write(u8 port, u8 data)
{
    VERIFY(m_lock.is_locked());
    TRY(prepare_for_any_output());
    IO::out8(port, data);
    return {};
}

ErrorOr<u8> I8042Controller::do_wait_then_read_any_input(u8 port)
{
    VERIFY(m_lock.is_locked());
    TRY(prepare_for_any_input());
    return IO::in8(port);
}
}
