/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/aarch64/BootPPMParser.h>
#include <Kernel/Arch/aarch64/RPi/Framebuffer.h>
#include <Kernel/Arch/aarch64/RPi/FramebufferMailboxMessages.h>
#include <Kernel/Boot/BootInfo.h>
#include <Kernel/Sections.h>

namespace Kernel::RPi {

Framebuffer::Framebuffer()
{
    // FIXME: query HDMI for best mode
    // https://github.com/raspberrypi/userland/blob/master/host_applications/linux/apps/tvservice/tvservice.c
    m_width = 1280;
    m_height = 720;
    m_depth = 32;
    m_initialized = false;

    struct __attribute__((aligned(16))) {
        Mailbox::MessageHeader header;
        FramebufferSetPhysicalSizeMboxMessage set_physical_size;
        FramebufferSetVirtualSizeMboxMessage set_virtual_size;
        FramebufferSetVirtualOffsetMboxMessage set_virtual_offset;
        FramebufferSetDepthMboxMessage set_depth;
        FramebufferSetPixelOrderMboxMessage set_pixel_order;
        FramebufferAllocateBufferMboxMessage allocate_buffer;
        FramebufferGetPitchMboxMessage get_pitch;
        Mailbox::MessageTail tail;
    } message_queue;

    message_queue.header.set_queue_size(sizeof(message_queue));
    message_queue.set_physical_size.width = m_width;
    message_queue.set_physical_size.height = m_height;
    message_queue.set_virtual_size.width = m_width;
    message_queue.set_virtual_size.height = m_height;

    // FIXME! those next 2 lines crash...
    // message_queue.set_virtual_offset.x = 0;
    // message_queue.set_virtual_offset.y = 0;

    message_queue.set_depth.depth_bits = 32;
    message_queue.set_pixel_order.pixel_order = FramebufferSetPixelOrderMboxMessage::PixelOrder::BGR;
    message_queue.allocate_buffer.alignment = 4096;

    if (!Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
        dbgln("Framebuffer(): Mailbox send failed.");
        return;
    }

    // Now message queue contains responses. Process them.

    if (message_queue.set_physical_size.width != m_width || message_queue.set_physical_size.height != m_height) {
        dbgln("Framebuffer(): Setting physical dimension failed.");
        return;
    }

    if (message_queue.set_virtual_size.width != m_width || message_queue.set_virtual_size.height != m_height) {
        dbgln("Framebuffer(): Setting virtual dimension failed.");
        return;
    }

    if (message_queue.set_virtual_offset.x != 0 || message_queue.set_virtual_offset.y != 0) {
        dbgln("Framebuffer(): Setting virtual offset failed.");
        return;
    }

    if (message_queue.set_depth.depth_bits != m_depth) {
        dbgln("Framebuffer(): Setting depth failed.");
        return;
    }

    if (message_queue.allocate_buffer.size == 0 || message_queue.allocate_buffer.address == 0) {
        dbgln("Framebuffer(): Allocating buffer failed.");
        return;
    }

    if (message_queue.get_pitch.pitch == 0) {
        dbgln("Framebuffer(): Retrieving pitch failed.");
        return;
    }

    // Convert GPU address space to RAM
    // GPU maps memory from 0x80000000 instead of 0x00000000
    m_gpu_buffer = reinterpret_cast<u8*>(message_queue.allocate_buffer.address & 0x3FFFFFFF);

    m_buffer_size = message_queue.allocate_buffer.size;
    m_pitch = message_queue.get_pitch.pitch;

    switch (message_queue.set_pixel_order.pixel_order) {
    case FramebufferSetPixelOrderMboxMessage::PixelOrder::RGB:
        m_pixel_order = PixelOrder::RGB;
        break;
    case FramebufferSetPixelOrderMboxMessage::PixelOrder::BGR:
        m_pixel_order = PixelOrder::BGR;
        break;
    default:
        dbgln("Framebuffer(): Unsupported pixel order reported by GPU.");
        m_pixel_order = PixelOrder::RGB;
        break;
    }

    dbgln("Initialized framebuffer: {} x {} @ {} bits", m_width, m_height, m_depth);
    m_initialized = true;
}

Framebuffer& Framebuffer::the()
{
    static Framebuffer instance;
    return instance;
}

void Framebuffer::initialize()
{
    auto& framebuffer = the();
    if (framebuffer.initialized()) {
        multiboot_framebuffer_addr = PhysicalAddress((PhysicalPtr)framebuffer.gpu_buffer());
        multiboot_framebuffer_width = framebuffer.width();
        multiboot_framebuffer_height = framebuffer.height();
        multiboot_framebuffer_pitch = framebuffer.pitch();

        // NOTE: The required pixel format for MULTIBOOT_FRAMEBUFFER_TYPE_RGB is actually BGRx8888.
        VERIFY(framebuffer.pixel_order() == PixelOrder::BGR);
        multiboot_framebuffer_type = MULTIBOOT_FRAMEBUFFER_TYPE_RGB;

        multiboot_flags |= MULTIBOOT_INFO_FRAMEBUFFER_INFO;
    }
}

}
