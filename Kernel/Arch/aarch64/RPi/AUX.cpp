/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/RPi/AUX.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>

namespace Kernel::RPi {

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

AUX::AUX()
    : m_registers(MMIO::the().peripheral<AUXRegisters>(0x21'5000).release_value_but_fixme_should_propagate_errors())
{
}

AUX& AUX::the()
{
    static Singleton<AUX> instance;
    return instance;
}

void AUX::set_peripheral_enabled(Peripheral peripheral, bool enabled)
{
    switch (peripheral) {
    case Peripheral::MiniUART:
        m_registers->enables.mini_uart = enabled;
        break;
    case Peripheral::SPI1:
        m_registers->enables.spi1 = enabled;
        break;
    case Peripheral::SPI2:
        m_registers->enables.spi2 = enabled;
        break;
    }
}

}
