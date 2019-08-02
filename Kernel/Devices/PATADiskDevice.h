//
// A Disk Device Connected to a PATA Channel
//
//
#pragma once

#include <AK/RefPtr.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/Devices/PATAChannel.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/PCI.h>
#include <Kernel/VM/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

class PATADiskDevice final : public DiskDevice {
    AK_MAKE_ETERNAL
public:
    // Type of drive this IDEDiskDevice is on the ATA channel.
    //
    // Each PATA channel can contain only two devices, which (I think) are
    // jumper selectable on the drive itself by shorting two pins.
    enum class DriveType : u8 {
        Master,
        Slave
    };

public:
    static NonnullRefPtr<PATADiskDevice> create(PATAChannel&, DriveType, int major, int minor);
    virtual ~PATADiskDevice() override;

    // ^DiskDevice
    virtual unsigned block_size() const override;
    virtual bool read_block(unsigned index, u8*) const override;
    virtual bool write_block(unsigned index, const u8*) override;
    virtual bool read_blocks(unsigned index, u16 count, u8*) override;
    virtual bool write_blocks(unsigned index, u16 count, const u8*) override;

    void set_drive_geometry(u16, u16, u16);

    // ^BlockDevice
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override { return 0; }
    virtual bool can_read(FileDescription&) const override { return true; }
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override { return 0; }
    virtual bool can_write(FileDescription&) const override { return true; }

protected:
    explicit PATADiskDevice(PATAChannel&, DriveType, int, int);

private:
    // ^DiskDevice
    virtual const char* class_name() const override;

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
    DriveType m_drive_type { DriveType::Master };

    PATAChannel& m_channel;
};
