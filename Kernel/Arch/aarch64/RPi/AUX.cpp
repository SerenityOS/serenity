/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/aarch64/RPi/AUX.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>

namespace Kernel::RPi {

// bcm2711-peripherals.pdf "2. Auxiliaries: UART1, SPI1 & SPI2"
struct AUXRegisters {
    u32 IRQ;
    struct {
        u32 mini_uart_enable : 1;
        u32 spi1_enable : 1;
        u32 spi2_enable : 1;
        u32 : 29;
    } ENABLES;
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
        m_registers->ENABLES.mini_uart_enable = enabled;
        break;
    case Peripheral::SPI1:
        m_registers->ENABLES.spi1_enable = enabled;
        break;
    case Peripheral::SPI2:
        m_registers->ENABLES.spi2_enable = enabled;
        break;
    }
}

}
