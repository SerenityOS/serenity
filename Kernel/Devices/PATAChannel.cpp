/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "PATADiskDevice.h"
#include <AK/ByteBuffer.h>
#include <Kernel/Devices/PATAChannel.h>
#include <Kernel/Devices/PIT.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/Process.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibBareMetal/IO.h>

namespace Kernel {

#define PATA_PRIMARY_IRQ 14
#define PATA_SECONDARY_IRQ 15

//#define PATA_DEBUG

#define ATA_SR_BSY 0x80
#define ATA_SR_DRDY 0x40
#define ATA_SR_DF 0x20
#define ATA_SR_DSC 0x10
#define ATA_SR_DRQ 0x08
#define ATA_SR_CORR 0x04
#define ATA_SR_IDX 0x02
#define ATA_SR_ERR 0x01

#define ATA_ER_BBK 0x80
#define ATA_ER_UNC 0x40
#define ATA_ER_MC 0x20
#define ATA_ER_IDNF 0x10
#define ATA_ER_MCR 0x08
#define ATA_ER_ABRT 0x04
#define ATA_ER_TK0NF 0x02
#define ATA_ER_AMNF 0x01

#define ATA_CMD_READ_PIO 0x20
#define ATA_CMD_READ_PIO_EXT 0x24
#define ATA_CMD_READ_DMA 0xC8
#define ATA_CMD_READ_DMA_EXT 0x25
#define ATA_CMD_WRITE_PIO 0x30
#define ATA_CMD_WRITE_PIO_EXT 0x34
#define ATA_CMD_WRITE_DMA 0xCA
#define ATA_CMD_WRITE_DMA_EXT 0x35
#define ATA_CMD_CACHE_FLUSH 0xE7
#define ATA_CMD_CACHE_FLUSH_EXT 0xEA
#define ATA_CMD_PACKET 0xA0
#define ATA_CMD_IDENTIFY_PACKET 0xA1
#define ATA_CMD_IDENTIFY 0xEC

#define ATAPI_CMD_READ 0xA8
#define ATAPI_CMD_EJECT 0x1B

#define ATA_IDENT_DEVICETYPE 0
#define ATA_IDENT_CYLINDERS 2
#define ATA_IDENT_HEADS 6
#define ATA_IDENT_SECTORS 12
#define ATA_IDENT_SERIAL 20
#define ATA_IDENT_MODEL 54
#define ATA_IDENT_CAPABILITIES 98
#define ATA_IDENT_FIELDVALID 106
#define ATA_IDENT_MAX_LBA 120
#define ATA_IDENT_COMMANDSETS 164
#define ATA_IDENT_MAX_LBA_EXT 200

#define IDE_ATA 0x00
#define IDE_ATAPI 0x01

#define ATA_REG_DATA 0x00
#define ATA_REG_ERROR 0x01
#define ATA_REG_FEATURES 0x01
#define ATA_REG_SECCOUNT0 0x02
#define ATA_REG_LBA0 0x03
#define ATA_REG_LBA1 0x04
#define ATA_REG_LBA2 0x05
#define ATA_REG_HDDEVSEL 0x06
#define ATA_REG_COMMAND 0x07
#define ATA_REG_STATUS 0x07
#define ATA_CTL_CONTROL 0x00
#define ATA_CTL_ALTSTATUS 0x00
#define ATA_CTL_DEVADDRESS 0x01

#define PCI_Mass_Storage_Class 0x1
#define PCI_IDE_Controller_Subclass 0x1
static Lock& s_lock()
{
    static Lock* lock;
    if (!lock)
        lock = new Lock;

    return *lock;
};

OwnPtr<PATAChannel> PATAChannel::create(ChannelType type, bool force_pio)
{
    PCI::Address pci_address;
    PCI::enumerate_all([&](const PCI::Address& address, PCI::ID id) {
        if (PCI::get_class(address) == PCI_Mass_Storage_Class && PCI::get_subclass(address) == PCI_IDE_Controller_Subclass) {
            pci_address = address;
            kprintf("PATAChannel: PATA Controller found! id=%w:%w\n", id.vendor_id, id.device_id);
        }
    });
    return make<PATAChannel>(pci_address, type, force_pio);
}

PATAChannel::PATAChannel(PCI::Address address, ChannelType type, bool force_pio)
    : PCI::Device(address, (type == ChannelType::Primary ? PATA_PRIMARY_IRQ : PATA_SECONDARY_IRQ))
    , m_channel_number((type == ChannelType::Primary ? 0 : 1))
    , m_io_base((type == ChannelType::Primary ? 0x1F0 : 0x170))
    , m_control_base((type == ChannelType::Primary ? 0x3f6 : 0x376))
{
    disable_irq();

    m_dma_enabled.resource() = true;
    ProcFS::add_sys_bool("ide_dma", m_dma_enabled);

    m_prdt_page = MM.allocate_supervisor_physical_page();

    initialize(force_pio);
    detect_disks();
}

PATAChannel::~PATAChannel()
{
}

void PATAChannel::initialize(bool force_pio)
{

    if (force_pio) {
        kprintf("PATAChannel: Requested to force PIO mode; not setting up DMA\n");
        return;
    }

    // Let's try to set up DMA transfers.
    PCI::enable_bus_mastering(pci_address());
    prdt().end_of_table = 0x8000;
    m_bus_master_base = PCI::get_BAR4(pci_address()) & 0xfffc;
    m_dma_buffer_page = MM.allocate_supervisor_physical_page();
    kprintf("PATAChannel: Bus master IDE: I/O @ %x\n", m_bus_master_base);
}

static void print_ide_status(u8 status)
{
    kprintf("PATAChannel: print_ide_status: DRQ=%u BSY=%u DRDY=%u DSC=%u DF=%u CORR=%u IDX=%u ERR=%u\n",
        (status & ATA_SR_DRQ) != 0,
        (status & ATA_SR_BSY) != 0,
        (status & ATA_SR_DRDY) != 0,
        (status & ATA_SR_DSC) != 0,
        (status & ATA_SR_DF) != 0,
        (status & ATA_SR_CORR) != 0,
        (status & ATA_SR_IDX) != 0,
        (status & ATA_SR_ERR) != 0);
}

void PATAChannel::wait_for_irq()
{
    cli();
    enable_irq();
    Thread::current->wait_on(m_irq_queue);
    disable_irq();
}

void PATAChannel::handle_irq(RegisterState&)
{
    u8 status = IO::in8(m_io_base + ATA_REG_STATUS);
    if (status & ATA_SR_ERR) {
        print_ide_status(status);
        m_device_error = IO::in8(m_io_base + ATA_REG_ERROR);
        kprintf("PATAChannel: Error %b!\n", m_device_error);
    } else {
        m_device_error = 0;
    }
#ifdef PATA_DEBUG
    kprintf("PATAChannel: interrupt: DRQ=%u BSY=%u DRDY=%u\n", (status & ATA_SR_DRQ) != 0, (status & ATA_SR_BSY) != 0, (status & ATA_SR_DRDY) != 0);
#endif
    m_irq_queue.wake_all();
}

static void io_delay()
{
    for (int i = 0; i < 4; ++i)
        IO::in8(0x3f6);
}

void PATAChannel::detect_disks()
{
    // There are only two possible disks connected to a channel
    for (auto i = 0; i < 2; i++) {
        IO::out8(m_io_base + ATA_REG_HDDEVSEL, 0xA0 | (i << 4)); // First, we need to select the drive itself

        // Apparently these need to be 0 before sending IDENTIFY?!
        IO::out8(m_io_base + ATA_REG_SECCOUNT0, 0x00);
        IO::out8(m_io_base + ATA_REG_LBA0, 0x00);
        IO::out8(m_io_base + ATA_REG_LBA1, 0x00);
        IO::out8(m_io_base + ATA_REG_LBA2, 0x00);

        IO::out8(m_io_base + ATA_REG_COMMAND, ATA_CMD_IDENTIFY); // Send the ATA_IDENTIFY command

        // Wait for the BSY flag to be reset
        while (IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_BSY)
            ;

        if (IO::in8(m_io_base + ATA_REG_STATUS) == 0x00) {
#ifdef PATA_DEBUG
            kprintf("PATAChannel: No %s disk detected!\n", (i == 0 ? "master" : "slave"));
#endif
            continue;
        }

        ByteBuffer wbuf = ByteBuffer::create_uninitialized(512);
        ByteBuffer bbuf = ByteBuffer::create_uninitialized(512);
        u8* b = bbuf.data();
        u16* w = (u16*)wbuf.data();
        const u16* wbufbase = (u16*)wbuf.data();

        for (u32 i = 0; i < 256; ++i) {
            u16 data = IO::in16(m_io_base + ATA_REG_DATA);
            *(w++) = data;
            *(b++) = MSB(data);
            *(b++) = LSB(data);
        }

        // "Unpad" the device name string.
        for (u32 i = 93; i > 54 && bbuf[i] == ' '; --i)
            bbuf[i] = 0;

        u8 cyls = wbufbase[1];
        u8 heads = wbufbase[3];
        u8 spt = wbufbase[6];

        kprintf(
            "PATAChannel: Name=\"%s\", C/H/Spt=%u/%u/%u\n",
            bbuf.data() + 54,
            cyls,
            heads,
            spt);

        int major = (m_channel_number == 0) ? 3 : 4;
        if (i == 0) {
            m_master = PATADiskDevice::create(*this, PATADiskDevice::DriveType::Master, major, 0);
            m_master->set_drive_geometry(cyls, heads, spt);
        } else {
            m_slave = PATADiskDevice::create(*this, PATADiskDevice::DriveType::Slave, major, 1);
            m_slave->set_drive_geometry(cyls, heads, spt);
        }
    }
}

bool PATAChannel::ata_read_sectors_with_dma(u32 lba, u16 count, u8* outbuf, bool slave_request)
{
    LOCKER(s_lock());
#ifdef PATA_DEBUG
    kprintf("%s(%u): PATAChannel::ata_read_sectors_with_dma (%u x%u) -> %p\n",
        Process::current->name().characters(),
        Process::current->pid(), lba, count, outbuf);
#endif

    prdt().offset = m_dma_buffer_page->paddr();
    prdt().size = 512 * count;

    ASSERT(prdt().size <= PAGE_SIZE);

    // Stop bus master
    IO::out8(m_bus_master_base, 0);

    // Write the PRDT location
    IO::out32(m_bus_master_base + 4, m_prdt_page->paddr().get());

    // Turn on "Interrupt" and "Error" flag. The error flag should be cleared by hardware.
    IO::out8(m_bus_master_base + 2, IO::in8(m_bus_master_base + 2) | 0x6);

    // Set transfer direction
    IO::out8(m_bus_master_base, 0x8);

    while (IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_BSY)
        ;

    u8 devsel = 0xe0;
    if (slave_request)
        devsel |= 0x10;

    IO::out8(m_control_base + ATA_CTL_CONTROL, 0);
    IO::out8(m_io_base + ATA_REG_HDDEVSEL, devsel | (static_cast<u8>(slave_request) << 4));
    io_delay();

    IO::out8(m_io_base + ATA_REG_FEATURES, 0);

    IO::out8(m_io_base + ATA_REG_SECCOUNT0, 0);
    IO::out8(m_io_base + ATA_REG_LBA0, 0);
    IO::out8(m_io_base + ATA_REG_LBA1, 0);
    IO::out8(m_io_base + ATA_REG_LBA2, 0);

    IO::out8(m_io_base + ATA_REG_SECCOUNT0, count);
    IO::out8(m_io_base + ATA_REG_LBA0, (lba & 0x000000ff) >> 0);
    IO::out8(m_io_base + ATA_REG_LBA1, (lba & 0x0000ff00) >> 8);
    IO::out8(m_io_base + ATA_REG_LBA2, (lba & 0x00ff0000) >> 16);

    for (;;) {
        auto status = IO::in8(m_io_base + ATA_REG_STATUS);
        if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRDY))
            break;
    }

    IO::out8(m_io_base + ATA_REG_COMMAND, ATA_CMD_READ_DMA_EXT);
    io_delay();

    // Start bus master
    IO::out8(m_bus_master_base, 0x9);

    wait_for_irq();

    if (m_device_error)
        return false;

    memcpy(outbuf, m_dma_buffer_page->paddr().offset(0xc0000000).as_ptr(), 512 * count);

    // I read somewhere that this may trigger a cache flush so let's do it.
    IO::out8(m_bus_master_base + 2, IO::in8(m_bus_master_base + 2) | 0x6);
    return true;
}

