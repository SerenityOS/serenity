#pragma once

#include <AK/StdLibExtras.h>
#include <LibCore/CFile.h>

class CFileStreamReader {
public:
    CFileStreamReader(CFile& file)
        : m_file(file)
    {
    }

    bool handle_read_failure()
    {
        return exchange(m_had_failure, false);
    }

    template<typename T>
    CFileStreamReader& operator>>(T& value)
    {
        int nread = m_file.read((u8*)&value, sizeof(T));
        ASSERT(nread == sizeof(T));
        if (nread != sizeof(T))
            m_had_failure = true;
        return *this;
    }

private:
    CFile& m_file;
    bool m_had_failure { false };
};
