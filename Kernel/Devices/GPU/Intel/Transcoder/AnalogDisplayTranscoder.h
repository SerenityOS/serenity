/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Devices/GPU/Intel/Transcoder/DisplayTranscoder.h>

namespace Kernel {

class IntelDisplayConnectorGroup;
class IntelAnalogDisplayTranscoder final : public IntelDisplayTranscoder {
public:
    static ErrorOr<NonnullOwnPtr<IntelAnalogDisplayTranscoder>> create_with_physical_addresses(PhysicalAddress transcoder_registers_start_address,
        PhysicalAddress pipe_registers_start_address, PhysicalAddress dpll_registers_start_address, PhysicalAddress dpll_control_registers_start_address);

    virtual ErrorOr<void> set_dpll_settings(Badge<IntelDisplayConnectorGroup>, IntelGraphics::PLLSettings const& settings, size_t dac_multiplier) override;
    virtual ErrorOr<void> enable_dpll_without_vga(Badge<IntelDisplayConnectorGroup>) override;
    virtual ErrorOr<void> disable_dpll(Badge<IntelDisplayConnectorGroup>) override;

private:
    struct [[gnu::packed]] DPLLRegisters {
        u32 divisor_a0;
        u32 divisor_a1;
    };

    struct [[gnu::packed]] DPLLControlRegisters {
        u32 control;
        u32 padding; // On Gen4, this is the control register of DPLL B, don't touch this
        u32 multiplier;
    };

    IntelAnalogDisplayTranscoder(Memory::TypedMapping<TranscoderRegisters volatile>, Memory::TypedMapping<PipeRegisters volatile>, Memory::TypedMapping<DPLLRegisters volatile>, Memory::TypedMapping<DPLLControlRegisters volatile>);
    Memory::TypedMapping<DPLLRegisters volatile> m_dpll_registers;
    Memory::TypedMapping<DPLLControlRegisters volatile> m_dpll_control_registers;
};
}
