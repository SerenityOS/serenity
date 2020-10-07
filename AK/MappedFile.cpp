/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/MappedFile.h>
#include <AK/String.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

//#define DEBUG_MAPPED_FILE

namespace AK {

MappedFile::MappedFile(const StringView& file_name)
{
    int fd = open_with_path_length(file_name.characters_without_null_termination(), file_name.length(), O_RDONLY | O_CLOEXEC, 0);

    if (fd == -1) {
        m_errno = errno;
        perror("open");
        return;
    }

    struct stat st;
    fstat(fd, &st);
    m_size = st.st_size;
    m_map = mmap(nullptr, m_size, PROT_READ, MAP_SHARED, fd, 0);

    if (m_map == MAP_FAILED) {
        m_errno = errno;
        perror("mmap");
    }

#ifdef DEBUG_MAPPED_FILE
    dbgln("MappedFile(\"{}\") := ( fd={}, m_size={}, m_map={} )", file_name, fd, m_size, m_map);
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
