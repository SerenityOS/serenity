/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Interrupts/Interrupts.h>
#include <Kernel/Net/Cadence/GEM.h>

namespace Kernel::RPi {

class RP1;

class RP1GEMNetworkAdapter final : public CadenceGEMNetworkAdapter {
public:
    static ErrorOr<NonnullRefPtr<RP1GEMNetworkAdapter>> create(RP1&, StringView interface_name, PhysicalAddress, InterruptNumber);

private:
    RP1GEMNetworkAdapter(RP1&, StringView interface_name, MACAddress, Memory::TypedMapping<CadenceGEMNetworkAdapter::Registers volatile>, InterruptNumber);

    virtual void register_and_enable_interrupt() override;
    virtual u32 mdio_clock_input_frequency() override;

    RP1& m_rp1;
    InterruptNumber m_interrupt_number { 0 };
};

}
