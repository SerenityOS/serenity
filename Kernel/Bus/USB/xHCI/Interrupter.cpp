/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <Kernel/Bus/USB/xHCI/Interrupter.h>
#include <Kernel/Bus/USB/xHCI/Utils.h>
#include <Kernel/Bus/USB/xHCI/xHCIController.h>

namespace Kernel::USB::xHCI {

Interrupter::Interrupter(xHCIController& controller, u16 interrupter_id, u8 irq)
    : PCI::IRQHandler(controller, irq)
    , m_interrupter_id(interrupter_id)
    , m_controller(controller)
    , m_registers(controller.m_runtime_regs->interrupt_set[interrupter_id])
{
}

ErrorOr<NonnullOwnPtr<Interrupter>> Interrupter::try_create(xHCIController& controller, u16 interrupter_id, u8 irq)
{
    auto interrupter = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Interrupter(controller, interrupter_id, irq)));
    TRY(interrupter->initialize());
    return interrupter;
}

ErrorOr<void> Interrupter::initialize()
{
    // Initialize each active interrupter by:
    // * Defining the Event Ring: (refer to section  4.9.4 for a discussion of Event Ring Management.)
    //   *  Allocate and initialize the Event Ring Segment(s).
    // FIXME: Maybe make use of more Event Ring Segments
    TRY(m_event_ring_segments.try_append(TRY(allocate_trb_ring(PAGE_SIZE, false))));

    //   * Allocate the Event Ring Segment Table (ERST)
    //     (section 6.5). Initialize ERST table entries to point
    //     to and to define the size (in TRBs) of the respective
    //     Event Ring Segment.
    auto* event_ring_segment_table = new (std::align_val_t(32), nothrow) EventRingSegmentTableEntry(m_event_ring_segments.first().data(), PAGE_SIZE);
    if (event_ring_segment_table == nullptr)
        return ENOMEM;
    m_event_ring_segment_table = { event_ring_segment_table, 1 };
    m_event_ring_segment_table[0].segment_size = m_event_ring_segments.first().size();
    //   * Program the Interrupter Event Ring Segment Table
    //     Size (ERSTSZ) register (5.5.2.3.1) with the number
    //     of segments described by the Event Ring Segment
    //     Table.
    m_registers.event_ring.segment_table_size = 1;
    //   * Program the Interrupter Event Ring Dequeue
    //     Pointer (ERDP) register (5.5.2.3.3) with the starting
    //     address of the first segment described by the
    //     Event Ring Segment Table.
    set_address(
        m_registers.event_ring.event_ring_deque_pointer.addr, m_event_ring_segments.first().data());
    //   * Program the Interrupter Event Ring Segment Table
    //     Base Address (ERSTBA) register (5.5.2.3.2) with a
    //     64-bit address pointer to where the Event Ring
    //     Segment Table is located.
    set_address(
        m_registers.event_ring.segment_table_address, m_event_ring_segment_table.data());
    //   * Note that writing the ERSTBA enables the Event
    //     Ring. Refer to section 4.9.4 for more information
    //     on the Event Ring registers and their initialization.

    // * Defining the interrupts:
    //   * Enable the MSI-X interrupt mechanism by setting
    //     the MSI-X Enable flag in the MSI-X Capability
    //     Structure Message Control register (5.2.8.3).
    // Done by the Controller
    //   * Initializing the Interval field of the Interrupt
    //     Moderation register (5.5.2.2) with the target
    //     interrupt moderation rate.
    // FIXME: Do this properly
    //   * Enable system bus interrupt generation by writing
    //     a ‘1’ to the Interrupter Enable (INTE) flag of the
    //     USBCMD register (5.4.1).
    // Done by the Controller
    //   * Enable the Interrupter by writing a ‘1’ to the
    //     Interrupt Enable (IE) field of the Interrupter
    //     Management register (5.5.2.1).
    enable();

    return {};
}

void Interrupter::enable()
{
    // FIXME: We might need to do some precondition checks here
    m_registers.interrupter_management.interrupt_enable = 0;
}

}
