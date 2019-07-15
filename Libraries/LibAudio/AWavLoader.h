#pragma once

#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/AKString.h>

class ABuffer;
class ByteBuffer;

// Parses a WAV file and produces an ABuffer instance from it
class AWavLoader {
public:
    RefPtr<ABuffer> load_wav(const StringView& path);
    const char* error_string() { return m_error_string.characters(); }
private:
    RefPtr<ABuffer> parse_wav(ByteBuffer& buffer);
    String m_error_string;
};
