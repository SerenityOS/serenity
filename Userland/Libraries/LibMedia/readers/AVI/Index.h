#pragma once

#include <AK/Vector.h>
#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>

namespace Media::Reader::AVI
{

// Use to find where a tracks sample is in the file (in 'movi')
struct IndexEntry
{
	u32 chunk_id;
	u32 flags;
	u32 chunk_offset;
	u32 chunk_length;
};

struct IndexTable : public RefCounted<IndexTable>
{
	public:
		IndexTable(u32 const offset,  ByteBuffer const& data);
		u32 m_offset;
		Vector<IndexEntry> m_entries;
};

}
