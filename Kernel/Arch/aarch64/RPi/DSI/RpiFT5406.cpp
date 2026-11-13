/*
 * Copyright (c) 2026, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FT5406.h"

#include <AK/Array.h>
#include <AK/AtomicRefCounted.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/ScopeGuard.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Arch/aarch64/RPi/FramebufferMailboxMessages.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Devices/Input/Management.h>
#include <Kernel/Devices/Input/MouseDevice.h>
#include <Kernel/Firmware/DeviceTree/Device.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel::RPi {

class RPiFT5406Device : public AtomicRefCounted<RPiFT5406Device> {
public:
    static ErrorOr<NonnullRefPtr<RPiFT5406Device>> try_initialize(DeviceTree::Device const& node)
    {
        auto x_size = node.node().get_u32_property("touchscreen-size-x"sv);
        auto y_size = node.node().get_u32_property("touchscreen-size-y"sv);

        if (!x_size.has_value() || !y_size.has_value()) {
            dmesgln("FT5406: Touch area not sized, rejecting");
            return EIO;
        }

        struct {
            Mailbox::MessageHeader header;
            FramebufferGetTouchBuf touchbuf;
            Mailbox::MessageTail tail;
        } message_queue;
        message_queue.header.set_queue_size(sizeof(message_queue));

        if (!Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
            dmesgln("FT5406: Mailbox send failed.");
            return EIO;
        }

        if (message_queue.touchbuf.address == 0) {
            dmesgln("FT5406: Could not get TouchBuf from VC.");
            return ENODEV;
        }
        // Convert GPU address space to RAM
        // GPU maps memory from 0x80000000 instead of 0x00000000
        PhysicalAddress address { message_queue.touchbuf.address & 0x3FFFFFFF };
        dmesgln("FT5406: Got TouchBuf@GPU{:p} ({})", message_queue.touchbuf.address, address);
        auto mapping = TRY(Memory::map_typed_writable<FT5406TouchBuf volatile>(address));

        // FIXME: Support and make this a propper (Multi)PointerDevice
        auto mouse_device = TRY(MouseDevice::try_to_initialize());

        auto device = TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) RPiFT5406Device(move(mapping), mouse_device, *x_size, *y_size)));

        InputManagement::the().attach_standalone_input_device(*mouse_device);

        auto [process, poll_thread] = TRY(Process::create_kernel_process("FT5406 Poll"sv, [self = device]() { self->thread(); }));
        device->m_process = process;
        device->m_poll_thread = poll_thread;

        return device;
    }

    ~RPiFT5406Device()
    {
        if (m_process) {
            m_process->die();
            // Block until all threads exited to prevent UAF
            ErrorOr<siginfo_t> result = siginfo_t {};
            (void)Thread::current()->block<Thread::WaitBlocker>({}, WEXITED, m_process.release_nonnull(), result);
        }
        // Detach the mouse
        InputManagement::the().detach_standalone_input_device(m_mouse);
    }

private:
    RPiFT5406Device(Memory::TypedMapping<FT5406TouchBuf volatile> touch_buf, NonnullRefPtr<MouseDevice> mouse, int x_size, int y_size)
        : m_touch_buf(move(touch_buf))
        , m_mouse(move(mouse))
        , m_x_size(x_size)
        , m_y_size(y_size)
    {
    }

    void thread()
    {
        MousePacket state {};

        while (true) {
            (void)m_poll_thread->sleep(Duration::from_milliseconds(17)); // 60 Hz Poll

            // Local copy to not have too many data races with the firmware
            FT5406TouchBuf localbuf;
            { // Note: We can't memcpy from volatile, neither can the typesystem copy, so a manual loop it is
                u8* dst = (u8*)&localbuf;
                u8 volatile* src = (u8 volatile*)m_touch_buf.ptr();
                for (auto i = 0uz; i < sizeof(localbuf); ++i) {
                    dst[i] = src[i];
                }
            }

            // Note: This seems to have to be 99,
            //       otherwise we seem to get spurious empty packets.
            m_touch_buf->num_points = 99;  // Mark as read
            if (localbuf.num_points == 99) // Not updated yet
                continue;

            auto num_points = min<u8>(FT5406TouchBuf::max_points, localbuf.num_points);

            if (num_points == 0) {
                if (state.buttons != 0) {
                    // The touch was just released
                    state.buttons = 0;
                    m_mouse->handle_mouse_packet_input_event(MousePacket {});
                };
                continue;
            }

            // FIXME: Check the gesture field, and use that to do zoom/forward/back?

            MousePacket packet;
            for (u8 i = 0; i < num_points; ++i) {
                auto& point = localbuf.touch_points[i];

                // FIXME: Support multi point touch
                if (point.touch_id != 0)
                    continue;

                // Mouse Up event's are detected by an absence of packets,
                // so we need to only check for Contact and PutDown.
                if (point.event != FT5406TouchBuf::EventFlag::Contact && point.event != FT5406TouchBuf::EventFlag::PutDown)
                    continue;

                // FIXME: Long Press for right click,
                //        or should that be done in Userland?

                int x = (point.xh << 8 | point.xl);
                int y = (point.yh << 8 | point.yl);
                // Note: Absolute packets are scaled to the screen size,
                //       with 16 bits of subpixel precision we just throw away later.
                //       See: Userland/Services/WindowServer/Screen.cpp#ScreenInput::on_receive_mouse_data
                x = x * 0xFFFF / m_x_size;
                y = y * 0xFFFF / m_y_size;

                packet = MousePacket {
                    .x = x,
                    .y = y,
                    .z = 0,
                    .w = 0,
                    .buttons = MousePacket::Button::LeftButton,
                    .is_relative = false,
                };
            }

            if (packet.x == state.x && packet.y == state.y)
                continue;

            // Note: If we did not find a packet of ID 0 the packet will be empty,
            //       yielding the same result as the lift detection further up.
            state = packet;
            m_mouse->handle_mouse_packet_input_event(state);
        }
    }

    Memory::TypedMapping<FT5406TouchBuf volatile> m_touch_buf;
    NonnullRefPtr<MouseDevice> m_mouse;
    RefPtr<Process> m_process;
    RefPtr<Thread> m_poll_thread;
    int m_x_size;
    int m_y_size;
};

DEVICETREE_DRIVER(RPiFT5406Driver, to_array({ "raspberrypi,firmware-ts"sv }));

ErrorOr<void> RPiFT5406Driver::probe(Kernel::DeviceTree::Device const& device, [[maybe_unused]] StringView compatible_entry) const
{
    (void)TRY(RPiFT5406Device::try_initialize(device));
    return {};
}

}
