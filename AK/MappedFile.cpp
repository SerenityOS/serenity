#include "MappedFile.h"
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstdio>

namespace AK {

MappedFile::MappedFile(String&& file_name)
    : m_file_name(std::move(file_name))
{
    m_file_length = 1024;
    m_fd = open(m_file_name.characters(), O_RDONLY);
    
    if (m_fd != -1) {
        struct stat st;
        fstat(m_fd, &st);
        m_file_length = st.st_size;
        m_map = mmap(nullptr, m_file_length, PROT_READ, MAP_SHARED, m_fd, 0);

        if (m_map == MAP_FAILED)
            perror("");
    }

    printf("MappedFile{%s} := { m_fd=%d, m_file_length=%zu, m_map=%p }\n", m_file_name.characters(), m_fd, m_file_length, m_map);
}

MappedFile::~MappedFile()
{
    if (m_map != (void*)-1) {
        ASSERT(m_fd != -1);
        munmap(m_map, m_file_length);
    }
}

MappedFile::MappedFile(MappedFile&& other)
    : m_file_name(std::move(other.m_file_name))
    , m_file_length(other.m_file_length)
    , m_fd(other.m_fd)
    , m_map(other.m_map)
{
    other.m_file_length = 0;
    other.m_fd = -1;
    other.m_map = (void*)-1;
}

}

