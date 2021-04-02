/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Devices/HID/I8042Controller.h>
#include <Kernel/Devices/HID/PS2KeyboardDevice.h>
#include <Kernel/Devices/HID/PS2MouseDevice.h>
#include <Kernel/Devices/HID/VMWareMouseDevice.h>
#include <Kernel/IO.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<I8042Controller> I8042Controller::initialize()
{
    return adopt(*new I8042Controller());
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

UNMAP_AFTER_INIT void I8042Controller::detect_devices()
{
    u8 configuration;
    {
        ScopedSpinLock lock(m_lock);
        // Disable devices
        do_wait_then_write(I8042_STATUS, 0xad);
        do_wait_then_write(I8042_STATUS, 0xa7); // ignored if it doesn't exist

        // Drain buffers
        do_drain();

        do_wait_then_write(I8042_STATUS, 0x20);
        configuration = do_wait_then_read(I8042_BUFFER);
        do_wait_then_write(I8042_STATUS, 0x60);
        configuration &= ~3; // Disable IRQs for all
        do_wait_then_write(I8042_BUFFER, configuration);

        m_is_dual_channel = (configuration & (1 << 5)) != 0;
        dbgln("I8042: {} channel controller",
            m_is_dual_channel ? "Dual" : "Single");

        // Perform controller self-test
        do_wait_then_write(I8042_STATUS, 0xaa);
        if (do_wait_then_read(I8042_BUFFER) == 0x55) {
            // Restore configuration in case the controller reset
            do_wait_then_write(I8042_STATUS, 0x60);
            do_wait_then_write(I8042_BUFFER, configuration);
        } else {
            dbgln("I8042: Controller self test failed");
        }

        // Test ports and enable them if available
        do_wait_then_write(I8042_STATUS, 0xab); // test
        m_first_port_available = (do_wait_then_read(I8042_BUFFER) == 0);

        if (m_first_port_available) {
            do_wait_then_write(I8042_STATUS, 0xae); //enable
            configuration |= 1;
            configuration &= ~(1 << 4);
        } else {
            dbgln("I8042: Keyboard port not available");
        }

        if (m_is_dual_channel) {
            do_wait_then_write(I8042_STATUS, 0xa9); // test
            m_second_port_available = (do_wait_then_read(I8042_BUFFER) == 0);
            if (m_second_port_available) {
                do_wait_then_write(I8042_STATUS, 0xa8); // enable
                configuration |= 2;
                configuration &= ~(1 << 5);
            } else {
                dbgln("I8042: Mouse port not available");
            }
        }

        // Enable IRQs for the ports that are usable
        if (m_first_port_available || m_second_port_available) {
            configuration &= ~0x30; // renable clocks
            do_wait_then_write(I8042_STATUS, 0x60);
            do_wait_then_write(I8042_BUFFER, configuration);
        }
    }

    // Try to detect and initialize the devices
    if (m_first_port_available) {
        m_keyboard_device = PS2KeyboardDevice::try_to_initialize(*this);
        if (!m_keyboard_device) {
            dbgln("I8042: Keyboard device failed to initialize, disable");
            m_first_port_available = false;
            configuration &= ~1;
            configuration |= 1 << 4;
            ScopedSpinLock lock(m_lock);
            do_wait_then_write(I8042_STATUS, 0x60);
            do_wait_then_write(I8042_BUFFER, configuration);
        }
    }
    if (m_second_port_available) {
        m_mouse_device = VMWareMouseDevice::try_to_initialize(*this);
        if (!m_mouse_device) {
            m_mouse_device = PS2MouseDevice::try_to_initialize(*this);
            if (!m_mouse_device) {
                dbgln("I8042: Mouse device failed to initialize, disable");
                m_second_port_available = false;
                configuration |= 1 << 5;
                ScopedSpinLock lock(m_lock);
                do_wait_then_write(I8042_STATUS, 0x60);
                do_wait_then_write(I8042_BUFFER, configuration);
            }
        }
    }

    // Enable IRQs after both are detected and initialized
    if (m_keyboard_device)
        m_keyboard_device->enable_interrupts();
    if (m_mouse_device)
        m_mouse_device->enable_interrupts();
}

void I8042Controller::irq_process_input_buffer(HIDDevice::Type)
{
    VERIFY(Processor::current().in_irq());

    u8 status = IO::in8(I8042_STATUS);
    if (!(status & I8042_BUFFER_FULL))
        return;
    HIDDevice::Type data_for_device = ((status & I8042_WHICH_BUFFER) == I8042_MOUSE_BUFFER) ? HIDDevice::Type::Mouse : HIDDevice::Type::Keyboard;
    u8 byte = IO::in8(I8042_BUFFER);
    if (data_for_device == HIDDevice::Type::Mouse) {
        VERIFY(m_mouse_device);
        static_cast<PS2MouseDevice&>(*m_mouse_device).irq_handle_byte_read(byte);
        return;
    }
    if (data_for_device == HIDDevice::Type::Keyboard) {
        VERIFY(m_keyboard_device);
        static_cast<PS2KeyboardDevice&>(*m_keyboard_device).irq_handle_byte_read(byte);
        return;
    }
}

void I8042Controller::do_drain()
{
    for (;;) {
        u8 status = IO::in8(I8042_STATUS);
        if (!(status & I8042_BUFFER_FULL))
            return;
        IO::in8(I8042_BUFFER);
    }
}

bool I8042Controller::do_reset_device(HIDDevice::Type device)
{
    VERIFY(device != HIDDevice::Type::Unknown);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current().in_irq());
    if (do_send_command(device, 0xff) != I8042_ACK)
        return false;
    // Wait until we get the self-test result
    return do_wait_then_read(I8042_BUFFER) == 0xaa;
}

