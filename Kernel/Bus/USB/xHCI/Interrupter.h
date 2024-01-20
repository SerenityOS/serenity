/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/USB/xHCI/DataStructures.h>
#include <Kernel/Bus/USB/xHCI/Registers.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>

namespace Kernel::USB::xHCI {

class xHCIController;

class Interrupter : public PCI::IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<Interrupter>> try_create(xHCIController&, u16 interrupter_id, u8 irq);
    ~Interrupter();
    // ^IRQHandler
    virtual bool handle_irq(RegisterState const&) override;
    virtual StringView purpose() const override { return "xHCI Interrupter"sv; }

    void enable();
    void disable();

private:
    Interrupter(xHCIController&, u16 interrupter_id, u8 irq);
    // Software maintains an Event Ring Consumer Cycle State (CCS) bit, initializing it
    // to ‘1’ and toggling it every time the Event Ring Dequeue Pointer wraps back to
    // the beginning of the Event Ring.
    bool m_event_ring_consumer_cycle_state { true };

    u16 m_interrupter_id;
    xHCIController& m_controller;
    RuntimeRegisters::InterrupterRegisters volatile& m_registers;
    Span<EventRingSegmentTableEntry> m_event_ring_segment_table;
    Vector<Span<TransferRequestBlock>> m_event_ring_segments;

    ErrorOr<void> initialize();
};

}
