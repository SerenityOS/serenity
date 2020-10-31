/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {
namespace PCI {

class DeviceConfigurationSpaceMapping {
public:
    DeviceConfigurationSpaceMapping(Address, const MMIOSegment&);
    VirtualAddress vaddr() const { return m_mapped_region->vaddr(); };
    PhysicalAddress paddr() const { return m_mapped_region->physical_page(0)->paddr(); }
    const Address& address() const { return m_device_address; };

private:
    Address m_device_address;
    NonnullOwnPtr<Region> m_mapped_region;
};

class MMIOAccess final : public Access {
public:
    static void initialize(PhysicalAddress mcfg);

protected:
    explicit MMIOAccess(PhysicalAddress mcfg);

private:
    virtual const char* access_type() const override { return "MMIO-Access"; };
    virtual u32 segment_count() const override;
    virtual void enumerate_hardware(Function<void(Address, ID)>) override;
    virtual void write8_field(Address address, u32, u8) override;
    virtual void write16_field(Address address, u32, u16) override;
    virtual void write32_field(Address address, u32, u32) override;
    virtual u8 read8_field(Address address, u32) override;
    virtual u16 read16_field(Address address, u32) override;
    virtual u32 read32_field(Address address, u32) override;

    Optional<VirtualAddress> get_device_configuration_space(Address address);
    virtual u8 segment_start_bus(u32) const override;
    virtual u8 segment_end_bus(u32) const override;

    PhysicalAddress m_mcfg;
    HashMap<u16, MMIOSegment> m_segments;
    NonnullOwnPtrVector<DeviceConfigurationSpaceMapping> m_mapped_device_regions;
};

}
}
