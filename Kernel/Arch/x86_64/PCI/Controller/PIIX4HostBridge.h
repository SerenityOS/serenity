/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

class PIIX4HostBridge : public HostController {
public:
    static NonnullOwnPtr<PIIX4HostBridge> must_create_with_io_access();

private:
    virtual void write8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) override;
    virtual void write16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) override;
    virtual void write32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) override;

    virtual u8 read8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u16 read16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u32 read32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;

    explicit PIIX4HostBridge(PCI::Domain const&);
};

}
