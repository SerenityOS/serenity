/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Prekernel/Arch/aarch64/GPIO.h>
#include <Kernel/Prekernel/Arch/aarch64/Mailbox.h>

extern "C" [[noreturn]] void halt();

extern "C" [[noreturn]] void init();
extern "C" [[noreturn]] void init()
{
    auto& gpio = Prekernel::GPIO::the();
    gpio.set_pin_function(14, Prekernel::GPIO::PinFunction::Alternate0);
    gpio.set_pin_function(15, Prekernel::GPIO::PinFunction::Alternate0);

    gpio.set_pin_pull_up_down_state(Array { 14, 15 }, Prekernel::GPIO::PullUpDownState::Disable);

    [[maybe_unused]] u32 firmware_version = Prekernel::Mailbox::query_firmware_version();
    halt();
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
