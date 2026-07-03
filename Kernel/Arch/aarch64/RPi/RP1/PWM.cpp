/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/RP1/Clocks.h>
#include <Kernel/Arch/aarch64/RPi/RP1/PWM.h>
#include <Kernel/Arch/aarch64/RPi/RP1/RP1.h>

namespace Kernel::RPi {

ErrorOr<NonnullRefPtr<RP1PWM>> RP1PWM::create(RP1& rp1, RP1Clocks& clocks, PhysicalAddress paddr, size_t index)
{
    auto registers = TRY(Memory::map_typed_writable<Registers volatile>(paddr));
    auto pwm = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RP1PWM(rp1, clocks, move(registers))));

    if (index == 1)
        clocks.enable_pwm1_clock();
    else
        TODO();

    return pwm;
}

void RP1PWM::set_up_channel(u8 channel_number, size_t period_ns, InvertOutput invert_output)
{
    VERIFY(channel_number <= 3);

    auto pwm1_frequency_hz = m_rp1_clocks->get_pwm1_clock_frequency_hz();

    m_registers->channel[channel_number].range = (period_ns * pwm1_frequency_hz) / 1'000'000'000;

    auto control = m_registers->channel[channel_number].control;

    control &= ~Registers::Control::ModeMask;
    control |= Registers::Control::ModeGenerate0;

    if (invert_output == InvertOutput::Yes)
        control |= Registers::Control::Invert;
    else
        control &= ~Registers::Control::Invert;

    m_registers->channel[channel_number].control = control;

    m_registers->global_control |= ((1u << channel_number) << Registers::GLOBAL_CONTROL_ENABLE_CHANNEL_OFFSET)
        | Registers::GLOBAL_CONTROL_TRIGGER_SETTINGS_UPDATE;
}

void RP1PWM::disable_channel(u8 channel_number)
{
    VERIFY(channel_number <= 3);

    auto control = m_registers->channel[channel_number].control;

    control &= ~Registers::Control::ModeMask;
    control |= Registers::Control::ModeGenerate0;

    m_registers->channel[channel_number].control = control;

    m_registers->global_control |= Registers::GLOBAL_CONTROL_TRIGGER_SETTINGS_UPDATE;
}

void RP1PWM::enable_channel(u8 channel_number, size_t pulse_active_time_ns)
{
    VERIFY(channel_number <= 3);

    auto pwm1_frequency_hz = m_rp1_clocks->get_pwm1_clock_frequency_hz();

    m_registers->channel[channel_number].duty = (pulse_active_time_ns * pwm1_frequency_hz) / 1'000'000'000;

    auto control = m_registers->channel[channel_number].control;

    control &= ~Registers::Control::ModeMask;
    control |= Registers::Control::ModeTrailingEdgeMarkSpacePWMModulation;

    m_registers->channel[channel_number].control = control;

    m_registers->global_control |= Registers::GLOBAL_CONTROL_TRIGGER_SETTINGS_UPDATE;
}

RP1PWM::RP1PWM(RP1& rp1, RP1Clocks& clocks, Memory::TypedMapping<Registers volatile> registers)
    : m_rp1(rp1)
    , m_rp1_clocks(clocks)
    , m_registers(move(registers))
{
}

}
