/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/PlatformInit.h>

#include <Kernel/Arch/aarch64/DebugOutput.h>
#include <Kernel/Arch/aarch64/RPi/Framebuffer.h>
#include <Kernel/Arch/aarch64/RPi/GPIO.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Arch/aarch64/RPi/Timer.h>
#include <Kernel/Arch/aarch64/Serial/PL011.h>

namespace Kernel {

void raspberry_pi_3_4_platform_init(StringView compatible_string)
{
    // We have to use a raw pointer here because this variable will be set before global constructors are called.
    static PL011* s_debug_console_uart;

    static DebugConsole const s_debug_console {
        .write_character = [](char character) {
            s_debug_console_uart->send(character);
        },
    };

    PhysicalAddress peripheral_base_address;
    if (compatible_string == "raspberrypi,3-model-b"sv)
        peripheral_base_address.set(0x3f00'0000);
    else if (compatible_string == "raspberrypi,4-model-b"sv)
        peripheral_base_address.set(0xfe00'0000);
    else
        VERIFY_NOT_REACHED();

    MUST(RPi::Mailbox::initialize(peripheral_base_address.offset(0xb880)));
    RPi::GPIO::initialize();
    s_debug_console_uart = MUST(PL011::initialize(peripheral_base_address.offset(0x20'1000))).leak_ptr();

    constexpr int baud_rate = 115'200;

    // Set UART clock so that the baud rate divisor ends up as 1.0.
    // FIXME: Not sure if this is a good UART clock rate.
    u32 rate_in_hz = RPi::Timer::set_clock_rate(RPi::Timer::ClockID::UART, 16 * baud_rate);

    // The BCM's PL011 UART is alternate function 0 on pins 14 and 15.
    auto& gpio = RPi::GPIO::the();
    gpio.set_pin_function(14, RPi::GPIO::PinFunction::Alternate0);
    gpio.set_pin_function(15, RPi::GPIO::PinFunction::Alternate0);
    gpio.set_pin_pull_up_down_state(Array { 14, 15 }, RPi::GPIO::PullUpDownState::Disable);

    // Clock and pins are configured. Turn UART on.
    s_debug_console_uart->set_baud_rate(baud_rate, rate_in_hz);

    set_debug_console(&s_debug_console);

    auto firmware_version = RPi::Mailbox::the().query_firmware_version();
    dmesgln("RPi: Firmware version: {}", firmware_version);

    RPi::Framebuffer::initialize();

    // The BCM's SDHC is alternate function 3 on pins 21-27.
    gpio.set_pin_function(21, RPi::GPIO::PinFunction::Alternate3); // CD
    gpio.set_pin_high_detect_enable(21, true);

    gpio.set_pin_function(22, RPi::GPIO::PinFunction::Alternate3); // SD1_CLK
    gpio.set_pin_function(23, RPi::GPIO::PinFunction::Alternate3); // SD1_CMD

    gpio.set_pin_function(24, RPi::GPIO::PinFunction::Alternate3); // SD1_DAT0
    gpio.set_pin_function(25, RPi::GPIO::PinFunction::Alternate3); // SD1_DAT1
    gpio.set_pin_function(26, RPi::GPIO::PinFunction::Alternate3); // SD1_DAT2
    gpio.set_pin_function(27, RPi::GPIO::PinFunction::Alternate3); // SD1_DAT3
}

void raspberry_pi_5_platform_init(StringView)
{
    // We have to use a raw pointer here because this variable will be set before global constructors are called.
    static PL011* s_debug_console_uart;

    static DebugConsole const s_debug_console {
        .write_character = [](char character) {
            s_debug_console_uart->send(character);
        },
    };

    // Use the dedicated debug UART (UART10) that can be connected with the Raspberry Pi Debug Probe (https://www.raspberrypi.com/documentation/microcontrollers/debug-probe.html).
    // The GPIO UARTs are not yet accessible since they reside in the RP1, which is connected via PCIe.
    // However, we could set "pciex4_reset=0" in config.txt to keep the PCIe root complex initialized.
    s_debug_console_uart = MUST(PL011::initialize(PhysicalAddress { 0x10'7d00'1000 })).leak_ptr();
    set_debug_console(&s_debug_console);

    // FIXME: Don't rely on the firmware configuring the baud rate.

    MUST(RPi::Mailbox::initialize(PhysicalAddress { 0x10'7c01'3880 }));
    RPi::Framebuffer::initialize();
}

}
