/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Forward.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtraDetails.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

class RP1;
class RP1Clocks;

class RP1PWM : public AtomicRefCounted<RP1PWM> {
public:
    static ErrorOr<NonnullRefPtr<RP1PWM>> create(RP1&, RP1Clocks&, PhysicalAddress, size_t index);

    enum class InvertOutput {
        No,
        Yes
    };

    void set_up_channel(u8 channel_number, size_t period_ns, InvertOutput);
    void disable_channel(u8 channel_number);
    void enable_channel(u8 channel_number, size_t pulse_active_time_ns);

    struct Registers;

private:
    RP1PWM(RP1&, RP1Clocks&, Memory::TypedMapping<Registers volatile>);

    NonnullRefPtr<RP1> m_rp1;
    NonnullRefPtr<RP1Clocks> m_rp1_clocks;
    Memory::TypedMapping<Registers volatile> m_registers;
};

struct RP1PWM::Registers {
    static constexpr size_t GLOBAL_CONTROL_TRIGGER_SETTINGS_UPDATE = 1u << 31;
    static constexpr size_t GLOBAL_CONTROL_ENABLE_CHANNEL_OFFSET = 0;

    enum Control : u32 {
        ModeGenerate0 = 0b000u << 0,
        ModeTrailingEdgeMarkSpacePWMModulation = 0b001u << 0,
        ModeMask = 0b111u << 0,

        Invert = 1u << 3,
    };

    u32 global_control;
    u32 fifo_control;
    u32 common_range;
    u32 common_duty;
    u32 duty_fifo;

    struct {
        Control control;
        u32 range;
        u32 phase;
        u32 duty;
    } channel[4];

    u32 raw_interrupts;
    u32 interrupt_enable;
    u32 interrupt_force;
    u32 interrupt_status;
};

static_assert(AssertSize<RP1PWM::Registers, 0x64>());

AK_ENUM_BITWISE_OPERATORS(RP1PWM::Registers::Control)

}
