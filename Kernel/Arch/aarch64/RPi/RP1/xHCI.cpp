/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/RP1/RP1.h>
#include <Kernel/Arch/aarch64/RPi/RP1/xHCI.h>

namespace Kernel::RPi {

ErrorOr<NonnullLockRefPtr<RP1xHCIController>> RP1xHCIController::try_to_initialize(RP1& rp1, PhysicalAddress paddr, size_t index, InterruptNumber interrupt_number)
{
    auto registers_mapping = TRY(Memory::map_typed_writable<u8>(paddr));

    auto controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) RP1xHCIController(rp1, move(registers_mapping), index, interrupt_number)));
    TRY(controller->initialize());
    return controller;
}

UNMAP_AFTER_INIT RP1xHCIController::RP1xHCIController(RP1& rp1, Memory::TypedMapping<u8> registers_mapping, size_t index, InterruptNumber interrupt_number)
    : xHCIController(move(registers_mapping))
    , m_rp1(rp1)
    , m_index(index)
    , m_interrupt_number(interrupt_number)
{
}

ErrorOr<NonnullOwnPtr<USB::xHCI::xHCIInterrupter>> RP1xHCIController::create_interrupter(u16 interrupter_id)
{
    return TRY(RP1xHCIInterrupter::create(m_rp1, *this, interrupter_id, m_interrupt_number));
}

ErrorOr<NonnullOwnPtr<RP1xHCIInterrupter>> RP1xHCIInterrupter::create(RP1& rp1, RP1xHCIController& controller, u16 interrupter_id, InterruptNumber interrupt_number)
{
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) RP1xHCIInterrupter(rp1, controller, interrupter_id, interrupt_number)));
}

RP1xHCIInterrupter::RP1xHCIInterrupter(RP1& rp1, RP1xHCIController& controller, u16 interrupter_id, InterruptNumber interrupt_number)
    : xHCIInterrupter(controller, interrupter_id)
{
    rp1.register_interrupt_handler(interrupt_number, RP1::InterruptTriggerMode::Edge, [this](InterruptNumber) {
        handle_interrupt();
        return true;
    });
}

}
