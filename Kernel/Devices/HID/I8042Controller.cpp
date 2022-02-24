/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Devices/HID/I8042Controller.h>
#include <Kernel/Devices/HID/PS2KeyboardDevice.h>
#include <Kernel/Devices/HID/PS2MouseDevice.h>
#include <Kernel/Devices/HID/VMWareMouseDevice.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<I8042Controller> I8042Controller::initialize()
{
    return adopt_ref(*new I8042Controller());
}

RefPtr<MouseDevice> I8042Controller::mouse() const
{
    return m_mouse_device;
}
RefPtr<KeyboardDevice> I8042Controller::keyboard() const
{
    return m_keyboard_device;
}

UNMAP_AFTER_INIT I8042Controller::I8042Controller()
{
}

UNMAP_AFTER_INIT bool I8042Controller::check_existence_via_probing(Badge<HIDManagement>)
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
            auto result = do_wait_then_read(I8042Port::Buffer);
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
            IO::delay(500);
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

UNMAP_AFTER_INIT ErrorOr<void> I8042Controller::detect_devices()
{

    u8 configuration;
    {
        SpinlockLocker lock(m_lock);
        // Note: This flushes all the garbage left in the controller registers
        TRY(drain_output_buffer());

        TRY(do_wait_then_write(I8042Port::Command, I8042Command::DisableFirstPS2Port));
        TRY(do_wait_then_write(I8042Port::Command, I8042Command::DisableSecondPS2Port)); // ignored if it doesn't exist

        TRY(do_wait_then_write(I8042Port::Command, I8042Command::ReadConfiguration));
        configuration = TRY(do_wait_then_read(I8042Port::Buffer));
        TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
        configuration &= ~I8042ConfigurationFlag::FirstPS2PortInterrupt;
        configuration &= ~I8042ConfigurationFlag::SecondPS2PortInterrupt;
        TRY(do_wait_then_write(I8042Port::Buffer, configuration));

        m_is_dual_channel = (configuration & I8042ConfigurationFlag::SecondPS2PortClock) != 0;
        dbgln("I8042: {} channel controller", m_is_dual_channel ? "Dual" : "Single");

        // Perform controller self-test
        TRY(do_wait_then_write(I8042Port::Command, I8042Command::TestPS2Controller));
        auto self_test_result = TRY(do_wait_then_read(I8042Port::Buffer));
        if (self_test_result == I8042Response::ControllerTestPassed) {
            // Restore configuration in case the controller reset
            TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
            TRY(do_wait_then_write(I8042Port::Buffer, configuration));
        } else {
            dbgln("I8042: Controller self test failed");
        }

        // Test ports and enable them if available
        TRY(do_wait_then_write(I8042Port::Command, I8042Command::TestFirstPS2Port));
        auto first_port_test_result = TRY(do_wait_then_read(I8042Port::Buffer));
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
            auto test_second_port_result = TRY(do_wait_then_read(I8042Port::Buffer));
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
        m_keyboard_device = PS2KeyboardDevice::try_to_initialize(*this);
        if (!m_keyboard_device) {
            dbgln("I8042: Keyboard device failed to initialize, disable");
            m_first_port_available = false;
            configuration &= ~I8042ConfigurationFlag::FirstPS2PortInterrupt;
            configuration |= I8042ConfigurationFlag::FirstPS2PortClock;
            SpinlockLocker lock(m_lock);
            TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
            TRY(do_wait_then_write(I8042Port::Buffer, configuration));
        }
    }
    if (m_second_port_available) {
        m_mouse_device = VMWareMouseDevice::try_to_initialize(*this);
        if (!m_mouse_device) {
            m_mouse_device = PS2MouseDevice::try_to_initialize(*this);
            if (!m_mouse_device) {
                dbgln("I8042: Mouse device failed to initialize, disable");
                m_second_port_available = false;
                configuration |= I8042ConfigurationFlag::SecondPS2PortClock;
                SpinlockLocker lock(m_lock);
                TRY(do_wait_then_write(I8042Port::Command, I8042Command::WriteConfiguration));
                TRY(do_wait_then_write(I8042Port::Buffer, configuration));
            }
        }
    }

    // Enable IRQs after both are detected and initialized
    if (m_keyboard_device)
        m_keyboard_device->enable_interrupts();
    if (m_mouse_device)
        m_mouse_device->enable_interrupts();
    return {};
}

