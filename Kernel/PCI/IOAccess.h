#pragma once
#include <Kernel/PCI/Access.h>

class PCI::IOAccess final : public PCI::Access {
public:
    static void initialize();
    virtual void enumerate_all(Function<void(Address, ID)>&) override final;

    virtual String get_access_type() override final { return "IO-Access"; };

protected:
    IOAccess();

private:
    virtual u8 read8_field(Address address, u32) override final;
    virtual u16 read16_field(Address address, u32) override final;
    virtual u32 read32_field(Address address, u32) override final;
    virtual void write8_field(Address address, u32, u8) override final;
    virtual void write16_field(Address address, u32, u16) override final;
    virtual void write32_field(Address address, u32, u32) override final;

    virtual uint32_t get_segments_count() { return 1; };
    virtual uint8_t get_segment_start_bus(u32) { return 0x0; };
    virtual uint8_t get_segment_end_bus(u32) { return 0xFF; };
};