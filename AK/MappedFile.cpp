#include <AK/AKString.h>
#include <AK/MappedFile.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

//#define DEBUG_MAPPED_FILE

namespace AK {

MappedFile::MappedFile(const StringView& file_name)
{
    m_size = PAGE_SIZE;
    m_fd = open_with_path_length(file_name.characters_without_null_termination(), file_name.length(), O_RDONLY | O_CLOEXEC, 0);

    if (m_fd != -1) {
        struct stat st;
        fstat(m_fd, &st);
        m_size = st.st_size;
        m_map = mmap(nullptr, m_size, PROT_READ, MAP_SHARED, m_fd, 0);

        if (m_map == MAP_FAILED)
            perror("");
    }

#ifdef DEBUG_MAPPED_FILE
    dbgprintf("MappedFile{%s} := { m_fd=%d, m_size=%u, m_map=%p }\n", file_name.characters(), m_fd, m_size, m_map);
#endif
}

MappedFile::~MappedFile()
{
    unmap();
}

void MappedFile::unmap()
{
    if (!is_valid())
        return;
    ASSERT(m_fd != -1);
    int rc = munmap(m_map, m_size);
    ASSERT(rc == 0);
    rc = close(m_fd);
    ASSERT(rc == 0);
    m_size = 0;
    m_fd = -1;
    m_map = (void*)-1;
}

MappedFile::MappedFile(MappedFile&& other)
    : m_size(other.m_size)
    , m_fd(other.m_fd)
    , m_map(other.m_map)
{
    other.m_size = 0;
    other.m_fd = -1;
    other.m_map = (void*)-1;
}

MappedFile& MappedFile::operator=(MappedFile&& other)
{
    if (this == &other)
        return *this;
    unmap();
    swap(m_size, other.m_size);
    swap(m_fd, other.m_fd);
    swap(m_map, other.m_map);
    return *this;
}

}
