/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/MMIOAccess.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {
namespace PCI {

class DeviceConfigurationSpaceMapping {
public:
    DeviceConfigurationSpaceMapping(Address, const MMIOAccess::MMIOSegment&);
    VirtualAddress vaddr() const { return m_mapped_region->vaddr(); };
    PhysicalAddress paddr() const { return m_mapped_region->physical_page(0)->paddr(); }
    const Address& address() const { return m_device_address; };

private:
    Address m_device_address;
    NonnullOwnPtr<Region> m_mapped_region;
};

class WindowedMMIOAccess final : public MMIOAccess {
public:
    static void initialize(PhysicalAddress mcfg);

private:
    explicit WindowedMMIOAccess(PhysicalAddress mcfg);
    virtual void write8_field(Address address, u32, u8) override;
    virtual void write16_field(Address address, u32, u16) override;
    virtual void write32_field(Address address, u32, u32) override;
    virtual u8 read8_field(Address address, u32) override;
    virtual u16 read16_field(Address address, u32) override;
    virtual u32 read32_field(Address address, u32) override;

    Optional<VirtualAddress> get_device_configuration_space(Address address);
    NonnullOwnPtrVector<DeviceConfigurationSpaceMapping> m_mapped_device_regions;
};

}
}
