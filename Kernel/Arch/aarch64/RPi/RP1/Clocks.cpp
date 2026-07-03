/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/RP1/Clocks.h>
#include <Kernel/Arch/aarch64/RPi/RP1/RP1.h>

namespace Kernel::RPi {

ErrorOr<NonnullRefPtr<RP1Clocks>> RP1Clocks::create(RP1& rp1, PhysicalAddress paddr)
{
    auto registers = TRY(Memory::map_typed_writable<Registers volatile>(paddr));

    return adopt_nonnull_ref_or_enomem(new (nothrow) RP1Clocks(rp1, move(registers)));
}

void RP1Clocks::enable_pwm1_clock()
{
    auto volatile& pwm1_clock_regs = m_registers->clock[to_underlying(Clock::PWM1)];

    auto control = pwm1_clock_regs.control;

    control &= ~Registers::CONTROL_AUXILIARY_CLOCK_SOURCE_MASK;
    control |= 2u << Registers::CONTROL_AUXILIARY_CLOCK_SOURCE_OFFSET; // PWM1's AUXSRC 2 is the 50 MHz crystal oscillator.

    control |= Registers::CONTROL_ENABLE;

    pwm1_clock_regs.control = control;
    pwm1_clock_regs.divisor_integer = 1;
    pwm1_clock_regs.divisor_fractional_component = 0;
}

size_t RP1Clocks::get_pwm1_clock_frequency_hz()
{
    auto volatile& pwm1_clock_regs = m_registers->clock[to_underlying(Clock::PWM1)];

    auto control = pwm1_clock_regs.control;
    auto divisor_integer = pwm1_clock_regs.divisor_integer;
    auto divisor_fractional_component = pwm1_clock_regs.divisor_fractional_component;

    auto auxiliary_clock_source = (control & Registers::CONTROL_AUXILIARY_CLOCK_SOURCE_MASK) >> Registers::CONTROL_AUXILIARY_CLOCK_SOURCE_OFFSET;

    VERIFY(auxiliary_clock_source == 2);
    VERIFY(divisor_integer == 1);
    VERIFY(divisor_fractional_component == 0);

    return 50'000'000u;
}

RP1Clocks::RP1Clocks(RP1& rp1, Memory::TypedMapping<Registers volatile> registers)
    : m_rp1(rp1)
    , m_registers(move(registers))
{
}

}
