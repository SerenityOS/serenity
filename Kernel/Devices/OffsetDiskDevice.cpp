#include <Kernel/Devices/OffsetDiskDevice.h>

// #define OFFD_DEBUG

Retained<OffsetDiskDevice> OffsetDiskDevice::create(Retained<DiskDevice>&& device, unsigned offset)
{
    return adopt(*new OffsetDiskDevice(move(device), offset));
}

OffsetDiskDevice::OffsetDiskDevice(Retained<DiskDevice>&& device, unsigned offset)
    : m_device(move(device)), m_offset(offset)
{
}

OffsetDiskDevice::~OffsetDiskDevice()
{
}

unsigned OffsetDiskDevice::block_size() const
{
    return m_device->block_size();
}

bool OffsetDiskDevice::read_block(unsigned index, byte* out) const
{
#ifdef OFFD_DEBUG
    kprintf("OffsetDiskDevice::read_block %u (really: %u)\n", index, m_offset + index);
#endif

    return m_device->read_block(m_offset + index, out);
}

bool OffsetDiskDevice::write_block(unsigned index, const byte* data)
{
#ifdef OFFD_DEBUG
    kprintf("OffsetDiskDevice::write_block %u (really: %u)\n", index, m_offset + index);
#endif

    return m_device->write_block(m_offset + index, data);
}

bool OffsetDiskDevice::read_blocks(unsigned index, word count, byte* out)
{
#ifdef OFFD_DEBUG
    kprintf("OffsetDiskDevice::read_blocks %u (really: %u) count=%u\n", index, m_offset + index, count);
#endif

    return m_device->read_blocks(m_offset + index, count, out);
}

bool OffsetDiskDevice::write_blocks(unsigned index, word count, const byte* data)
{
#ifdef OFFD_DEBUG
    kprintf("OffsetDiskDevice::write_blocks %u (really: %u) count=%u\n", index, m_offset + index, count);
#endif

    return m_device->write_blocks(m_offset + index, count, data);
}

const char* OffsetDiskDevice::class_name() const
{
    return "OffsetDiskDevice";
}
