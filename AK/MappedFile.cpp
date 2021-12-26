/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MappedFile.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

namespace AK {

Result<NonnullRefPtr<MappedFile>, OSError> MappedFile::map(const String& path)
{
    int fd = open(path.characters(), O_RDONLY | O_CLOEXEC, 0);
    if (fd < 0)
        return OSError(errno);

    ScopeGuard fd_close_guard = [fd] {
        close(fd);
    };

    struct stat st;
    if (fstat(fd, &st) < 0) {
        auto saved_errno = errno;
        return OSError(saved_errno);
    }

    auto size = st.st_size;
    auto* ptr = mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0);

    if (ptr == MAP_FAILED)
        return OSError(errno);

    return adopt_ref(*new MappedFile(ptr, size));
}

MappedFile::MappedFile(void* ptr, size_t size)
    : m_data(ptr)
    , m_size(size)
{
}

MappedFile::~MappedFile()
{
    auto rc = munmap(m_data, m_size);
    VERIFY(rc == 0);
}

}