u8 I8042Controller::do_send_command(HIDDevice::Type device, u8 command)
{
    VERIFY(device != HIDDevice::Type::Unknown);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current().in_irq());

    return do_write_to_device(device, command);
}

u8 I8042Controller::do_send_command(HIDDevice::Type device, u8 command, u8 data)
{
    VERIFY(device != HIDDevice::Type::Unknown);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current().in_irq());

    u8 response = do_write_to_device(device, command);
    if (response == I8042_ACK)
        response = do_write_to_device(device, data);
    return response;
}

u8 I8042Controller::do_write_to_device(HIDDevice::Type device, u8 data)
{
    VERIFY(device != HIDDevice::Type::Unknown);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current().in_irq());

    int attempts = 0;
    u8 response;
    do {
        if (device != HIDDevice::Type::Keyboard) {
            prepare_for_output();
            IO::out8(I8042_STATUS, 0xd4);
        }
        prepare_for_output();
        IO::out8(I8042_BUFFER, data);

        response = do_wait_then_read(I8042_BUFFER);
    } while (response == I8042_RESEND && ++attempts < 3);
    if (attempts >= 3)
        dbgln("Failed to write byte to device, gave up");
    return response;
}

u8 I8042Controller::do_read_from_device(HIDDevice::Type device)
{
    VERIFY(device != HIDDevice::Type::Unknown);

    prepare_for_input(device);
    return IO::in8(I8042_BUFFER);
}

void I8042Controller::prepare_for_input(HIDDevice::Type device)
{
    VERIFY(m_lock.is_locked());
    const u8 buffer_type = device == HIDDevice::Type::Keyboard ? I8042_KEYBOARD_BUFFER : I8042_MOUSE_BUFFER;
    for (;;) {
        u8 status = IO::in8(I8042_STATUS);
        if ((status & I8042_BUFFER_FULL) && (device == HIDDevice::Type::Unknown || ((status & I8042_WHICH_BUFFER) == buffer_type)))
            return;
    }
}

void I8042Controller::prepare_for_output()
{
    VERIFY(m_lock.is_locked());
    for (;;) {
        if (!(IO::in8(I8042_STATUS) & 2))
            return;
    }
}

void I8042Controller::do_wait_then_write(u8 port, u8 data)
{
    VERIFY(m_lock.is_locked());
    prepare_for_output();
    IO::out8(port, data);
}

u8 I8042Controller::do_wait_then_read(u8 port)
{
    VERIFY(m_lock.is_locked());
    prepare_for_input(HIDDevice::Type::Unknown);
    return IO::in8(port);
}

}
