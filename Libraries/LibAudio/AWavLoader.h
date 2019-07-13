#pragma once

#include <AK/RefPtr.h>

class AWavFile;

class AWavLoader {
public:
    RefPtr<AWavFile> load_wav(const StringView& path);
    const char* error_string() { return m_error_string.characters(); }
private:
    RefPtr<AWavFile> parse_wav(const ByteBuffer& buffer);
    String m_error_string;
};
