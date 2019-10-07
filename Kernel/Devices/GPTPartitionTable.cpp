#include <AK/ByteBuffer.h>
#include <Kernel/Devices/GPTPartitionTable.h>

#define GPT_DEBUG

GPTPartitionTable::GPTPartitionTable(DiskDevice& device)
    : m_device(move(device))
{
}

GPTPartitionTable::~GPTPartitionTable()
{
}

const GPTPartitionHeader& GPTPartitionTable::header() const
{
    return *reinterpret_cast<const GPTPartitionHeader*>(m_cached_header);
}

bool GPTPartitionTable::initialize()
{
    if (!m_device->read_block(1, m_cached_header)) {
        return false;
    }

    auto& header = this->header();

#ifdef GPT_DEBUG
    kprintf("GPTPartitionTable::initialize: gpt_signature=%#x%x\n", header.sig[1], header.sig[0]);
#endif

    if (header.sig[0] != GPT_SIGNATURE && header.sig[1] != GPT_SIGNATURE2) {
        kprintf("GPTPartitionTable::initialize: bad GPT signature %#x%x\n", header.sig[1], header.sig[0]);
        return false;
    }

    return true;
}

RefPtr<DiskPartition> GPTPartitionTable::partition(unsigned index)
{
    ASSERT(index >= 1 && index <= 4294967294);

    auto& header = this->header();
    unsigned lba = header.partition_array_start_lba + (((index - 1) * header.partition_entry_size) / BytesPerSector);

    if (header.sig[0] != GPT_SIGNATURE) {
        kprintf("GPTPartitionTable::initialize: bad gpt signature - not initalized? %#x\n", header.sig);
        return nullptr;
    }

    u8 entries_per_sector = BytesPerSector / header.partition_entry_size;

    GPTPartitionEntry entries[entries_per_sector];
    this->m_device->read_blocks(lba, 1, (u8*)&entries);
    GPTPartitionEntry& entry = entries[((index - 1) % entries_per_sector)];

#ifdef GPT_DEBUG
    kprintf("GPTPartitionTable::partition %d\n", index);
    kprintf("GPTPartitionTable - offset = %d%d\n", entry.first_lba[1], entry.first_lba[0]);
#endif

    if (entry.first_lba[0] == 0x00) {
#ifdef GPT_DEBUG
        kprintf("GPTPartitionTable::partition: missing partition requested index=%d\n", index);
#endif

        return nullptr;
    }

#ifdef GPT_DEBUG
    kprintf("GPTPartitionTable::partition: found partition index=%d type=%x-%x-%x-%x\n", index, entry.partition_guid[3], entry.partition_guid[2], entry.partition_guid[1], entry.partition_guid[0]);
#endif
    return DiskPartition::create(m_device, entry.first_lba[0], entry.last_lba[0]);
}
