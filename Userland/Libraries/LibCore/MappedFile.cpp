/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

namespace Core {

ErrorOr<NonnullOwnPtr<MappedFile>> MappedFile::map(StringView path)
{
    auto fd = TRY(Core::System::open(path, O_RDONLY | O_CLOEXEC, 0));
    return map_from_fd_and_close(fd, path);
}

ErrorOr<NonnullOwnPtr<MappedFile>> MappedFile::map_from_file(NonnullOwnPtr<Core::File> stream, StringView path)
{
    return map_from_fd_and_close(stream->leak_fd(Badge<MappedFile> {}), path);
}

ErrorOr<NonnullOwnPtr<MappedFile>> MappedFile::map_from_fd_and_close(int fd, [[maybe_unused]] StringView path)
{
    TRY(Core::System::fcntl(fd, F_SETFD, FD_CLOEXEC));

    ScopeGuard fd_close_guard = [fd] {
        close(fd);
    };

    auto stat = TRY(Core::System::fstat(fd));
    auto size = stat.st_size;

    auto* ptr = TRY(Core::System::mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0, 0, path));

    return adopt_own(*new MappedFile(ptr, size));
}

MappedFile::MappedFile(void* ptr, size_t size)
    : m_data(ptr)
    , m_size(size)
{
}

MappedFile::~MappedFile()
{
    auto res = Core::System::munmap(m_data, m_size);
    if (res.is_error())
        dbgln("Failed to unmap MappedFile (@ {:p}): {}", m_data, res.error());
}

}
