/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Access.h>

namespace Kernel {
namespace PCI {

class IOAccess final : public PCI::Access {
public:
    static void initialize();

protected:
    IOAccess();

private:
    virtual void enumerate_hardware(Function<void(Address, ID)>) override;
    virtual uint32_t segment_count() const override { return 1; };
    virtual void write8_field(Address address, u32, u8) override final;
    virtual void write16_field(Address address, u32, u16) override final;
    virtual void write32_field(Address address, u32, u32) override final;
    virtual u8 read8_field(Address address, u32) override;
    virtual u16 read16_field(Address address, u32) override;
    virtual u32 read32_field(Address address, u32) override;

    virtual uint8_t segment_start_bus(u32) const override { return 0x0; }
    virtual uint8_t segment_end_bus(u32) const override { return 0xFF; }
};

}
}
