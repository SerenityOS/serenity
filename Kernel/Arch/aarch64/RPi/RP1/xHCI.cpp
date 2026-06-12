/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
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

ErrorOr<OwnPtr<USB::xHCI::xHCIInterrupter>> RP1xHCIController::create_interrupter(u16)
{
    // FIXME: Add interrupt support. This requires adding support for the BCM2712 MSI-X interrupt controller.
    return nullptr;
}

}
