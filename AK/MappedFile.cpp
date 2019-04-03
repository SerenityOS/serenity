#include <AK/MappedFile.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>

namespace AK {

MappedFile::MappedFile(const String& file_name)
    : m_file_name(file_name)
{
    m_file_length = PAGE_SIZE;
    m_fd = open(m_file_name.characters(), O_RDONLY);
    
    if (m_fd != -1) {
        struct stat st;
        fstat(m_fd, &st);
        m_file_length = st.st_size;
        m_map = mmap(nullptr, m_file_length, PROT_READ, MAP_SHARED, m_fd, 0);

        if (m_map == MAP_FAILED)
            perror("");
    }

    dbgprintf("MappedFile{%s} := { m_fd=%d, m_file_length=%zu, m_map=%p }\n", m_file_name.characters(), m_fd, m_file_length, m_map);
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
    int rc = munmap(m_map, m_file_length);
    ASSERT(rc == 0);
    m_file_name = { };
    m_file_length = 0;
    m_fd = -1;
    m_map = (void*)-1;
}

MappedFile::MappedFile(MappedFile&& other)
    : m_file_name(move(other.m_file_name))
    , m_file_length(other.m_file_length)
    , m_fd(other.m_fd)
    , m_map(other.m_map)
{
    other.m_file_length = 0;
    other.m_fd = -1;
    other.m_map = (void*)-1;
}

MappedFile& MappedFile::operator=(MappedFile&& other)
{
    if (this == &other)
        return *this;
    unmap();
    swap(m_file_name, other.m_file_name);
    swap(m_file_length, other.m_file_length);
    swap(m_fd, other.m_fd);
    swap(m_map, other.m_map);
    return *this;
}

}

