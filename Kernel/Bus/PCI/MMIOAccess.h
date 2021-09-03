/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/HashMap.h>
#include <YAK/NonnullOwnPtrVector.h>
#include <YAK/OwnPtr.h>
#include <YAK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/PhysicalRegion.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/VMObject.h>

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
    Spinlock<u8> m_access_lock;
    u8 m_mapped_bus { 0 };
    OwnPtr<Memory::Region> m_mapped_region;

protected:
    explicit MMIOAccess(PhysicalAddress mcfg);

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
