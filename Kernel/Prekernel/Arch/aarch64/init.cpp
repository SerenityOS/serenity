/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Arch/aarch64/Aarch64Registers.h>
#include <Kernel/Prekernel/Arch/aarch64/Aarch64_asm_utils.h>
#include <Kernel/Prekernel/Arch/aarch64/BootPPMParser.h>
#include <Kernel/Prekernel/Arch/aarch64/Framebuffer.h>
#include <Kernel/Prekernel/Arch/aarch64/Mailbox.h>
#include <Kernel/Prekernel/Arch/aarch64/Timer.h>
#include <Kernel/Prekernel/Arch/aarch64/UART.h>
#include <Kernel/Prekernel/Arch/aarch64/Utils.h>

extern "C" [[noreturn]] void halt();
extern "C" [[noreturn]] void init();
extern "C" [[noreturn]] void os_start();

static void set_up_el1_mode();
static void set_up_el2_mode();
static void set_up_el3_mode();
static void print_current_exception_level(const char* msg);
static void draw_logo();
static u32 query_firmware_version();
[[noreturn]] static void jump_to_os_start_from_el2();
[[noreturn]] static void jump_to_os_start_from_el3();

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

    print_current_exception_level("CPU started in:");

    set_up_el2_mode();
    set_up_el1_mode();

    auto current_exception_level = get_current_exception_level();
    switch (current_exception_level) {
    case 2:
        jump_to_os_start_from_el2();
        break;
    case 3:
        set_up_el3_mode();
        jump_to_os_start_from_el3();
        break;
    default:
        uart.print_str("FATAL: CPU booted in unsupported exception mode!\r\n");
        halt();
    }
}

extern "C" [[noreturn]] void os_start()
{
    auto& uart = Prekernel::UART::the();

    print_current_exception_level("CPU switched to:");

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

static void set_up_el1_mode()
{
    Kernel::Aarch64_SCTLR_EL1 system_control_register_el1 = {};

    // Those bits are reserved on ARMv8.0
    system_control_register_el1.LSMAOE = 1;
    system_control_register_el1.nTLSMD = 1;
    system_control_register_el1.SPAN = 1;
    system_control_register_el1.IESB = 1;

    // Don't trap access to CTR_EL0
    system_control_register_el1.UCT = 1;

    // Don't trap WFE instructions
    system_control_register_el1.nTWE = 1;

    // Don't trap WFI instructions
    system_control_register_el1.nTWI = 1;

    // Don't trap DC ZVA instructions
    system_control_register_el1.DZE = 1;

    // Don't trap access to DAIF (debugging) flags of EFLAGS register
    system_control_register_el1.UMA = 1;

    // Enable stack access alignment check for EL0
    system_control_register_el1.SA0 = 1;

    // Enable stack access alignment check for EL1
    system_control_register_el1.SA = 1;

    // Enable memory access alignment check
    system_control_register_el1.A = 1;

    Kernel::Aarch64_SCTLR_EL1::write(system_control_register_el1);
}

static void set_up_el2_mode()
{
    Kernel::Aarch64_HCR_EL2 hypervisor_configuration_register_el2 = {};

    // EL1 to use 64-bit mode
    hypervisor_configuration_register_el2.RW = 1;

    Kernel::Aarch64_HCR_EL2::write(hypervisor_configuration_register_el2);
}

static void set_up_el3_mode()
{
    Kernel::Aarch64_SCR_EL3 secure_configuration_register_el3 = {};

    // Don't trap access to Counter-timer Physical Secure registers
    secure_configuration_register_el3.ST = 1;

    // Lower level to use Aarch64
    secure_configuration_register_el3.RW = 1;

    // Enable Hypervisor instructions at all levels
    secure_configuration_register_el3.HCE = 1;

    Kernel::Aarch64_SCR_EL3::write(secure_configuration_register_el3);
}

[[noreturn]] static void jump_to_os_start_from_el2()
{
    // Processor state to set when returned from this function (in new EL1 world)
    Kernel::Aarch64_SPSR_EL2 saved_program_status_register_el2 = {};

    // Mask (disable) all interrupts
    saved_program_status_register_el2.A = 1;
    saved_program_status_register_el2.I = 1;
    saved_program_status_register_el2.F = 1;

    // Indicate EL1 as exception origin mode (so we go back there)
    saved_program_status_register_el2.M = Kernel::Aarch64_SPSR_EL2::Mode::EL1h;

    Kernel::Aarch64_SPSR_EL2::write(saved_program_status_register_el2);

    // This will jump into os_start()
    return_from_el2();
}

[[noreturn]] static void jump_to_os_start_from_el3()
{
    // Processor state to set when returned from this function (in new EL1 world)
    Kernel::Aarch64_SPSR_EL3 saved_program_status_register_el3 = {};

    // Mask (disable) all interrupts
    saved_program_status_register_el3.A = 1;
    saved_program_status_register_el3.I = 1;
    saved_program_status_register_el3.F = 1;

    // Indicate EL1 as exception origin mode (so we go back there)
    saved_program_status_register_el3.M = Kernel::Aarch64_SPSR_EL3::Mode::EL1h;

    Kernel::Aarch64_SPSR_EL3::write(saved_program_status_register_el3);

    // This will jump into os_start() below
    return_from_el3();
}

static void print_current_exception_level(const char* msg)
{
    auto& uart = Prekernel::UART::the();

    auto exception_level = get_current_exception_level();
    uart.print_str(msg);
    uart.print_str(" EL");
    uart.print_num(exception_level);
    uart.print_str("\r\n");
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
