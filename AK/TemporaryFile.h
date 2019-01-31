#pragma once

#include "AKString.h"
#include <stdio.h>

namespace AK {

class TemporaryFile {
public:
    TemporaryFile();
    ~TemporaryFile();

    bool is_valid() const { return m_stream; }
    FILE* stream() { return m_stream; }
    String file_name() const { return m_file_name; }
    void sync();

private:
    FILE* m_stream { nullptr };
    String m_file_name;
};

}

using AK::TemporaryFile;

