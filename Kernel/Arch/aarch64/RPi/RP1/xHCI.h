/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/xHCI/xHCIController.h>

namespace Kernel::RPi {

class RP1xHCIController final : public USB::xHCI::xHCIController {
public:
    static ErrorOr<NonnullLockRefPtr<RP1xHCIController>> try_to_initialize(PhysicalAddress, size_t index, InterruptNumber interrupt_number);

private:
    RP1xHCIController(Memory::TypedMapping<u8> registers_mapping, size_t index, InterruptNumber interrupt_number);

    // ^xHCIController
    virtual bool using_message_signalled_interrupts() const override { return m_using_message_signalled_interrupts; }
    virtual ErrorOr<OwnPtr<USB::xHCI::xHCIInterrupter>> create_interrupter(u16 interrupter_id) override;
    virtual ErrorOr<void> write_dmesgln_prefix(StringBuilder& builder) const override
    {
        TRY(builder.try_appendff("xHCI: RP1 USBHOST{}: "sv, m_index));
        return {};
    }

    size_t m_index { 0 };
    InterruptNumber m_interrupt_number { 0 };
    bool m_using_message_signalled_interrupts { false };
};

}
