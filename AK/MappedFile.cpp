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
    int fd = open_with_path_length(file_name.characters_without_null_termination(), file_name.length(), O_RDONLY | O_CLOEXEC, 0);

    if (fd == -1) {
        perror("open");
        return;
    }

    struct stat st;
    fstat(fd, &st);
    m_size = st.st_size;
    m_map = mmap(nullptr, m_size, PROT_READ, MAP_SHARED, fd, 0);

    if (m_map == MAP_FAILED)
        perror("mmap");

#ifdef DEBUG_MAPPED_FILE
    dbgprintf("MappedFile{%s} := { fd=%d, m_size=%u, m_map=%p }\n", file_name.characters(), fd, m_size, m_map);
#endif

    close(fd);
}

MappedFile::~MappedFile()
{
    unmap();
}

void MappedFile::unmap()
{
    if (!is_valid())
        return;
    int rc = munmap(m_map, m_size);
    ASSERT(rc == 0);
    m_size = 0;
    m_map = (void*)-1;
}

MappedFile::MappedFile(MappedFile&& other)
    : m_size(other.m_size)
    , m_map(other.m_map)
{
    other.m_size = 0;
    other.m_map = (void*)-1;
}

MappedFile& MappedFile::operator=(MappedFile&& other)
{
    if (this == &other)
        return *this;
    unmap();
    swap(m_size, other.m_size);
    swap(m_map, other.m_map);
    return *this;
}

}
