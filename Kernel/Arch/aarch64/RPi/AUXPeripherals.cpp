/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/RPi/AUXPeripherals.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>

namespace Kernel::RPi::AUX {

// bcm2711-peripherals.pdf "2.1.1. AUX registers"
struct AUXRegisters {
    struct {
        u32 mini_uart : 1;
        u32 spi1 : 1;
        u32 spi2 : 1;
        u32 : 29;
    } interrupt_pending;
    struct {
        u32 mini_uart : 1;
        u32 spi1 : 1;
        u32 spi2 : 1;
        u32 : 29;
    } enables;
};
static_assert(AssertSize<AUXRegisters, 8>());

struct AUX {
    AUX();

    Memory::TypedMapping<AUXRegisters volatile> registers;
};

AUX::AUX()
    : registers(MMIO::the().peripheral<AUXRegisters>(0x21'5000).release_value_but_fixme_should_propagate_errors())
{
}

static Singleton<AUX> s_the;

void set_peripheral_enabled(Peripheral peripheral, bool enabled)
{
    switch (peripheral) {
    case Peripheral::MiniUART:
        s_the->registers->enables.mini_uart = enabled;
        break;
    case Peripheral::SPI1:
        s_the->registers->enables.spi1 = enabled;
        break;
    case Peripheral::SPI2:
        s_the->registers->enables.spi2 = enabled;
        break;
    }
}

}
