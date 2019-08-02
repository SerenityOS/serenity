#include "PATADiskDevice.h"
#include <Kernel/Arch/i386/PIC.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/StdLib.h>
#include <Kernel/VM/MemoryManager.h>

//#define DISK_DEBUG

#define IRQ_FIXED_DISK 14

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
#define ATA_REG_SECCOUNT1 0x08
#define ATA_REG_LBA3 0x09
#define ATA_REG_LBA4 0x0A
#define ATA_REG_LBA5 0x0B
#define ATA_REG_CONTROL 0x0C
#define ATA_REG_ALTSTATUS 0x0C
#define ATA_REG_DEVADDRESS 0x0D

NonnullRefPtr<PATADiskDevice> PATADiskDevice::create(PATAChannel& channel, DriveType type, int major, int minor)
{
    return adopt(*new PATADiskDevice(channel, type, major, minor));
}

PATADiskDevice::PATADiskDevice(PATAChannel& channel, DriveType type, int major, int minor)
    : DiskDevice(major, minor)
    , m_drive_type(type)
    , m_channel(channel)
{
}

PATADiskDevice::~PATADiskDevice()
{
}

const char* PATADiskDevice::class_name() const
{
    return "PATADiskDevice";
}

unsigned PATADiskDevice::block_size() const
{
    return 512;
}

bool PATADiskDevice::read_blocks(unsigned index, u16 count, u8* out)
{
    if (m_channel.m_bus_master_base && m_channel.m_dma_enabled.resource())
        return read_sectors_with_dma(index, count, out);
    return read_sectors(index, count, out);
}

bool PATADiskDevice::read_block(unsigned index, u8* out) const
{
    return const_cast<PATADiskDevice*>(this)->read_blocks(index, 1, out);
}

bool PATADiskDevice::write_blocks(unsigned index, u16 count, const u8* data)
{
    if (m_channel.m_bus_master_base && m_channel.m_dma_enabled.resource())
        return write_sectors_with_dma(index, count, data);
    for (unsigned i = 0; i < count; ++i) {
        if (!write_sectors(index + i, 1, data + i * 512))
            return false;
    }
    return true;
}

bool PATADiskDevice::write_block(unsigned index, const u8* data)
{
    return write_blocks(index, 1, data);
}

void PATADiskDevice::set_drive_geometry(u16 cyls, u16 heads, u16 spt)
{
    m_cylinders = cyls;
    m_heads = heads;
    m_sectors_per_track = spt;
}

bool PATADiskDevice::read_sectors_with_dma(u32 lba, u16 count, u8* outbuf)
{
    return m_channel.ata_read_sectors_with_dma(lba, count, outbuf, is_slave());
}

bool PATADiskDevice::read_sectors(u32 start_sector, u16 count, u8* outbuf)
{
    return m_channel.ata_read_sectors(start_sector, count, outbuf, is_slave());
}

bool PATADiskDevice::write_sectors_with_dma(u32 lba, u16 count, const u8* inbuf)
{
    return m_channel.ata_write_sectors_with_dma(lba, count, inbuf, is_slave());
}

bool PATADiskDevice::write_sectors(u32 start_sector, u16 count, const u8* inbuf)
{
    return m_channel.ata_write_sectors(start_sector, count, inbuf, is_slave());
}

bool PATADiskDevice::is_slave() const
{
    return m_drive_type == DriveType::Slave;
}
