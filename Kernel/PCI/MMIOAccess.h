#pragma once
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/Region.h>

class PCI::MMIOAccess final : public PCI::Access {
public:
    static void initialize(ACPI_RAW::MCFG&);
    virtual void enumerate_all(Function<void(Address, ID)>&) override final;

    virtual String get_access_type() override final { return "MMIO-Access"; };

protected:
    MMIOAccess(ACPI_RAW::MCFG&);

private:
    virtual u8 read8_field(Address address, u32) override final;
    virtual u16 read16_field(Address address, u32) override final;
    virtual u32 read32_field(Address address, u32) override final;
    virtual void write8_field(Address address, u32, u8) override final;
    virtual void write16_field(Address address, u32, u16) override final;
    virtual void write32_field(Address address, u32, u32) override final;

    void map_device(Address address);
    void mmap(VirtualAddress preferred_vaddr, PhysicalAddress paddr, u32);
    void mmap_region(Region& region, PhysicalAddress paddr);

    virtual u32 get_segments_count();
    virtual u8 get_segment_start_bus(u32);
    virtual u8 get_segment_end_bus(u32);

    ACPI_RAW::MCFG& m_mcfg;
    HashMap<u16, MMIOSegment*>& m_segments;
    OwnPtr<Region> m_mmio_segment;
};

class PCI::MMIOSegment {
public:
    MMIOSegment(PhysicalAddress, u8, u8);
    u8 get_start_bus();
    u8 get_end_bus();
    size_t get_size();
    PhysicalAddress get_paddr();

private:
    PhysicalAddress m_base_addr;
    u8 m_start_bus;
    u8 m_end_bus;
};