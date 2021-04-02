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

#pragma once

#include <AK/RefCounted.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/Devices/HID/MouseDevice.h>
#include <Kernel/SpinLock.h>

namespace Kernel {

#define I8042_BUFFER 0x60
#define I8042_STATUS 0x64
#define I8042_ACK 0xFA
#define I8042_RESEND 0xFE
#define I8042_BUFFER_FULL 0x01

#define I8042_WHICH_BUFFER 0x20

#define I8042_KEYBOARD_BUFFER 0x00
#define I8042_MOUSE_BUFFER 0x20

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
class I8042Controller : public RefCounted<I8042Controller> {
    friend class PS2KeyboardDevice;
    friend class PS2MouseDevice;

public:
    static NonnullRefPtr<I8042Controller> initialize();

    void detect_devices();

    bool reset_device(HIDDevice::Type device)
    {
        ScopedSpinLock lock(m_lock);
        return do_reset_device(device);
    }

    u8 send_command(HIDDevice::Type device, u8 command)
    {
        ScopedSpinLock lock(m_lock);
        return do_send_command(device, command);
    }
    u8 send_command(HIDDevice::Type device, u8 command, u8 data)
    {
        ScopedSpinLock lock(m_lock);
        return do_send_command(device, command, data);
    }

    u8 read_from_device(HIDDevice::Type device)
    {
        ScopedSpinLock lock(m_lock);
        return do_read_from_device(device);
    }

    void wait_then_write(u8 port, u8 data)
    {
        ScopedSpinLock lock(m_lock);
        do_wait_then_write(port, data);
    }

    u8 wait_then_read(u8 port)
    {
        ScopedSpinLock lock(m_lock);
        return do_wait_then_read(port);
    }

    void prepare_for_output();
    void prepare_for_input(HIDDevice::Type);

    void irq_process_input_buffer(HIDDevice::Type);

    RefPtr<MouseDevice> mouse() const;
    RefPtr<KeyboardDevice> keyboard() const;

private:
    I8042Controller();
    void do_drain();
    bool do_reset_device(HIDDevice::Type);
    u8 do_send_command(HIDDevice::Type type, u8 data);
    u8 do_send_command(HIDDevice::Type device, u8 command, u8 data);
    u8 do_write_to_device(HIDDevice::Type device, u8 data);
    u8 do_read_from_device(HIDDevice::Type device);
    void do_wait_then_write(u8 port, u8 data);
    u8 do_wait_then_read(u8 port);

    SpinLock<u8> m_lock;
    bool m_first_port_available { false };
    bool m_second_port_available { false };
    bool m_is_dual_channel { false };
    RefPtr<MouseDevice> m_mouse_device;
    RefPtr<KeyboardDevice> m_keyboard_device;
};

}
