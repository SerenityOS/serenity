#include <Kernel/Devices/PATAChannel.h>
#include <Kernel/Devices/PATADiskDevice.h>

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

bool PATADiskDevice::read_blocks(unsigned index, u16 count, u8* out)
{
    if (m_channel.m_bus_master_base && m_channel.m_dma_enabled.resource() && !m_channel.m_force_pio.resource())
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
