/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

class BroadcomSTBGPIOController {
public:
    static ErrorOr<NonnullOwnPtr<BroadcomSTBGPIOController>> create(PhysicalAddress, Span<size_t> bank_pin_counts);

    enum class LogicLevel : bool {
        Low = 0,
        High = 1,
    };

    enum class PinDirection : u8 {
        Input = 0,
        Output = 1,
    };

    ErrorOr<void> set_pin_output_level(u32 pin_number, LogicLevel);
    ErrorOr<void> set_pin_direction(u32 pin_number, PinDirection);

    struct BankRegisters;

private:
    BroadcomSTBGPIOController(Memory::TypedMapping<BankRegisters volatile[]>, FixedArray<size_t> bank_pin_counts);

    Memory::TypedMapping<BankRegisters volatile[]> m_bank_registers;
    FixedArray<size_t> m_bank_pin_counts;
};

}