bool PATAChannel::ata_write_sectors_with_dma(u32 lba, u16 count, const u8* inbuf, bool slave_request)
{
    LOCKER(s_lock());
#ifdef PATA_DEBUG
    kprintf("%s(%u): PATAChannel::ata_write_sectors_with_dma (%u x%u) <- %p\n",
        Process::current->name().characters(),
        Process::current->pid(), lba, count, inbuf);
#endif

    prdt().offset = m_dma_buffer_page->paddr();
    prdt().size = 512 * count;

    memcpy(m_dma_buffer_page->paddr().offset(0xc0000000).as_ptr(), inbuf, 512 * count);

    ASSERT(prdt().size <= PAGE_SIZE);

    // Stop bus master
    IO::out8(m_bus_master_base, 0);

    // Write the PRDT location
    IO::out32(m_bus_master_base + 4, m_prdt_page->paddr().get());

    // Turn on "Interrupt" and "Error" flag. The error flag should be cleared by hardware.
    IO::out8(m_bus_master_base + 2, IO::in8(m_bus_master_base + 2) | 0x6);

    while (IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_BSY)
        ;

    u8 devsel = 0xe0;
    if (slave_request)
        devsel |= 0x10;

    IO::out8(m_control_base + ATA_CTL_CONTROL, 0);
    IO::out8(m_io_base + ATA_REG_HDDEVSEL, devsel | (static_cast<u8>(slave_request) << 4));
    io_delay();

    IO::out8(m_io_base + ATA_REG_FEATURES, 0);

    IO::out8(m_io_base + ATA_REG_SECCOUNT0, 0);
    IO::out8(m_io_base + ATA_REG_LBA0, 0);
    IO::out8(m_io_base + ATA_REG_LBA1, 0);
    IO::out8(m_io_base + ATA_REG_LBA2, 0);

    IO::out8(m_io_base + ATA_REG_SECCOUNT0, count);
    IO::out8(m_io_base + ATA_REG_LBA0, (lba & 0x000000ff) >> 0);
    IO::out8(m_io_base + ATA_REG_LBA1, (lba & 0x0000ff00) >> 8);
    IO::out8(m_io_base + ATA_REG_LBA2, (lba & 0x00ff0000) >> 16);

    for (;;) {
        auto status = IO::in8(m_io_base + ATA_REG_STATUS);
        if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRDY))
            break;
    }

    IO::out8(m_io_base + ATA_REG_COMMAND, ATA_CMD_WRITE_DMA_EXT);
    io_delay();

    // Start bus master
    IO::out8(m_bus_master_base, 0x1);

    wait_for_irq();

    if (m_device_error)
        return false;

    // I read somewhere that this may trigger a cache flush so let's do it.
    IO::out8(m_bus_master_base + 2, IO::in8(m_bus_master_base + 2) | 0x6);
    return true;
}

