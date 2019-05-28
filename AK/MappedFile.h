#pragma once

#include "AKString.h"

namespace AK {

class MappedFile {
public:
    MappedFile() {}
    explicit MappedFile(const String& file_name);
    MappedFile(MappedFile&&);
    ~MappedFile();

    MappedFile& operator=(MappedFile&&);

    bool is_valid() const { return m_map != (void*)-1; }
    void unmap();

    void* pointer() { return m_map; }
    const void* pointer() const { return m_map; }
    size_t size() const { return m_size; }

private:
    String m_file_name;
    size_t m_size { 0 };
    int m_fd { -1 };
    void* m_map { (void*)-1 };
};

}

using AK::MappedFile;
