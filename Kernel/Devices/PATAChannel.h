//
// Parallel ATA (PATA) controller driver
//
// This driver describes a logical PATA Channel. Each channel can connect up to 2
// IDE Hard Disk Drives. The drives themselves can be either the master drive (hd0)
// or the slave drive (hd1).
//
// More information about the ATA spec for PATA can be found here:
//      ftp://ftp.seagate.com/acrobat/reference/111-1c.pdf
//
#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/PCI.h>
#include <Kernel/VM/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

struct PhysicalRegionDescriptor {
    PhysicalAddress offset;
    u16 size { 0 };
    u16 end_of_table { 0 };
};

class PATADiskDevice;
class PATAChannel final : public IRQHandler {
    friend class PATADiskDevice;
    AK_MAKE_ETERNAL
public:
    enum class ChannelType : u8 {
        Primary,
        Secondary
    };

public:
    static OwnPtr<PATAChannel> create(ChannelType type, bool force_pio);
    PATAChannel(ChannelType type, bool force_pio);
    virtual ~PATAChannel() override;

    RefPtr<PATADiskDevice> master_device() { return m_master; };
    RefPtr<PATADiskDevice> slave_device() { return m_slave; };

private:
    //^ IRQHandler
    virtual void handle_irq() override;

    void initialize(bool force_pio);
    void detect_disks();

    bool wait_for_irq();
    bool ata_read_sectors_with_dma(u32, u16, u8*, bool);
    bool ata_write_sectors_with_dma(u32, u16, const u8*, bool);
    bool ata_read_sectors(u32, u16, u8*, bool);
    bool ata_write_sectors(u32, u16, const u8*, bool);

    PhysicalRegionDescriptor& prdt() { return *reinterpret_cast<PhysicalRegionDescriptor*>(m_prdt_page->paddr().as_ptr()); }

    // Data members
    u8 m_channel_number { 0 }; // Channel number. 0 = master, 1 = slave
    u16 m_io_base { 0x1F0 };
    u16 m_control_base { 0 };
    volatile u8 m_device_error { 0 };
    volatile bool m_interrupted { false };

    PCI::Address m_pci_address;
    RefPtr<PhysicalPage> m_prdt_page;
    RefPtr<PhysicalPage> m_dma_buffer_page;
    u16 m_bus_master_base { 0 };
    Lockable<bool> m_dma_enabled;
    Lockable<bool> m_force_pio;

    RefPtr<PATADiskDevice> m_master;
    RefPtr<PATADiskDevice> m_slave;
};
