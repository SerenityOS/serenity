#pragma once

#include <AK/AKString.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <LibCore/CFile.h>

class ABuffer;

namespace AK {
class ByteBuffer;
}

// Parses a WAV file and produces an ABuffer instance from it
class AWavLoader {
public:
    explicit AWavLoader(const StringView& path);

    bool has_error() const { return !m_error_string.is_null(); }
    const char* error_string() { return m_error_string.characters(); }

    RefPtr<ABuffer> get_more_samples(size_t max_bytes_to_read_from_input = 128 * KB);

    int loaded_samples() const { return m_loaded_samples; }
    int total_samples() const { return m_total_samples; }
    u32 sample_rate() const { return m_sample_rate; }
    u16 num_channels() const { return m_num_channels; }
    u16 bits_per_sample() const { return m_bits_per_sample; }

private:
    bool parse_header();
    CFile m_file;
    String m_error_string;

    u32 m_sample_rate { 0 };
    u16 m_num_channels { 0 };
    u16 m_bits_per_sample { 0 };

    int m_loaded_samples { 0 };
    int m_total_samples { 0 };
};
