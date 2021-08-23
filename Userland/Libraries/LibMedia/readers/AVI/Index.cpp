
#include <LibMedia/Manip.h>
#include <LibMedia/readers/AVI/Index.h>

namespace Media::Reader::AVI {

IndexEntry data_to_index_entry(ByteBuffer const& data);

IndexTable::IndexTable(u32 const offset, ByteBuffer const& data)
    : m_offset(offset)
{
    u32 data_offset = 0;
    while (data_offset < data.size()) {
        m_entries.append(data_to_index_entry(data.slice(data_offset, 16)));
        data_offset += 16;
    }
}

IndexEntry data_to_index_entry(ByteBuffer const& data)
{
    VERIFY(data.size() >= 16);
    IndexEntry entry;
    entry.chunk_id = bytes_to_u32le(data.slice(0, 4));
    entry.flags = bytes_to_u32le(data.slice(4, 4));
    entry.chunk_offset = bytes_to_u32le(data.slice(8, 4));
    entry.chunk_length = bytes_to_u32le(data.slice(12, 4));
    return entry;
}

}
