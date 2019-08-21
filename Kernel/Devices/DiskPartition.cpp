#include <Kernel/Devices/DiskPartition.h>

// #define OFFD_DEBUG

NonnullRefPtr<DiskPartition> DiskPartition::create(DiskDevice& device, unsigned block_offset)
{
    return adopt(*new DiskPartition(device, block_offset));
}

DiskPartition::DiskPartition(DiskDevice& device, unsigned block_offset)
    : DiskDevice(100, 0, device.block_size())
    , m_device(device)
    , m_block_offset(block_offset)
{
}

DiskPartition::~DiskPartition()
{
}

bool DiskPartition::read_block(unsigned index, u8* out) const
{
#ifdef OFFD_DEBUG
    kprintf("DiskPartition::read_block %u (really: %u)\n", index, m_block_offset + index);
#endif

    return m_device->read_block(m_block_offset + index, out);
}

bool DiskPartition::write_block(unsigned index, const u8* data)
{
#ifdef OFFD_DEBUG
    kprintf("DiskPartition::write_block %u (really: %u)\n", index, m_block_offset + index);
#endif

    return m_device->write_block(m_block_offset + index, data);
}

bool DiskPartition::read_blocks(unsigned index, u16 count, u8* out)
{
#ifdef OFFD_DEBUG
    kprintf("DiskPartition::read_blocks %u (really: %u) count=%u\n", index, m_block_offset + index, count);
#endif

    return m_device->read_blocks(m_block_offset + index, count, out);
}

bool DiskPartition::write_blocks(unsigned index, u16 count, const u8* data)
{
#ifdef OFFD_DEBUG
    kprintf("DiskPartition::write_blocks %u (really: %u) count=%u\n", index, m_block_offset + index, count);
#endif

    return m_device->write_blocks(m_block_offset + index, count, data);
}

const char* DiskPartition::class_name() const
{
    return "DiskPartition";
}