bool PATAChannel::ata_read_sectors(u32 start_sector, u16 count, u8* outbuf, bool slave_request)
{
    ASSERT(count <= 256);
    LOCKER(s_lock());
#ifdef PATA_DEBUG
    kprintf("%s(%u): PATAChannel::ata_read_sectors request (%u sector(s) @ %u into %p)\n",
        Process::current->name().characters(),
        Process::current->pid(),
        count,
        start_sector,
        outbuf);
#endif

    while (IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_BSY)
        ;

#ifdef PATA_DEBUG
    kprintf("PATAChannel: Reading %u sector(s) @ LBA %u\n", count, start_sector);
#endif

    u8 devsel = 0xe0;
    if (slave_request)
        devsel |= 0x10;

    IO::out8(m_io_base + ATA_REG_SECCOUNT0, count == 256 ? 0 : LSB(count));
    IO::out8(m_io_base + ATA_REG_LBA0, start_sector & 0xff);
    IO::out8(m_io_base + ATA_REG_LBA1, (start_sector >> 8) & 0xff);
    IO::out8(m_io_base + ATA_REG_LBA2, (start_sector >> 16) & 0xff);
    IO::out8(m_io_base + ATA_REG_HDDEVSEL, devsel | ((start_sector >> 24) & 0xf));

    IO::out8(0x3F6, 0x08);
    while (!(IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_DRDY))
        ;

    IO::out8(m_io_base + ATA_REG_COMMAND, ATA_CMD_READ_PIO);
    wait_for_irq();

    if (m_device_error)
        return false;

    for (int i = 0; i < count; i++) {
        io_delay();

        while (IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_BSY)
            ;

        u8 status = IO::in8(m_io_base + ATA_REG_STATUS);
        ASSERT(status & ATA_SR_DRQ);
#ifdef PATA_DEBUG
        kprintf("PATAChannel: Retrieving 512 bytes (part %d) (status=%b), outbuf=%p...\n", i, status, outbuf + (512 * i));
#endif

        IO::repeated_in16(m_io_base + ATA_REG_DATA, outbuf + (512 * i), 256);
    }

    return true;
}

