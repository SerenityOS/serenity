/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/NonnullOwnPtr.h>
#include <Kernel/Arch/aarch64/RPi/BroadcomSTBGPIOController.h>

namespace Kernel::RPi {

struct BroadcomSTBGPIOController::BankRegisters {
    u32 _;
    u32 pin_levels;
    u32 pin_directions;
    u32 _[5];
};

static_assert(AssertSize<BroadcomSTBGPIOController::BankRegisters, 8 * sizeof(u32)>());

ErrorOr<NonnullOwnPtr<BroadcomSTBGPIOController>> Kernel::RPi::BroadcomSTBGPIOController::create(PhysicalAddress paddr, Span<size_t> bank_pin_counts)
{
    for (auto bank_pin_count : bank_pin_counts) {
        if (bank_pin_count > 32) {
            return EINVAL;
        }
    }

    auto bank_count = bank_pin_counts.size();
    auto registers = TRY(Memory::map_typed_array<BankRegisters volatile>(paddr, bank_count, Memory::Region::Access::ReadWrite));

    auto bank_pin_counts_array = TRY(FixedArray<size_t>::create(bank_pin_counts));
    return adopt_nonnull_own_or_enomem(new (nothrow) BroadcomSTBGPIOController(move(registers), move(bank_pin_counts_array)));
}

ErrorOr<void> Kernel::RPi::BroadcomSTBGPIOController::set_pin_output_level(u32 pin_number, LogicLevel level)
{
    auto bank_number = pin_number / 32;
    auto pin_number_in_bank = pin_number % 32;

    if (bank_number >= m_bank_pin_counts.size())
        return EINVAL;

    if (pin_number_in_bank >= m_bank_pin_counts[bank_number])
        return EINVAL;

    if (level == LogicLevel::High)
        m_bank_registers[bank_number].pin_levels |= 1u << pin_number_in_bank;
    else
        m_bank_registers[bank_number].pin_levels &= ~(1u << pin_number_in_bank);

    return {};
}

ErrorOr<void> Kernel::RPi::BroadcomSTBGPIOController::set_pin_direction(u32 pin_number, PinDirection direction)
{
    auto bank_number = pin_number / 32;
    auto pin_number_in_bank = pin_number % 32;

    if (bank_number >= m_bank_pin_counts.size())
        return EINVAL;

    if (pin_number_in_bank >= m_bank_pin_counts[bank_number])
        return EINVAL;

    if (direction == PinDirection::Input)
        m_bank_registers[bank_number].pin_directions |= 1u << pin_number_in_bank;
    else
        m_bank_registers[bank_number].pin_directions &= ~(1u << pin_number_in_bank);

    return {};
}

BroadcomSTBGPIOController::BroadcomSTBGPIOController(Memory::TypedMapping<BankRegisters volatile[]> bank_registers, FixedArray<size_t> bank_pin_counts)
    : m_bank_registers(move(bank_registers))
    , m_bank_pin_counts(move(bank_pin_counts))
{
}

}
