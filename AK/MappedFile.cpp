/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
#include <AK/ScopeGuard.h>
#include <AK/String.h>
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

    return adopt(*new MappedFile(ptr, size));
}

MappedFile::MappedFile(void* ptr, size_t size)
    : m_data(ptr)
    , m_size(size)
{
}

MappedFile::~MappedFile()
{
    auto rc = munmap(m_data, m_size);
    ASSERT(rc == 0);
}

}
