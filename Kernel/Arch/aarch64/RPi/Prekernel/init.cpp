/*
 * Copyright (c) 2023, Timon Kruiper <timonkruiper@gmail.com>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/CPU.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Arch/aarch64/RPi/UART.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>

extern uintptr_t __stack_chk_guard;
uintptr_t __stack_chk_guard __attribute__((used));
extern "C" [[noreturn]] void __stack_chk_fail();

FlatPtr kernel_mapping_base __attribute__((used));

extern "C" [[noreturn]] NEVER_INLINE void halt();
extern "C" [[noreturn]] NEVER_INLINE void halt()
{
    asm volatile("hlt:\n"
                 "msr daifset, #2\n"
                 "wfi\n"
                 "b hlt");
    __builtin_unreachable();
}

extern "C" void __stack_chk_fail()
{
    halt();
}

void __assertion_failed(char const*, char const*, unsigned int, char const*)
{
    halt();
}

// Mailbox and MMIO require these two functions to exist.
// There's nowhere to log, so we just return.
void AK::v_critical_dmesgln(AK::StringView, AK::TypeErasedFormatParams&)
{
}

void AK::vdbg(AK::StringView, AK::TypeErasedFormatParams&, bool)
{
}

// Define some needed Kernel symbols.
namespace Kernel {

[[noreturn]] void __panic(char const*, unsigned int, char const*)
{
    halt();
}

}

namespace Prekernel {

// This prints hex numbers with forced zero padding.
template<Integral T, size_t nibbles = sizeof(T) * 2>
void format_hex(char buffer[nibbles], T value)
{
    constexpr Array<char, 16> chars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    for (auto nibble_index = 0u; nibble_index < nibbles; ++nibble_index) {
        auto const nibble = static_cast<u8>((value >> ((nibbles - nibble_index - 1) * 4)) & 0xf);
        auto const symbol = chars[nibble];
        buffer[nibble_index] = symbol;
    }
}

void uart_print(StringView message)
{
    Kernel::RPi::UART::the().print_str(message.characters_without_null_termination(), message.length());
}

void print_hexdump(u8 const* start_address, size_t amount = 0x40)
{
    auto& uart = Kernel::RPi::UART::the();

    char number_buffer[sizeof(u8) * 2];
    for (auto i = 0u; i < amount; ++i) {
        if (i % 16 == 0)
            uart_print("\n"sv);
        auto const data = start_address[i];
        format_hex(number_buffer, data);
        uart.print_str(number_buffer, array_size(number_buffer));
        uart_print(" "sv);
    }
}

void load_kernel_via_uart();

// We arrive here in an unknown exception level, with all the code and data relocated to safe addresses below 0x80000.
// All cores except the first are sleeping.
// At this point, the Prekernel only needs to load the Kernel image from wherever it is instructed to load it.
// FIXME: We currently only support loading the Kernel via serial, since this is the primary use case of the Prekernel.
//        Also support loading the Kernel from an SD card image, possibly even an ELF.
extern "C" [[noreturn]] void init();
extern "C" [[noreturn]] void init()
{
    // Since we don't use the MMU, this effectively allows the I/O registers to operate at their physical addresses.
    kernel_mapping_base = 0;
    // FIXME: probably not needed?
    __stack_chk_guard = 0;

    load_kernel_via_uart();

    halt();
}

void load_kernel_via_uart()
{
    auto& uart = Kernel::RPi::UART::the();

    // Signal readiness. ("SerenityOS Prekernel")
    uart_print("SPK\x03"sv);

    // Recieve Kernel image size (4 bytes little endian).
    u8 byte_0 = uart.receive();
    u8 byte_1 = uart.receive();
    u8 byte_2 = uart.receive();
    u8 byte_3 = uart.receive();

    u32 const image_size = byte_0 | (byte_1 << 8) | (byte_2 << 16) | (byte_3 << 24);

    auto* current_position = bit_cast<u8*>(0x80000L);
    auto remaining_size = image_size;

    uart_print("OK"sv);

    // Read in image.
    while (remaining_size > 0) {
        u8 data = uart.receive();
        *current_position = data;
        ++current_position;
        --remaining_size;
    }

    // Directly jump to normal RPi entry as if the Kernel was loaded directly.
    asm volatile("mov lr, #0x80000\n"
                 "ret");
}

}