bool I8042Controller::irq_process_input_buffer(HIDDevice::Type instrument_type)
{
    VERIFY(Processor::current_in_irq());

    u8 status = IO::in8(I8042Port::Status);
    if (!(status & I8042StatusFlag::OutputBuffer))
        return false;
    u8 byte = IO::in8(I8042Port::Buffer);
    if (instrument_type == HIDDevice::Type::Mouse) {
        VERIFY(m_mouse_device);
        static_cast<PS2MouseDevice&>(*m_mouse_device).irq_handle_byte_read(byte);
        return true;
    }
    if (instrument_type == HIDDevice::Type::Keyboard) {
        VERIFY(m_keyboard_device);
        static_cast<PS2KeyboardDevice&>(*m_keyboard_device).irq_handle_byte_read(byte);
        return true;
    }
    return false;
}

ErrorOr<void> I8042Controller::drain_output_buffer()
{
    for (int attempt = 0; attempt < 50; attempt++) {
        u8 status = IO::in8(I8042Port::Status);
        if (!(status & I8042StatusFlag::OutputBuffer))
            return {};
        IO::in8(I8042Port::Buffer);

        IO::delay(100);
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> I8042Controller::do_reset_device(HIDDevice::Type device)
{
    VERIFY(device != HIDDevice::Type::Unknown);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current_in_irq());
    auto reset_result = TRY(do_send_command(device, I8042Command::Reset));
    // FIXME: Is this the correct errno value for this?
    if (reset_result != I8042Response::Acknowledge)
        return Error::from_errno(EIO);
    // Wait until we get the self-test result
    auto self_test_result = TRY(do_wait_then_read(I8042Port::Buffer));

    // FIXME: Is this the correct errno value for this?
    if (self_test_result != I8042Response::Success)
        return Error::from_errno(EIO);
    return {};
}

ErrorOr<u8> I8042Controller::do_send_command(HIDDevice::Type device, u8 command)
{
    VERIFY(device != HIDDevice::Type::Unknown);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current_in_irq());

    return do_write_to_device(device, command);
}

ErrorOr<u8> I8042Controller::do_send_command(HIDDevice::Type device, u8 command, u8 data)
{
    VERIFY(device != HIDDevice::Type::Unknown);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current_in_irq());

    u8 response = TRY(do_write_to_device(device, command));
    if (response == I8042Response::Acknowledge)
        response = TRY(do_write_to_device(device, data));
    return response;
}

ErrorOr<u8> I8042Controller::do_write_to_device(HIDDevice::Type device, u8 data)
{
    VERIFY(device != HIDDevice::Type::Unknown);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current_in_irq());

    int attempts = 0;
    u8 response;
    do {
        if (device != HIDDevice::Type::Keyboard) {
            TRY(prepare_for_output());
            IO::out8(I8042Port::Command, I8042Command::WriteSecondPS2PortInputBuffer);
        }
        TRY(prepare_for_output());
        IO::out8(I8042Port::Buffer, data);

        response = TRY(do_wait_then_read(I8042Port::Buffer));
    } while (response == I8042Response::Resend && ++attempts < 3);
    if (attempts >= 3)
        dbgln("Failed to write byte to device, gave up");
    return response;
}

ErrorOr<u8> I8042Controller::do_read_from_device(HIDDevice::Type device)
{
    VERIFY(device != HIDDevice::Type::Unknown);

    TRY(prepare_for_input(device));
    return IO::in8(I8042Port::Buffer);
}

ErrorOr<void> I8042Controller::prepare_for_input(HIDDevice::Type device)
{
    VERIFY(m_lock.is_locked());
    u8 const second_port_flag = device == HIDDevice::Type::Keyboard ? 0 : I8042StatusFlag::SecondPS2PortOutputBuffer;
    for (int attempt = 0; attempt < 250; attempt++) {
        u8 status = IO::in8(I8042Port::Status);
        if (!(status & I8042StatusFlag::OutputBuffer)) {
            IO::delay(1000);
            continue;
        }
        if (device == HIDDevice::Type::Unknown)
            return {};
        if ((status & I8042StatusFlag::SecondPS2PortOutputBuffer) == second_port_flag)
            return {};
        IO::delay(1000);
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> I8042Controller::prepare_for_output()
{
    VERIFY(m_lock.is_locked());
    for (int attempt = 0; attempt < 250; attempt++) {
        u8 status = IO::in8(I8042Port::Status);
        if (!(status & I8042StatusFlag::InputBuffer))
            return {};
        IO::delay(1000);
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
    TRY(prepare_for_output());
    IO::out8(port, data);
    return {};
}

ErrorOr<u8> I8042Controller::do_wait_then_read(u8 port)
{
    VERIFY(m_lock.is_locked());
    TRY(prepare_for_input(HIDDevice::Type::Unknown));
    return IO::in8(port);
}

}
