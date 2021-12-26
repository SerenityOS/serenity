/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Prekernel/Arch/aarch64/Aarch64_asm_utils.h>
#include <Kernel/Prekernel/Arch/aarch64/BootPPMParser.h>
#include <Kernel/Prekernel/Arch/aarch64/Framebuffer.h>
#include <Kernel/Prekernel/Arch/aarch64/Mailbox.h>
#include <Kernel/Prekernel/Arch/aarch64/Prekernel.h>
#include <Kernel/Prekernel/Arch/aarch64/Timer.h>
#include <Kernel/Prekernel/Arch/aarch64/UART.h>
#include <Kernel/Prekernel/Arch/aarch64/Utils.h>

static void draw_logo();
static u32 query_firmware_version();

extern "C" [[noreturn]] void halt();
extern "C" [[noreturn]] void init();

extern "C" [[noreturn]] void init()
{
    auto& uart = Prekernel::UART::the();

    uart.print_str("\r\nWelcome to Serenity OS!\r\n");
    uart.print_str("Imagine this being your ideal operating system.\r\n");
    uart.print_str("Observed deviations from that ideal are shortcomings of your imagination.\r\n\r\n");

    auto firmware_version = query_firmware_version();
    uart.print_str("Firmware version: ");
    uart.print_num(firmware_version);
    uart.print_str("\r\n");

    uart.print_str("CPU started in: EL");
    uart.print_num(static_cast<u64>(get_current_exception_level()));
    uart.print_str("\r\n");

    uart.print_str("Drop CPU to EL1\r\n");
    Prekernel::drop_to_exception_level_1();

    auto& framebuffer = Prekernel::Framebuffer::the();
    if (framebuffer.initialized()) {
        draw_logo();
    }

    auto& timer = Prekernel::Timer::the();
    u64 start_musec = 0;
    for (;;) {
        u64 now_musec;
        while ((now_musec = timer.microseconds_since_boot()) - start_musec < 1'000'000)
            ;
        start_musec = now_musec;
        uart.print_str("Timer: ");
        uart.print_num(now_musec);
        uart.print_str("\r\n");
    }
}

// FIXME: Share this with the Intel Prekernel.
extern size_t __stack_chk_guard;
size_t __stack_chk_guard;
extern "C" [[noreturn]] void __stack_chk_fail();

[[noreturn]] void halt()
{
    for (;;) {
        asm volatile("wfi");
    }
}

void __stack_chk_fail()
{
    halt();
}

[[noreturn]] void __assertion_failed(char const*, char const*, unsigned int, char const*)
{
    halt();
}

class QueryFirmwareVersionMboxMessage : Prekernel::Mailbox::Message {
public:
    u32 version;

    QueryFirmwareVersionMboxMessage()
        : Prekernel::Mailbox::Message(0x0000'0001, 4)
    {
        version = 0;
    }
};

static u32 query_firmware_version()
{
    struct __attribute__((aligned(16))) {
        Prekernel::Mailbox::MessageHeader header;
        QueryFirmwareVersionMboxMessage query_firmware_version;
        Prekernel::Mailbox::MessageTail tail;
    } message_queue;

    if (!Prekernel::Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
        return 0xffff'ffff;
    }

    return message_queue.query_firmware_version.version;
}

extern "C" const u32 serenity_boot_logo_start;
extern "C" const u32 serenity_boot_logo_size;

static void draw_logo()
{
    Prekernel::BootPPMParser logo_parser(reinterpret_cast<const u8*>(&serenity_boot_logo_start), serenity_boot_logo_size);
    if (!logo_parser.parse()) {
        Prekernel::warnln("Invalid boot logo.");
        return;
    }

    auto& uart = Prekernel::UART::the();
    uart.print_str("Boot logo size: ");
    uart.print_num(serenity_boot_logo_size);
    uart.print_str("\r\n");
    uart.print_str("Width: ");
    uart.print_num(logo_parser.image.width);
    uart.print_str("\r\n");
    uart.print_str("Height: ");
    uart.print_num(logo_parser.image.height);
    uart.print_str("\r\n");

    auto& framebuffer = Prekernel::Framebuffer::the();
    auto fb_ptr = framebuffer.gpu_buffer();
    auto image_left = (framebuffer.width() - logo_parser.image.width) / 2;
    auto image_right = image_left + logo_parser.image.width;
    auto image_top = (framebuffer.height() - logo_parser.image.height) / 2;
    auto image_bottom = image_top + logo_parser.image.height;
    auto logo_pixels = logo_parser.image.pixel_data;

    for (u32 y = 0; y < framebuffer.height(); y++) {
        for (u32 x = 0; x < framebuffer.width(); x++) {
            if (x >= image_left && x < image_right && y >= image_top && y < image_bottom) {
                switch (framebuffer.pixel_order()) {
                case Prekernel::Framebuffer::PixelOrder::RGB:
                    fb_ptr[0] = logo_pixels[0];
                    fb_ptr[1] = logo_pixels[1];
                    fb_ptr[2] = logo_pixels[2];
                    break;
                case Prekernel::Framebuffer::PixelOrder::BGR:
                    fb_ptr[0] = logo_pixels[2];
                    fb_ptr[1] = logo_pixels[1];
                    fb_ptr[2] = logo_pixels[0];
                    break;
                default:
                    Prekernel::warnln("Unsupported pixel format");
                    halt();
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
        fb_ptr += framebuffer.pitch() - framebuffer.width() * 4;
    }
}
