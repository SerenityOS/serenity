/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Types.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

class RP1;

class RP1Clocks : public AtomicRefCounted<RP1Clocks> {
public:
    static ErrorOr<NonnullRefPtr<RP1Clocks>> create(RP1&, PhysicalAddress);

    enum class Clock {
        PWM1 = 8,
    };

    void enable_pwm1_clock();
    size_t get_pwm1_clock_frequency_hz();

    struct Registers;

private:
    RP1Clocks(RP1&, Memory::TypedMapping<Registers volatile>);

    NonnullRefPtr<RP1> m_rp1;
    Memory::TypedMapping<Registers volatile> m_registers;
};

struct RP1Clocks::Registers {
    static constexpr size_t CONTROL_ENABLE = 1u << 11;
    static constexpr size_t CONTROL_AUXILIARY_CLOCK_SOURCE_MASK = 0xf << 5;
    static constexpr size_t CONTROL_AUXILIARY_CLOCK_SOURCE_OFFSET = 5;

    u32 _;

    struct {
        u32 control;                      // RW, some fields RO
        u32 divisor_integer;              // RW
        u32 divisor_fractional_component; // RW
        u32 selected_source;              // RO
    } clock[29];
};

static_assert(offsetof(RP1Clocks::Registers, clock[to_underlying(RP1Clocks::Clock::PWM1)].control) == 0x84);

}
