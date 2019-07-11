#include <AK/ByteBuffer.h>
#include <Kernel/Devices/MBRPartitionTable.h>

#define MBR_DEBUG

MBRPartitionTable::MBRPartitionTable(NonnullRefPtr<DiskDevice> device)
    : m_device(move(device))
{
}

MBRPartitionTable::~MBRPartitionTable()
{
}

const MBRPartitionHeader& MBRPartitionTable::header() const
{
    return *reinterpret_cast<const MBRPartitionHeader*>(m_cached_header);
}

bool MBRPartitionTable::initialize()
{
    if (!m_device->read_block(0, m_cached_header)) {
        return false;
    }

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

RefPtr<DiskPartition> MBRPartitionTable::partition(unsigned index)
{
    ASSERT(index >= 1 && index <= 4);

    auto& header = this->header();
    auto& entry = header.entry[index - 1];

    if (header.mbr_signature != MBR_SIGNATURE) {
        kprintf("MBRPartitionTable::initialize: bad mbr signature - not initalized? %#x\n", header.mbr_signature);
        return nullptr;
    }

#ifdef MBR_DEBUG
    kprintf("MBRPartitionTable::partition: status=%#x offset=%#x\n", entry.status, entry.offset);
#endif

    if (entry.offset == 0x00) {
#ifdef MBR_DEBUG
    kprintf("MBRPartitionTable::partition: missing partition requested index=%d\n", index);
#endif

        return nullptr;
    }

#ifdef MBR_DEBUG
    kprintf("MBRPartitionTable::partition: found partition index=%d type=%x\n", index, entry.type);
#endif

    return DiskPartition::create(m_device, entry.offset);
}
