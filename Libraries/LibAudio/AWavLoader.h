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
    RefPtr<ABuffer> load_wav(const StringView& path);
    const char* error_string() { return m_error_string.characters(); }

    RefPtr<ABuffer> get_more_samples();

private:
    bool parse_header();
    CFile m_file;
    String m_error_string;

    u32 m_sample_rate { 0 };
    u16 m_num_channels { 0 };
    u16 m_bits_per_sample { 0 };
};
