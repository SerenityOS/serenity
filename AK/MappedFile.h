#pragma once

#include <AK/Noncopyable.h>
#include <AK/StringView.h>

namespace AK {

class MappedFile {
    AK_MAKE_NONCOPYABLE(MappedFile);
public:
    MappedFile() {}
    explicit MappedFile(const StringView& file_name);
    MappedFile(MappedFile&&);
    ~MappedFile();

    MappedFile& operator=(MappedFile&&);

    bool is_valid() const { return m_map != (void*)-1; }
    void unmap();

    void* data() { return m_map; }
    const void* data() const { return m_map; }
    size_t size() const { return m_size; }

private:
    size_t m_size { 0 };
    void* m_map { (void*)-1 };
};

}

using AK::MappedFile;
