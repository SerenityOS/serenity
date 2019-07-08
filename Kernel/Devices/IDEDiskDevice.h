#pragma once

#include <AK/RefPtr.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/PCI.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

struct PhysicalRegionDescriptor {
    PhysicalAddress offset;
    u16 size { 0 };
    u16 end_of_table { 0 };
};

class IDEDiskDevice final : public IRQHandler
    , public DiskDevice {
    AK_MAKE_ETERNAL
public:

    // Type of drive this IDEDiskDevice is on the ATA channel.
    //
    // Each PATA channel can contain only two devices, which (I think) are
    // jumper selectable on the drive itself by shorting two pins.
    enum class DriveType : u8 {
        MASTER,
        SLAVE
    };

public:
    static NonnullRefPtr<IDEDiskDevice> create(DriveType type);
    virtual ~IDEDiskDevice() override;

    // ^DiskDevice
    virtual unsigned block_size() const override;
    virtual bool read_block(unsigned index, u8*) const override;
    virtual bool write_block(unsigned index, const u8*) override;
    virtual bool read_blocks(unsigned index, u16 count, u8*) override;
    virtual bool write_blocks(unsigned index, u16 count, const u8*) override;

protected:
    explicit IDEDiskDevice(DriveType);

private:
    // ^IRQHandler
    virtual void handle_irq() override;

    // ^DiskDevice
    virtual const char* class_name() const override;

    void initialize();
    bool wait_for_irq();
    bool read_sectors_with_dma(u32 lba, u16 count, u8*);
    bool write_sectors_with_dma(u32 lba, u16 count, const u8*);
    bool read_sectors(u32 lba, u16 count, u8* buffer);
    bool write_sectors(u32 lba, u16 count, const u8* data);

    bool is_slave() const;

    Lock m_lock { "IDEDiskDevice" };
    u16 m_cylinders { 0 };
    u16 m_heads { 0 };
    u16 m_sectors_per_track { 0 };
    u16 m_io_base { 0 };
    volatile bool m_interrupted { false };
    volatile u8 m_device_error { 0 };

    DriveType m_drive_type { DriveType::MASTER };
    PCI::Address m_pci_address;
    PhysicalRegionDescriptor m_prdt;
    RefPtr<PhysicalPage> m_dma_buffer_page;
    u16 m_bus_master_base { 0 };
    Lockable<bool> m_dma_enabled;
};
