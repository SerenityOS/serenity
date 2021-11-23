/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

namespace Core {

ErrorOr<NonnullRefPtr<MappedFile>> MappedFile::map(String const& path)
{
    auto fd = TRY(Core::System::open(path, O_RDONLY | O_CLOEXEC, 0));
    return map_from_fd_and_close(fd, path);
}

ErrorOr<NonnullRefPtr<MappedFile>> MappedFile::map_from_fd_and_close(int fd, [[maybe_unused]] String const& path)
{
    TRY(Core::System::fcntl(fd, F_SETFD, FD_CLOEXEC));

    ScopeGuard fd_close_guard = [fd] {
        close(fd);
    };

    auto stat = TRY(Core::System::fstat(fd));
    auto size = stat.st_size;

    auto* ptr = TRY(Core::System::mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0, 0, path));

    return adopt_ref(*new MappedFile(ptr, size));
}

MappedFile::MappedFile(void* ptr, size_t size)
    : m_data(ptr)
    , m_size(size)
{
}

MappedFile::~MappedFile()
{
    MUST(Core::System::munmap(m_data, m_size));
}

}
