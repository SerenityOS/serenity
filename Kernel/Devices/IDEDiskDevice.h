#pragma once

#include <AK/RetainPtr.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/PCI.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

struct PhysicalRegionDescriptor {
    PhysicalAddress offset;
    word size { 0 };
    word end_of_table { 0 };
};

class IDEDiskDevice final : public IRQHandler
    , public DiskDevice {
    AK_MAKE_ETERNAL
public:
    static Retained<IDEDiskDevice> create();
    virtual ~IDEDiskDevice() override;

    // ^DiskDevice
    virtual unsigned block_size() const override;
    virtual bool read_block(unsigned index, byte*) const override;
    virtual bool write_block(unsigned index, const byte*) override;
    virtual bool read_blocks(unsigned index, word count, byte*) override;
    virtual bool write_blocks(unsigned index, word count, const byte*) override;

protected:
    IDEDiskDevice();

private:
    // ^IRQHandler
    virtual void handle_irq() override;

    // ^DiskDevice
    virtual const char* class_name() const override;

    void initialize();
    bool wait_for_irq();
    bool read_sectors_with_dma(dword lba, word count, byte*);
    bool write_sectors_with_dma(dword lba, word count, const byte*);
    bool read_sectors(dword lba, word count, byte* buffer);
    bool write_sectors(dword lba, word count, const byte* data);

    Lock m_lock { "IDEDiskDevice" };
    word m_cylinders { 0 };
    word m_heads { 0 };
    word m_sectors_per_track { 0 };
    word m_io_base { 0 };
    volatile bool m_interrupted { false };
    volatile byte m_device_error { 0 };

    PCI::Address m_pci_address;
    PhysicalRegionDescriptor m_prdt;
    RetainPtr<PhysicalPage> m_dma_buffer_page;
    word m_bus_master_base { 0 };
    Lockable<bool> m_dma_enabled;
};
