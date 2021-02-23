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

class I8042Device {
public:
    virtual ~I8042Device() { }

    virtual void irq_handle_byte_read(u8 byte) = 0;
    virtual void enable_interrupts() = 0;
};

class I8042Controller {
public:
    enum class Device {
        None = 0,
        Keyboard,
        Mouse
    };

    static void initialize();
    static I8042Controller& the();

    bool reset_device(Device device)
    {
        ScopedSpinLock lock(m_lock);
        return do_reset_device(device);
    }

    u8 send_command(Device device, u8 command)
    {
        ScopedSpinLock lock(m_lock);
        return do_send_command(device, command);
    }
    u8 send_command(Device device, u8 command, u8 data)
    {
        ScopedSpinLock lock(m_lock);
        return do_send_command(device, command, data);
    }

    u8 read_from_device(Device device)
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
    void prepare_for_input(Device);

    void irq_process_input_buffer(Device);

private:
    I8042Controller();
    void do_drain();
    bool do_reset_device(Device device);
    u8 do_send_command(Device device, u8 data);
    u8 do_send_command(Device device, u8 command, u8 data);
    u8 do_write_to_device(Device device, u8 data);
    u8 do_read_from_device(Device device);
    void do_wait_then_write(u8 port, u8 data);
    u8 do_wait_then_read(u8 port);

    static int device_to_deviceinfo_index(Device device)
    {
        VERIFY(device != Device::None);
        return (device == Device::Keyboard) ? 0 : 1;
    }

    struct DeviceInfo {
        I8042Device* device { nullptr };
        bool available { false };
    };

    SpinLock<u8> m_lock;
    bool m_is_dual_channel { false };
    DeviceInfo m_devices[2];
};

}
