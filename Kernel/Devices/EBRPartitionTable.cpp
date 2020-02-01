/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/ByteBuffer.h>
#include <Kernel/Devices/EBRPartitionTable.h>

#define EBR_DEBUG

EBRPartitionTable::EBRPartitionTable(NonnullRefPtr<DiskDevice> device)
    : m_device(move(device))
{
}

EBRPartitionTable::~EBRPartitionTable()
{
}

const MBRPartitionHeader& EBRPartitionTable::header() const
{
    return *reinterpret_cast<const MBRPartitionHeader*>(m_cached_mbr_header);
}

const EBRPartitionExtension& EBRPartitionTable::ebr_extension() const
{
    return *reinterpret_cast<const EBRPartitionExtension*>(m_cached_ebr_header);
}

int EBRPartitionTable::index_of_ebr_container() const
{
    for (int i = 0; i < 4; i++) {
        if (header().entry[i].type == EBR_CHS_CONTAINER || header().entry[i].type == EBR_LBA_CONTAINER)
            return i;
    }
    ASSERT_NOT_REACHED();
}

bool EBRPartitionTable::initialize()
{
    if (!m_device->read_block(0, m_cached_mbr_header)) {
        return false;
    }
    auto& header = this->header();

    m_ebr_container_id = index_of_ebr_container() + 1;

#ifdef EBR_DEBUG
    kprintf("EBRPartitionTable::initialize: MBR_signature=%#x\n", header.mbr_signature);
#endif

    if (header.mbr_signature != MBR_SIGNATURE) {
        kprintf("EBRPartitionTable::initialize: bad MBR signature %#x\n", header.mbr_signature);
        return false;
    }

    auto& ebr_entry = header.entry[m_ebr_container_id - 1];
    if (!m_device->read_block(ebr_entry.offset, m_cached_ebr_header)) {
        return false;
    }
    size_t index = 1;
    while (index < 128) { // Unlikely to encounter a disk with 128 partitions in this configuration...
        if (ebr_extension().next_chained_ebr_extension.offset == 0 && ebr_extension().next_chained_ebr_extension.type == 0) {
            break;
        }
        index++;
        if (!m_device->read_block(ebr_extension().next_chained_ebr_extension.offset, m_cached_ebr_header)) {
            return false;
        }
    }

    m_ebr_chained_extensions_count = index;

    kprintf("EBRPartitionTable::initialize: Extended partitions count - %d\n", m_ebr_chained_extensions_count);

    return true;
}

RefPtr<DiskPartition> EBRPartitionTable::get_non_extended_partition(unsigned index)
{
    auto& header = this->header();
    auto& entry = header.entry[index - 1];

#ifdef EBR_DEBUG
    kprintf("EBRPartitionTable::partition: status=%#x offset=%#x\n", entry.status, entry.offset);
#endif

    if (entry.offset == 0x00) {
#ifdef EBR_DEBUG
        kprintf("EBRPartitionTable::partition: missing partition requested index=%d\n", index);
#endif

        return nullptr;
    }

#ifdef EBR_DEBUG
    kprintf("EBRPartitionTable::partition: found partition index=%d type=%x\n", index, entry.type);
#endif

    return DiskPartition::create(m_device, entry.offset, (entry.offset + entry.length));
}

RefPtr<DiskPartition> EBRPartitionTable::get_extended_partition(unsigned index)
{

    unsigned relative_index = index - m_ebr_container_id;
    auto& header = this->header();

#ifdef EBR_DEBUG
    kprintf("EBRPartitionTable::partition: relative index %d\n", relative_index);
#endif

    auto& ebr_entry = header.entry[m_ebr_container_id - 1];
#ifdef EBR_DEBUG
    kprintf("EBRPartitionTable::partition: Extended partition, offset 0x%x, type %x\n", ebr_entry.offset, ebr_entry.type);
#endif

    if (!m_device->read_block(ebr_entry.offset, m_cached_ebr_header)) {
        return nullptr;
    }
    size_t i = 0;
    while (i < relative_index) {
#ifdef EBR_DEBUG
        kprintf("EBRPartitionTable::partition: logical partition, relative offset 0x%x, type %x\n", ebr_extension().entry.offset, ebr_extension().entry.type);
        kprintf("EBRPartitionTable::partition: next logical partition, relative offset 0x%x, type %x\n", ebr_extension().next_chained_ebr_extension.offset, ebr_extension().next_chained_ebr_extension.type);
#endif
        if (ebr_extension().next_chained_ebr_extension.offset == 0 && ebr_extension().next_chained_ebr_extension.type == 0) {
            break;
        }

        i++;
        if (!m_device->read_block(ebr_extension().next_chained_ebr_extension.offset, m_cached_ebr_header)) {
            return nullptr;
        }
    }

#ifdef EBR_DEBUG
    kprintf("EBRPartitionTable::partition: status=%#x offset=%#x\n", ebr_extension().entry.status, ebr_extension().entry.offset + ebr_entry.offset);
#endif

    if (ebr_extension().entry.offset == 0x00) {
#ifdef EBR_DEBUG
        kprintf("EBRPartitionTable::partition: missing partition requested index=%d\n", index);
#endif

        return nullptr;
    }

#ifdef EBR_DEBUG
    kprintf("EBRPartitionTable::partition: found partition index=%d type=%x\n", index, ebr_extension().entry.type);
#endif

    return DiskPartition::create(m_device, ebr_extension().entry.offset + ebr_entry.offset, (ebr_extension().entry.offset + ebr_entry.offset + ebr_extension().entry.length));
}

bool EBRPartitionTable::index_is_extended_partition(unsigned index) const
{
    return !(m_ebr_container_id > index || index > (m_ebr_container_id + m_ebr_chained_extensions_count));
}

RefPtr<DiskPartition> EBRPartitionTable::partition(unsigned index)
{
    ASSERT(index >= 1 && index <= m_ebr_chained_extensions_count + 4);

    auto& header = this->header();
    if (header.mbr_signature != MBR_SIGNATURE) {
        kprintf("EBRPartitionTable::initialize: bad MBR signature - not initalized? %#x\n", header.mbr_signature);
        return nullptr;
    }
    if (index_is_extended_partition(index))
        return get_extended_partition(index);
    if (index > 4)
        return get_non_extended_partition(index - m_ebr_chained_extensions_count);
    return get_non_extended_partition(index);
}
