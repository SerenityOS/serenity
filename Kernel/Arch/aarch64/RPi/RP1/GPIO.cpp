/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Enumerate.h>
#include <Kernel/Arch/aarch64/RPi/RP1/GPIO.h>
#include <Kernel/Arch/aarch64/RPi/RP1/RP1.h>

namespace Kernel::RPi {

namespace {

struct BankAndRelativePinNumber {
    u8 bank_number;
    u8 relative_pin_number;
};

ErrorOr<BankAndRelativePinNumber> pin_number_to_bank_and_relative_pin_number(u32 pin_number)
{
    struct PinRange {
        u32 start;
        u32 end;
    };

    static constexpr auto bank_map = to_array<PinRange>({
        { 0, 28 },
        { 28, 34 },
        { 34, 54 },
    });

    bool found = false;
    u8 bank_number = 0;
    u8 relative_pin_number = 0;

    for (auto [i, range] : enumerate(bank_map)) {
        if (pin_number >= range.start && pin_number < range.end) {
            bank_number = i;
            relative_pin_number = pin_number - range.start;

            found = true;
            break;
        }
    }

    if (!found)
        return EINVAL;

    return BankAndRelativePinNumber {
        .bank_number = bank_number,
        .relative_pin_number = relative_pin_number,
    };
}

}

ErrorOr<NonnullRefPtr<RP1GPIO>> RP1GPIO::create(RP1& rp1, PhysicalAddress io_bank0_registers_paddr, PhysicalAddress pads_bank0_registers_paddr)
{
    auto io_bank0_registers = TRY(Memory::map_typed_writable<IOBankRegisters volatile>(io_bank0_registers_paddr));
    auto io_bank1_registers = TRY(Memory::map_typed_writable<IOBankRegisters volatile>(io_bank0_registers_paddr.offset(0x4000)));
    auto io_bank2_registers = TRY(Memory::map_typed_writable<IOBankRegisters volatile>(io_bank0_registers_paddr.offset(0x8000)));

    auto pads_bank0_registers = TRY(Memory::map_typed_writable<PadsBankRegisters volatile>(pads_bank0_registers_paddr));
    auto pads_bank1_registers = TRY(Memory::map_typed_writable<PadsBankRegisters volatile>(pads_bank0_registers_paddr.offset(0x4000)));
    auto pads_bank2_registers = TRY(Memory::map_typed_writable<PadsBankRegisters volatile>(pads_bank0_registers_paddr.offset(0x8000)));

    Array io_bank_registers = { move(io_bank0_registers), move(io_bank1_registers), move(io_bank2_registers) };
    Array pads_bank_registers = { move(pads_bank0_registers), move(pads_bank1_registers), move(pads_bank2_registers) };

    return adopt_nonnull_ref_or_enomem(new (nothrow) RP1GPIO(rp1, move(io_bank_registers), move(pads_bank_registers)));
}

void RP1GPIO::set_pin_function(u32 pin_number, u8 function)
{
    VERIFY(function <= 8 || function == FUNCTION_NONE);

    auto [bank_number, relative_pin_number] = MUST(pin_number_to_bank_and_relative_pin_number(pin_number));

    auto control = m_io_bank_registers[bank_number]->gpio[relative_pin_number].control;

    control &= ~IOBankRegisters::CONTROL_FUNCTION_SELECT_MASK;
    control |= static_cast<u32>(function) << IOBankRegisters::CONTROL_FUNCTION_SELECT_OFFSET;

    m_io_bank_registers[bank_number]->gpio[relative_pin_number].control = control;
}

void RP1GPIO::set_pin_enabled(u32 pin_number, bool enabled)
{
    auto [bank_number, relative_pin_number] = MUST(pin_number_to_bank_and_relative_pin_number(pin_number));

    auto pad_control = m_pads_bank_registers[bank_number]->pad_control[relative_pin_number];

    if (enabled)
        pad_control &= ~PadsBankRegisters::PadControl::OutputDisable;
    else
        pad_control |= PadsBankRegisters::PadControl::OutputDisable;

    m_pads_bank_registers[bank_number]->pad_control[relative_pin_number] = pad_control;
}

void RP1GPIO::set_output_enable_override(u32 pin_number, OutputEnableOverride output_enable_override)
{
    auto [bank_number, relative_pin_number] = MUST(pin_number_to_bank_and_relative_pin_number(pin_number));

    auto control = m_io_bank_registers[bank_number]->gpio[relative_pin_number].control;

    control &= ~IOBankRegisters::CONTROL_OUTPUT_ENABLE_OVERRIDE_MASK;
    control |= static_cast<u32>(output_enable_override) << IOBankRegisters::CONTROL_OUTPUT_ENABLE_OVERRIDE_OFFSET;

    m_io_bank_registers[bank_number]->gpio[relative_pin_number].control = control;
}

void RP1GPIO::set_output_override(u32 pin_number, OutputOverride output_override)
{
    auto [bank_number, relative_pin_number] = MUST(pin_number_to_bank_and_relative_pin_number(pin_number));

    auto control = m_io_bank_registers[bank_number]->gpio[relative_pin_number].control;

    control &= ~IOBankRegisters::CONTROL_OUTPUT_OVERRIDE_MASK;
    control |= static_cast<u32>(output_override) << IOBankRegisters::CONTROL_OUTPUT_OVERRIDE_OFFSET;

    m_io_bank_registers[bank_number]->gpio[relative_pin_number].control = control;
}

RP1GPIO::RP1GPIO(RP1& rp1, Array<Memory::TypedMapping<IOBankRegisters volatile>, 3> io_bank_registers, Array<Memory::TypedMapping<PadsBankRegisters volatile>, 3> pads_bank_registers)
    : m_rp1(rp1)
    , m_io_bank_registers(move(io_bank_registers))
    , m_pads_bank_registers(move(pads_bank_registers))
{
}

}
