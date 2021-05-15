/*
 * Copyright (c) 2021, Alexander Richards <electrodeyt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Storage/AHCIPort.h>

namespace Kernel {

class IntelAHCIPort : public AHCIPort {
public:
    UNMAP_AFTER_INIT static NonnullRefPtr<AHCIPort> create(const AHCIPortHandler&, volatile AHCI::PortRegisters&, u32 port_index);

protected:
    UNMAP_AFTER_INIT IntelAHCIPort(const AHCIPortHandler&, volatile AHCI::PortRegisters&, u32 port_index);
    bool virtual initialize(ScopedSpinLock<SpinLock<u8>>&) override;

    bool m_has_reset_quirk { false };
};

}
