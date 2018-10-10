#pragma once

#include "String.h"

namespace AK {

class MappedFile {
public:
    MappedFile() { }
    explicit MappedFile(String&& fileName);
    MappedFile(MappedFile&&);
    ~MappedFile();

    bool isValid() const { return m_map != (void*)-1; }

    void* pointer() { return m_map; }
    const void* pointer() const { return m_map; }
    size_t fileLength() const { return m_fileLength; }

private:
    String m_fileName;
    size_t m_fileLength { 0 };
    int m_fd { -1 };
    void* m_map { (void*)-1 };
};

}

using AK::MappedFile;

