/*
 * Copyright (c) 2023, Pankaj R <dev@pankajraghav.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/PCIMSI.h>
#include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#include <Kernel/Arch/x86_64/PCI/MSI.h>
#include <Kernel/Arch/x86_64/ProcessorInfo.h>
#include <Kernel/Interrupts/InterruptDisabler.h>

namespace Kernel {
u64 msi_address_register(u8 destination_id, bool redirection_hint, bool destination_mode)
{
    u64 flags = 0;
    if (redirection_hint) {
        flags |= msi_redirection_hint;
        if (destination_mode)
            flags |= msi_destination_mode_logical;
    }
    return (msi_address_base | (Processor::by_id(destination_id).info().apic_id() << msi_destination_shift) | flags);
}

u32 msi_data_register(u8 vector, bool level_trigger, bool assert)
{
    u32 flags = 0;

    if (level_trigger) {
        flags |= msi_trigger_mode_level;
        if (assert)
            flags |= msi_level_assert;
    }
    return ((vector + IRQ_VECTOR_BASE) & msi_data_vector_mask) | flags;
}

u32 msix_vector_control_register(u32 vector_control, bool mask)
{
    if (!mask)
        return (vector_control & msi_vector_control_unmask);
    return (vector_control | msi_vector_control_mask);
}

void msi_signal_eoi()
{
    InterruptDisabler disabler;
    APIC::the().eoi();
}

}
