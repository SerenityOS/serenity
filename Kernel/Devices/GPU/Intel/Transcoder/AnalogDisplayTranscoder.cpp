/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Devices/GPU/Intel/Transcoder/AnalogDisplayTranscoder.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

ErrorOr<NonnullOwnPtr<IntelAnalogDisplayTranscoder>> IntelAnalogDisplayTranscoder::create_with_physical_addresses(PhysicalAddress transcoder_registers_start_address,
    PhysicalAddress pipe_registers_start_address, PhysicalAddress dpll_registers_start_address, PhysicalAddress dpll_multiplier_register_start_address)
{
    auto transcoder_registers_mapping = TRY(Memory::map_typed<TranscoderRegisters volatile>(transcoder_registers_start_address, sizeof(IntelDisplayTranscoder::TranscoderRegisters), Memory::Region::Access::ReadWrite));
    auto pipe_registers_mapping = TRY(Memory::map_typed<PipeRegisters volatile>(pipe_registers_start_address, sizeof(IntelDisplayTranscoder::PipeRegisters), Memory::Region::Access::ReadWrite));
    auto dpll_registers_mapping = TRY(Memory::map_typed<DPLLRegisters volatile>(dpll_registers_start_address, sizeof(DPLLRegisters), Memory::Region::Access::ReadWrite));
    auto dpll_control_mapping = TRY(Memory::map_typed<DPLLControlRegisters volatile>(dpll_multiplier_register_start_address, sizeof(DPLLControlRegisters), Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) IntelAnalogDisplayTranscoder(move(transcoder_registers_mapping), move(pipe_registers_mapping), move(dpll_registers_mapping), move(dpll_control_mapping)));
}

IntelAnalogDisplayTranscoder::IntelAnalogDisplayTranscoder(Memory::TypedMapping<TranscoderRegisters volatile> transcoder_registers_mapping,
    Memory::TypedMapping<PipeRegisters volatile> pipe_registers_mapping, Memory::TypedMapping<DPLLRegisters volatile> dpll_registers_mapping, Memory::TypedMapping<DPLLControlRegisters volatile> dpll_control_registers)
    : IntelDisplayTranscoder(move(transcoder_registers_mapping), move(pipe_registers_mapping))
    , m_dpll_registers(move(dpll_registers_mapping))
    , m_dpll_control_registers(move(dpll_control_registers))
{
}

ErrorOr<void> IntelAnalogDisplayTranscoder::set_dpll_settings(Badge<IntelDisplayConnectorGroup>, IntelGraphics::PLLSettings const& settings, size_t dac_multiplier)
{
    SpinlockLocker locker(m_access_lock);
    u32 value = (settings.m2 - 2) | ((settings.m1 - 2) << 8) | ((settings.n - 2) << 16);
    m_dpll_registers->divisor_a0 = value;
    m_dpll_registers->divisor_a1 = value;
    m_shadow_registers.dpll_divisor_a0 = value;
    m_shadow_registers.dpll_divisor_a1 = value;

    // Note: We don't set the DAC multiplier now but reserve it for later usage (e.g. when enabling the DPLL)
    m_shadow_registers.dpll_reserved_dac_multiplier = dac_multiplier;
    // Note: We don't set the DPLL P1 now but reserve it for later usage (e.g. when enabling the DPLL)
    m_shadow_registers.dpll_p1 = settings.p1;
    return {};
}

ErrorOr<void> IntelAnalogDisplayTranscoder::enable_dpll_without_vga(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    // Explanation for Gen4 DPLL control bits:
    // 1. 0b0110 in bits 9 to 12 - use clock phase 6 (Default)
    // 2. bits 24,25 - set to 0b00 to ensure FPA0/FPA1 (DPLL A Divisor 0, 1) divide by 10 (used for DAC modes under 270 MHz)
    // 3. bit 26 - set to 0b1 to ensure mode select to DAC mode
    // 4. bit 28 - set to 0b1 to disable VGA mode
    // 5. bit 31 - enable DPLL VCO (DPLL enabled and operational)
    u32 control_value = (6 << 9) | (m_shadow_registers.dpll_p1) << 16 | (1 << 26) | (1 << 28) | (1 << 31);
    m_dpll_control_registers->control = control_value;
    m_shadow_registers.dpll_control = control_value;

    // Explanation for Gen4 DPLL multiplier bits:
    // 1. 0b0110 in bits 9 to 12 - use clock phase 6 (Default)
    // 2. bits 24,25 - set to 0b00 to ensure FPA0/FPA1 (DPLL A Divisor 0, 1) divide by 10 (used for DAC modes under 270 MHz)
    // 3. bit 26 - set to 0b1 to ensure mode select to DAC mode
    // 4. bit 28 - set to 0b1 to disable VGA mode
    // 5. bit 31 - enable DPLL VCO (DPLL enabled and operational)
    u32 dac_multiplier_value = (m_shadow_registers.dpll_reserved_dac_multiplier - 1) | ((m_shadow_registers.dpll_reserved_dac_multiplier - 1) << 8);
    m_dpll_control_registers->multiplier = dac_multiplier_value;
    m_shadow_registers.dpll_raw_dac_multiplier = dac_multiplier_value;

    // The specification says we should wait (at least) about 150 microseconds
    // after enabling the DPLL to allow the clock to stabilize
    microseconds_delay(200);
    for (size_t milliseconds_elapsed = 0; milliseconds_elapsed < 5; milliseconds_elapsed++) {
        u32 control_value = m_dpll_control_registers->control;
        if (control_value & (1 << 31))
            return {};
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> IntelAnalogDisplayTranscoder::disable_dpll(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    m_dpll_control_registers->control = 0;
    m_shadow_registers.dpll_control = 0;
    return {};
}

}
