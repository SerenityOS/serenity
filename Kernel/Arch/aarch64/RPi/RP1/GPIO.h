/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

class RP1;

class RP1GPIO : public AtomicRefCounted<RP1GPIO> {
public:
    static ErrorOr<NonnullRefPtr<RP1GPIO>> create(RP1&, PhysicalAddress io_bank0_registers_paddr);

    void set_pin_function(u32 pin_number, u8 function);

    struct IOBankRegisters;
    struct PadsBankRegisters;

private:
    RP1GPIO(RP1&, Array<Memory::TypedMapping<IOBankRegisters volatile>, 3> io_bank_registers);

    NonnullRefPtr<RP1> m_rp1;
    Array<Memory::TypedMapping<IOBankRegisters volatile>, 3> m_io_bank_registers;
};

struct RP1GPIO::IOBankRegisters {
    static constexpr size_t CONTROL_FUNCTION_SELECT_OFFSET = 0;
    static constexpr size_t CONTROL_FUNCTION_SELECT_MASK = 0x1f << 0;

    struct {
        u32 status;
        u32 control;
    } gpio[28];

    u8 _[0x100 - (0xdc + 4)];

    u32 raw_interrupts;
    u32 proc0_interrupt_enable;
    u32 proc0_interrupt_force;
    u32 proc0_interrupt_status;
    u32 proc1_interrupt_enable;
    u32 proc1_interrupt_force;
    u32 proc1_interrupt_status;
    u32 pcie_interrupt_enable;
    u32 pcie_interrupt_force;
    u32 pcie_interrupt_status;
};

static_assert(AssertSize<RP1GPIO::IOBankRegisters, 0x128>());

struct RP1GPIO::PadsBankRegisters {
    u32 voltage_select;

    u32 gpio_pad_control[28];
};

static_assert(AssertSize<RP1GPIO::PadsBankRegisters, 0x74>());

}
