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

#include <Kernel/ACPI/Parser.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/Devices/PS2MouseDevice.h>
#include <Kernel/IO.h>

namespace Kernel {

static I8042Controller* s_the;

UNMAP_AFTER_INIT void I8042Controller::initialize()
{
    if (ACPI::Parser::the()->have_8042())
        new I8042Controller;
}

I8042Controller& I8042Controller::the()
{
    VERIFY(s_the);
    return *s_the;
}

UNMAP_AFTER_INIT I8042Controller::I8042Controller()
{
    VERIFY(!s_the);
    s_the = this;

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
        m_devices[0].available = (do_wait_then_read(I8042_BUFFER) == 0);

        if (m_devices[0].available) {
            do_wait_then_write(I8042_STATUS, 0xae); //enable
            configuration |= 1;
            configuration &= ~(1 << 4);
        } else {
            dbgln("I8042: Keyboard port not available");
        }

        if (m_is_dual_channel) {
            do_wait_then_write(I8042_STATUS, 0xa9); // test
            m_devices[1].available = (do_wait_then_read(I8042_BUFFER) == 0);
            if (m_devices[1].available) {
                do_wait_then_write(I8042_STATUS, 0xa8); // enable
                configuration |= 2;
                configuration &= ~(1 << 5);
            } else {
                dbgln("I8042: Mouse port not available");
            }
        }

        // Enable IRQs for the ports that are usable
        if (m_devices[0].available || m_devices[1].available) {
            configuration &= ~0x30; // renable clocks
            do_wait_then_write(I8042_STATUS, 0x60);
            do_wait_then_write(I8042_BUFFER, configuration);
        }
    }

    // Try to detect and initialize the devices
    if (m_devices[0].available) {
        if (KeyboardDevice::the().initialize()) {
            m_devices[0].device = &KeyboardDevice::the();
        } else {
            dbgln("I8042: Keyboard device failed to initialize, disable");
            m_devices[0].available = false;
            configuration &= ~1;
            configuration |= 1 << 4;
            ScopedSpinLock lock(m_lock);
            do_wait_then_write(I8042_STATUS, 0x60);
            do_wait_then_write(I8042_BUFFER, configuration);
        }
    }
    if (m_devices[1].available) {
        if (PS2MouseDevice::the().initialize()) {
            m_devices[1].device = &PS2MouseDevice::the();
        } else {
            dbgln("I8042: Mouse device failed to initialize, disable");
            m_devices[1].available = false;
            configuration |= 1 << 5;
            ScopedSpinLock lock(m_lock);
            do_wait_then_write(I8042_STATUS, 0x60);
            do_wait_then_write(I8042_BUFFER, configuration);
        }
    }

    // Enable IRQs after both are detected and initialized
    if (m_devices[0].device)
        m_devices[0].device->enable_interrupts();
    if (m_devices[1].device)
        m_devices[1].device->enable_interrupts();
}

void I8042Controller::irq_process_input_buffer(Device)
{
    VERIFY(Processor::current().in_irq());

    u8 status = IO::in8(I8042_STATUS);
    if (!(status & I8042_BUFFER_FULL))
        return;
    Device data_for_device = ((status & I8042_WHICH_BUFFER) == I8042_MOUSE_BUFFER) ? Device::Mouse : Device::Keyboard;
    u8 byte = IO::in8(I8042_BUFFER);
    if (auto* device = m_devices[data_for_device == Device::Keyboard ? 0 : 1].device)
        device->irq_handle_byte_read(byte);
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

bool I8042Controller::do_reset_device(Device device)
{
    VERIFY(device != Device::None);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current().in_irq());
    if (do_send_command(device, 0xff) != I8042_ACK)
        return false;
    // Wait until we get the self-test result
    return do_wait_then_read(I8042_BUFFER) == 0xaa;
}

u8 I8042Controller::do_send_command(Device device, u8 command)
{
    VERIFY(device != Device::None);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current().in_irq());

    return do_write_to_device(device, command);
}

u8 I8042Controller::do_send_command(Device device, u8 command, u8 data)
{
    VERIFY(device != Device::None);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current().in_irq());

    u8 response = do_write_to_device(device, command);
    if (response == I8042_ACK)
        response = do_write_to_device(device, data);
    return response;
}

u8 I8042Controller::do_write_to_device(Device device, u8 data)
{
    VERIFY(device != Device::None);
    VERIFY(m_lock.is_locked());

    VERIFY(!Processor::current().in_irq());

    int attempts = 0;
    u8 response;
    do {
        if (device != Device::Keyboard) {
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

u8 I8042Controller::do_read_from_device(Device device)
{
    VERIFY(device != Device::None);

    prepare_for_input(device);
    return IO::in8(I8042_BUFFER);
}

void I8042Controller::prepare_for_input(Device device)
{
    VERIFY(m_lock.is_locked());
    const u8 buffer_type = device == Device::Keyboard ? I8042_KEYBOARD_BUFFER : I8042_MOUSE_BUFFER;
    for (;;) {
        u8 status = IO::in8(I8042_STATUS);
        if ((status & I8042_BUFFER_FULL) && (device == Device::None || ((status & I8042_WHICH_BUFFER) == buffer_type)))
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
    prepare_for_input(Device::None);
    return IO::in8(port);
}

}
