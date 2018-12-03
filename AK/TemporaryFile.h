#pragma once

#include "AKString.h"
#include <stdio.h>

namespace AK {

class TemporaryFile {
public:
    TemporaryFile();
    ~TemporaryFile();

    bool isValid() const { return m_stream; }
    FILE* stream() { return m_stream; }
    String fileName() const { return m_fileName; }
    void sync();

private:
    FILE* m_stream { nullptr };
    String m_fileName;
};

}

using AK::TemporaryFile;

