#pragma once

#include <AK/AKString.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>

class ABuffer;

namespace AK {
class ByteBuffer;
}

// Parses a WAV file and produces an ABuffer instance from it
class AWavLoader {
public:
    RefPtr<ABuffer> load_wav(const StringView& path);
    const char* error_string() { return m_error_string.characters(); }

private:
    RefPtr<ABuffer> parse_wav(ByteBuffer&);
    String m_error_string;
};
