/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/aarch64/BootPPMParser.h>
#include <Kernel/Arch/aarch64/RPi/Framebuffer.h>
#include <Kernel/Arch/aarch64/RPi/FramebufferMailboxMessages.h>
#include <Kernel/Sections.h>

extern READONLY_AFTER_INIT PhysicalAddress multiboot_framebuffer_addr;
extern READONLY_AFTER_INIT u32 multiboot_framebuffer_pitch;
extern READONLY_AFTER_INIT u32 multiboot_framebuffer_width;
extern READONLY_AFTER_INIT u32 multiboot_framebuffer_height;
extern READONLY_AFTER_INIT u8 multiboot_framebuffer_type;
extern const u32 serenity_boot_logo_start;
extern const u32 serenity_boot_logo_size;

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
        FramebufferGetPithMboxMessage get_pitch;
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
    message_queue.set_pixel_order.pixel_order = FramebufferSetPixelOrderMboxMessage::PixelOrder::RGB;
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

        VERIFY(framebuffer.pixel_order() == PixelOrder::RGB);
        multiboot_framebuffer_type = MULTIBOOT_FRAMEBUFFER_TYPE_RGB;
    }
}

void Framebuffer::draw_logo(u8* framebuffer_data)
{
    BootPPMParser logo_parser(reinterpret_cast<u8 const*>(&serenity_boot_logo_start), serenity_boot_logo_size);
    if (!logo_parser.parse()) {
        dbgln("Failed to parse boot logo.");
        return;
    }

    dbgln("Boot logo size: {} ({} x {})", serenity_boot_logo_size, logo_parser.image.width, logo_parser.image.height);

    auto fb_ptr = framebuffer_data;
    auto image_left = (width() - logo_parser.image.width) / 2;
    auto image_right = image_left + logo_parser.image.width;
    auto image_top = (height() - logo_parser.image.height) / 2;
    auto image_bottom = image_top + logo_parser.image.height;
    auto logo_pixels = logo_parser.image.pixel_data;

    for (u32 y = 0; y < height(); y++) {
        for (u32 x = 0; x < width(); x++) {
            if (x >= image_left && x < image_right && y >= image_top && y < image_bottom) {
                switch (pixel_order()) {
                case RPi::Framebuffer::PixelOrder::RGB:
                    fb_ptr[0] = logo_pixels[0];
                    fb_ptr[1] = logo_pixels[1];
                    fb_ptr[2] = logo_pixels[2];
                    break;
                case RPi::Framebuffer::PixelOrder::BGR:
                    fb_ptr[0] = logo_pixels[2];
                    fb_ptr[1] = logo_pixels[1];
                    fb_ptr[2] = logo_pixels[0];
                    break;
                default:
                    dbgln("Unsupported pixel format");
                    VERIFY_NOT_REACHED();
                }

                logo_pixels += 3;
            } else {
                fb_ptr[0] = 0xBD;
                fb_ptr[1] = 0xBD;
                fb_ptr[2] = 0xBD;
            }

            fb_ptr[3] = 0xFF;
            fb_ptr += 4;
        }
        fb_ptr += pitch() - width() * 4;
    }
}

}
