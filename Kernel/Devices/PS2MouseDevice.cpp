/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Memory.h>
#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/PS2MouseDevice.h>
#include <Kernel/Devices/VMWareBackdoor.h>
#include <Kernel/IO.h>

namespace Kernel {

#define IRQ_MOUSE 12

#define PS2MOUSE_SET_RESOLUTION 0xE8
#define PS2MOUSE_STATUS_REQUEST 0xE9
#define PS2MOUSE_REQUEST_SINGLE_PACKET 0xEB
#define PS2MOUSE_GET_DEVICE_ID 0xF2
#define PS2MOUSE_SET_SAMPLE_RATE 0xF3
#define PS2MOUSE_ENABLE_PACKET_STREAMING 0xF4
#define PS2MOUSE_DISABLE_PACKET_STREAMING 0xF5
#define PS2MOUSE_SET_DEFAULTS 0xF6
#define PS2MOUSE_RESEND 0xFE
#define PS2MOUSE_RESET 0xFF

#define PS2MOUSE_INTELLIMOUSE_ID 0x03
#define PS2MOUSE_INTELLIMOUSE_EXPLORER_ID 0x04

static AK::Singleton<PS2MouseDevice> s_the;

PS2MouseDevice::PS2MouseDevice()
    : IRQHandler(IRQ_MOUSE)
    , CharacterDevice(10, 1)
    , m_controller(I8042Controller::the())
{
}

PS2MouseDevice::~PS2MouseDevice()
{
}

PS2MouseDevice& PS2MouseDevice::the()
{
    return *s_the;
}

void PS2MouseDevice::handle_irq(const RegisterState&)
{
    // The controller will read the data and call irq_handle_byte_read
    // for the appropriate device
    m_controller.irq_process_input_buffer(I8042Controller::Device::Mouse);
}

void PS2MouseDevice::irq_handle_byte_read(u8 byte)
{
    auto* backdoor = VMWareBackdoor::the();

    if (backdoor && backdoor->vmmouse_is_absolute()) {
        // We won't receive complete packets with the backdoor enabled,
        // we will only get one byte for each event, which we'll just
        // discard. If we were to wait until we *think* that we got a
        // full PS/2 packet then we would create a backlog in the VM
        // because we wouldn't read the appropriate number of mouse
        // packets from VMWareBackdoor.
        auto mouse_packet = backdoor->receive_mouse_packet();
        if (mouse_packet.has_value()) {
            m_entropy_source.add_random_event(mouse_packet.value());
            {
                ScopedSpinLock lock(m_queue_lock);
                m_queue.enqueue(mouse_packet.value());
            }
            evaluate_block_conditions();
        }
        return;
    }

    auto commit_packet = [&] {
        m_data_state = 0;
        dbgln<PS2MOUSE_DEBUG>("PS2Mouse: {}, {} {} {}",
            m_data.bytes[1],
            m_data.bytes[2],
            (m_data.bytes[0] & 1) ? "Left" : "",
            (m_data.bytes[0] & 2) ? "Right" : "");

        m_entropy_source.add_random_event(m_data.dword);

        {
            ScopedSpinLock lock(m_queue_lock);
            m_queue.enqueue(parse_data_packet(m_data));
        }
        evaluate_block_conditions();
    };

    ASSERT(m_data_state < sizeof(m_data.bytes) / sizeof(m_data.bytes[0]));
    m_data.bytes[m_data_state] = byte;

    switch (m_data_state) {
    case 0:
        if (!(byte & 0x08)) {
            dbgln("PS2Mouse: Stream out of sync.");
            break;
        }
        ++m_data_state;
        break;
    case 1:
        ++m_data_state;
        break;
    case 2:
        if (m_has_wheel) {
            ++m_data_state;
            break;
        }
        commit_packet();
        break;
    case 3:
        ASSERT(m_has_wheel);
        commit_packet();
        break;
    }
}

MousePacket PS2MouseDevice::parse_data_packet(const RawPacket& raw_packet)
{
    int x = raw_packet.bytes[1];
    int y = raw_packet.bytes[2];
    int z = 0;
    if (m_has_wheel) {
        // FIXME: For non-Intellimouse, this is a full byte.
        //        However, for now, m_has_wheel is only set for Intellimouse.
        z = (char)(raw_packet.bytes[3] & 0x0f);

        // -1 in 4 bits
        if (z == 15)
            z = -1;
    }
    bool x_overflow = raw_packet.bytes[0] & 0x40;
    bool y_overflow = raw_packet.bytes[0] & 0x80;
    bool x_sign = raw_packet.bytes[0] & 0x10;
    bool y_sign = raw_packet.bytes[0] & 0x20;
    if (x && x_sign)
        x -= 0x100;
    if (y && y_sign)
        y -= 0x100;
    if (x_overflow || y_overflow) {
        x = 0;
        y = 0;
    }
    MousePacket packet;
    packet.x = x;
    packet.y = y;
    packet.z = z;
    packet.buttons = raw_packet.bytes[0] & 0x07;

    if (m_has_five_buttons) {
        if (raw_packet.bytes[3] & 0x10)
            packet.buttons |= MousePacket::BackButton;
        if (raw_packet.bytes[3] & 0x20)
            packet.buttons |= MousePacket::ForwardButton;
    }

    packet.is_relative = true;
#if PS2MOUSE_DEBUG
    dbgln("PS2 Relative Mouse: Buttons {:x}", packet.buttons);
    dbgln("Mouse: X {}, Y {}, Z {}", packet.x, packet.y, packet.z);
#endif
    return packet;
}

u8 PS2MouseDevice::get_device_id()
{
    if (send_command(PS2MOUSE_GET_DEVICE_ID) != I8042_ACK)
        return 0;
    return read_from_device();
}

u8 PS2MouseDevice::read_from_device()
{
    return m_controller.read_from_device(I8042Controller::Device::Mouse);
}

u8 PS2MouseDevice::send_command(u8 command)
{
    u8 response = m_controller.send_command(I8042Controller::Device::Mouse, command);
    if (response != I8042_ACK)
        dbgln("PS2MouseDevice: Command {} got {} but expected ack: {}", command, response, I8042_ACK);
    return response;
}

u8 PS2MouseDevice::send_command(u8 command, u8 data)
{
    u8 response = m_controller.send_command(I8042Controller::Device::Mouse, command, data);
    if (response != I8042_ACK)
        dbgln("PS2MouseDevice: Command {} got {} but expected ack: {}", command, response, I8042_ACK);
    return response;
}

void PS2MouseDevice::set_sample_rate(u8 rate)
{
    send_command(PS2MOUSE_SET_SAMPLE_RATE, rate);
}

bool PS2MouseDevice::initialize()
{
    if (!m_controller.reset_device(I8042Controller::Device::Mouse)) {
        dbgln("PS2MouseDevice: I8042 controller failed to reset device");
        return false;
    }

    u8 device_id = read_from_device();

    // Set default settings.
    if (send_command(PS2MOUSE_SET_DEFAULTS) != I8042_ACK)
        return false;

    if (send_command(PS2MOUSE_ENABLE_PACKET_STREAMING) != I8042_ACK)
        return false;

    if (device_id != PS2MOUSE_INTELLIMOUSE_ID) {
        // Send magical wheel initiation sequence.
        set_sample_rate(200);
        set_sample_rate(100);
        set_sample_rate(80);
        device_id = get_device_id();
    }
    if (device_id == PS2MOUSE_INTELLIMOUSE_ID) {
        m_has_wheel = true;
        klog() << "PS2MouseDevice: Mouse wheel enabled!";
    } else {
        klog() << "PS2MouseDevice: No mouse wheel detected!";
    }

    if (device_id == PS2MOUSE_INTELLIMOUSE_ID) {
        // Try to enable 5 buttons as well!
        set_sample_rate(200);
        set_sample_rate(200);
        set_sample_rate(80);
        device_id = get_device_id();
    }

    if (device_id == PS2MOUSE_INTELLIMOUSE_EXPLORER_ID) {
        m_has_five_buttons = true;
        klog() << "PS2MouseDevice: 5 buttons enabled!";
    }
    return true;
}

bool PS2MouseDevice::can_read(const FileDescription&, size_t) const
{
    ScopedSpinLock lock(m_queue_lock);
    return !m_queue.is_empty();
}

KResultOr<size_t> PS2MouseDevice::read(FileDescription&, size_t, UserOrKernelBuffer& buffer, size_t size)
{
    ASSERT(size > 0);
    size_t nread = 0;
    size_t remaining_space_in_buffer = static_cast<size_t>(size) - nread;
    ScopedSpinLock lock(m_queue_lock);
    while (!m_queue.is_empty() && remaining_space_in_buffer) {
        auto packet = m_queue.dequeue();
        lock.unlock();

        if constexpr (PS2MOUSE_DEBUG) {
            dbgln("PS2 Mouse Read: Buttons {:x}", packet.buttons);
            dbgln("PS2 Mouse: X {}, Y {}, Z {}, Relative {}", packet.x, packet.y, packet.z, packet.buttons);
            dbgln("PS2 Mouse Read: Filter packets");
        }

        size_t bytes_read_from_packet = min(remaining_space_in_buffer, sizeof(MousePacket));
        if (!buffer.write(&packet, nread, bytes_read_from_packet))
            return EFAULT;
        nread += bytes_read_from_packet;
        remaining_space_in_buffer -= bytes_read_from_packet;

        lock.lock();
    }
    return nread;
}

KResultOr<size_t> PS2MouseDevice::write(FileDescription&, size_t, const UserOrKernelBuffer&, size_t)
{
    return 0;
}

}
