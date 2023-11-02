/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibCore/System.h>
#include <LibIPC/File.h>
#include <fcntl.h>

#if !defined(AK_OS_WINDOWS)
#    include <sys/mman.h>
#endif

namespace Core {

ErrorOr<AnonymousBuffer> AnonymousBuffer::create_with_size(size_t size)
{
#if !defined(AK_OS_WINDOWS)
    auto fd = TRY(Core::System::anon_create(size, O_CLOEXEC));
    return create_from_anon_fd(fd, size);
#else
    dbgln("FIXME: Implement Core::AnonymousBuffer::create_with_size({}) on Windows", size);
    VERIFY_NOT_REACHED();
#endif
}

ErrorOr<NonnullRefPtr<AnonymousBufferImpl>> AnonymousBufferImpl::create(int fd, size_t size)
{
#if !defined(AK_OS_WINDOWS)
    auto* data = mmap(nullptr, round_up_to_power_of_two(size, PAGE_SIZE), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (data == MAP_FAILED)
        return Error::from_errno(errno);
    return AK::adopt_nonnull_ref_or_enomem(new (nothrow) AnonymousBufferImpl(fd, size, data));
#else
    auto* mapping = CreateFileMappingW(INVALID_HANDLE_VALUE, nullptr, PAGE_READWRITE | SEC_COMMIT, 0, size, nullptr);
    if (!mapping)
        return Error::from_windows_error(GetLastError());
    auto* data = MapViewOfFile(mapping, FILE_MAP_ALL_ACCESS, 0, 0, size);
    if (!data) {
        auto rc = CloseHandle(mapping);
        VERIFY(rc);
        return Error::from_windows_error(GetLastError());
    }

    return AK::adopt_nonnull_ref_or_enomem(new (nothrow) AnonymousBufferImpl(fd, size, data));
#endif
}

AnonymousBufferImpl::~AnonymousBufferImpl()
{
#if !defined(AK_OS_WINDOWS)
    if (m_fd != -1) {
        auto rc = close(m_fd);
        VERIFY(rc == 0);
    }
    auto rc = munmap(m_data, round_up_to_power_of_two(m_size, PAGE_SIZE));
    VERIFY(rc == 0);
#else
    dbgln("FIXME: Implement AnonymousBufferImpl::~AnonymousBufferImpl() on Windows");
    VERIFY_NOT_REACHED();
#endif
}

ErrorOr<AnonymousBuffer> AnonymousBuffer::create_from_anon_fd(int fd, size_t size)
{
    auto impl = TRY(AnonymousBufferImpl::create(fd, size));
    return AnonymousBuffer(move(impl));
}

AnonymousBuffer::AnonymousBuffer(NonnullRefPtr<AnonymousBufferImpl> impl)
    : m_impl(move(impl))
{
}

AnonymousBufferImpl::AnonymousBufferImpl(int fd, size_t size, void* data)
    : m_fd(fd)
    , m_size(size)
    , m_data(data)
{
}

}
