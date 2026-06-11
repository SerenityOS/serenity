/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/xHCI/xHCIController.h>

namespace Kernel::RPi {

class RP1;

class RP1xHCIController final : public USB::xHCI::xHCIController {
public:
    static ErrorOr<NonnullLockRefPtr<RP1xHCIController>> try_to_initialize(RP1&, PhysicalAddress, size_t index, InterruptNumber interrupt_number);

private:
    RP1xHCIController(RP1&, Memory::TypedMapping<u8> registers_mapping, size_t index, InterruptNumber interrupt_number);

    // ^xHCIController
    virtual bool using_message_signalled_interrupts() const override { return m_using_message_signalled_interrupts; }
    virtual ErrorOr<NonnullOwnPtr<USB::xHCI::xHCIInterrupter>> create_interrupter(u16 interrupter_id) override;
    virtual ErrorOr<void> write_dmesgln_prefix(StringBuilder& builder) const override
    {
        TRY(builder.try_appendff("xHCI: RP1 USBHOST{}: "sv, m_index));
        return {};
    }

    NonnullRefPtr<RP1> m_rp1;
    size_t m_index { 0 };
    InterruptNumber m_interrupt_number { 0 };
    bool m_using_message_signalled_interrupts { false };
};

class RP1xHCIInterrupter final : public USB::xHCI::xHCIInterrupter {
public:
    static ErrorOr<NonnullOwnPtr<RP1xHCIInterrupter>> create(RP1&, RP1xHCIController&, u16 interrupter_id, InterruptNumber);

private:
    RP1xHCIInterrupter(RP1&, RP1xHCIController& controller, u16 interrupter_id, InterruptNumber);
};

}
