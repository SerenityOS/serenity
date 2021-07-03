/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {
namespace PCI {

#define PCI_MMIO_CONFIG_SPACE_SIZE 4096

class MMIOAccess : public Access {
public:
    class MMIOSegment {
    public:
        MMIOSegment(PhysicalAddress, u8, u8);
        u8 get_start_bus() const;
        u8 get_end_bus() const;
        size_t get_size() const;
        PhysicalAddress get_paddr() const;

    private:
        PhysicalAddress m_base_addr;
        u8 m_start_bus;
        u8 m_end_bus;
    };
    static void initialize(PhysicalAddress mcfg);

private:
    PhysicalAddress determine_memory_mapped_bus_region(u32 segment, u8 bus) const;
    void map_bus_region(u32, u8);
    VirtualAddress get_device_configuration_space(Address address);
    SpinLock<u8> m_access_lock;
    u8 m_mapped_bus { 0 };
    OwnPtr<Region> m_mapped_region;

protected:
    explicit MMIOAccess(PhysicalAddress mcfg);

    virtual const char* access_type() const override { return "MMIOAccess"; };
    virtual u32 segment_count() const override;
    virtual void enumerate_hardware(Function<void(Address, ID)>) override;
    virtual void write8_field(Address address, u32, u8) override;
    virtual void write16_field(Address address, u32, u16) override;
    virtual void write32_field(Address address, u32, u32) override;
    virtual u8 read8_field(Address address, u32) override;
    virtual u16 read16_field(Address address, u32) override;
    virtual u32 read32_field(Address address, u32) override;

    virtual u8 segment_start_bus(u32) const override;
    virtual u8 segment_end_bus(u32) const override;

    PhysicalAddress m_mcfg;
    HashMap<u16, MMIOSegment> m_segments;
};

}
}