bool PATAChannel::ata_write_sectors(u32 start_sector, u16 count, const u8* inbuf, bool slave_request)
{
    ASSERT(count <= 256);
    LOCKER(s_lock());
#ifdef PATA_DEBUG
    kprintf("%s(%u): PATAChannel::ata_write_sectors request (%u sector(s) @ %u)\n",
        Process::current->name().characters(),
        Process::current->pid(),
        count,
        start_sector);
#endif

    while (IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_BSY)
        ;

#ifdef PATA_DEBUG
    kprintf("PATAChannel: Writing %u sector(s) @ LBA %u\n", count, start_sector);
#endif

    u8 devsel = 0xe0;
    if (slave_request)
        devsel |= 0x10;

    IO::out8(m_io_base + ATA_REG_SECCOUNT0, count == 256 ? 0 : LSB(count));
    IO::out8(m_io_base + ATA_REG_LBA0, start_sector & 0xff);
    IO::out8(m_io_base + ATA_REG_LBA1, (start_sector >> 8) & 0xff);
    IO::out8(m_io_base + ATA_REG_LBA2, (start_sector >> 16) & 0xff);
    IO::out8(m_io_base + ATA_REG_HDDEVSEL, devsel | ((start_sector >> 24) & 0xf));

    IO::out8(0x3F6, 0x08);
    while (!(IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_DRDY))
        ;

    IO::out8(m_io_base + ATA_REG_COMMAND, ATA_CMD_WRITE_PIO);

    for (int i = 0; i < count; i++) {
        io_delay();
        while (IO::in8(m_io_base + ATA_REG_STATUS) & ATA_SR_BSY)
            ;

        u8 status = IO::in8(m_io_base + ATA_REG_STATUS);
        ASSERT(status & ATA_SR_DRQ);

#ifdef PATA_DEBUG
        kprintf("PATAChannel: Writing 512 bytes (part %d) (status=%b), inbuf=%p...\n", i, status, inbuf + (512 * i));
#endif

        IO::repeated_out16(m_io_base + ATA_REG_DATA, inbuf + (512 * i), 256);
        wait_for_irq();
        status = IO::in8(m_io_base + ATA_REG_STATUS);
        ASSERT(!(status & ATA_SR_BSY));
    }

    IO::out8(m_io_base + ATA_REG_COMMAND, ATA_CMD_CACHE_FLUSH);
    wait_for_irq();
    u8 status = IO::in8(m_io_base + ATA_REG_STATUS);
    ASSERT(!(status & ATA_SR_BSY));

    return !m_device_error;
}

}
