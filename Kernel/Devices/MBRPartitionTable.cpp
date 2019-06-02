#include <AK/ByteBuffer.h>
#include <Kernel/Devices/MBRPartitionTable.h>

#define MBR_DEBUG

Retained<MBRPartitionTable> MBRPartitionTable::create(Retained<DiskDevice>&& device)
{
    return adopt(*new MBRPartitionTable(move(device)));
}

MBRPartitionTable::MBRPartitionTable(Retained<DiskDevice>&& device)
    : m_device(move(device))
{
}

MBRPartitionTable::~MBRPartitionTable()
{
}

ByteBuffer MBRPartitionTable::read_header() const
{
    auto buffer = ByteBuffer::create_uninitialized(512);
    bool success = m_device->read_block(0, buffer.pointer());
    ASSERT(success);
    return buffer;
}

const MBRPartitionHeader& MBRPartitionTable::header() const
{
    if (!m_cached_header) {
        m_cached_header = read_header();
    }

    return *reinterpret_cast<MBRPartitionHeader*>(m_cached_header.pointer());
}

bool MBRPartitionTable::initialize()
{
    auto& header = this->header();

#ifdef MBR_DEBUG
    kprintf("MBRPartitionTable::initialize: mbr_signature=%#x\n", header.mbr_signature);
#endif

    if (header.mbr_signature != MBR_SIGNATURE) {
        kprintf("MBRPartitionTable::initialize: bad mbr signature %#x\n", header.mbr_signature);
        return false;
    }

    return true;
}

RetainPtr<DiskPartition> MBRPartitionTable::partition(unsigned index)
{
    ASSERT(index >= 1 && index <= 4);

    auto& header = this->header();
    auto& entry = header.entry[index - 1];

#ifdef MBR_DEBUG
    kprintf("MBRPartitionTable::partition: status=%#x offset=%#x\n", entry.status, entry.offset);
#endif

    if (entry.status == 0x00) {
        return nullptr;
    }

    return DiskPartition::create(m_device.copy_ref(), entry.offset);
}

const char* MBRPartitionTable::class_name() const
{
    return "MBRPartitionTable";
}
