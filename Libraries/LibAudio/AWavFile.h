#pragma once

#include <AK/RefCounted.h>
#include <AK/ByteBuffer.h>
#include <AK/Types.h>

class AWavFile : public RefCounted<AWavFile> {
public:
    enum class Format {
        Invalid,
        PCM,
    };

    Format format() const { return m_format; }
    u16 channel_count() const { return m_channel_count; }
    u32 sample_rate_per_second() const { return m_sample_rate; }
    u32 average_byte_rate_per_second() const { return m_byte_rate; }
    u16 block_align() const { return m_block_align; }
    u16 bits_per_sample() const { return m_bits_per_sample; }
    const ByteBuffer& sample_data() const { return m_sample_data; }

private:
    Format m_format = Format::Invalid;
    u16 m_channel_count = 0;
    u32 m_sample_rate = 0;
    u32 m_byte_rate = 0;
    u16 m_block_align = 0;
    u16 m_bits_per_sample = 0;
    ByteBuffer m_sample_data;

    friend class AWavLoader;
};
