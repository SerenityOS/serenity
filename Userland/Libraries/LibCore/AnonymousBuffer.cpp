/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/AnonymousBuffer.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>

#if defined(__serenity__)
#    include <serenity.h>
#endif

#if defined(__linux__) && !defined(MFD_CLOEXEC)
#    include <linux/memfd.h>
#    include <sys/syscall.h>

static int memfd_create(const char* name, unsigned int flags)
{
    return syscall(SYS_memfd_create, name, flags);
}
#endif

namespace Core {

AnonymousBuffer AnonymousBuffer::create_with_size(size_t size)
{
    int fd = -1;
#if defined(__serenity__)
    fd = anon_create(round_up_to_power_of_two(size, PAGE_SIZE), O_CLOEXEC);
    if (fd < 0) {
        perror("anon_create");
        return {};
    }
#elif defined(__linux__)
    fd = memfd_create("", MFD_CLOEXEC);
    if (fd < 0) {
        perror("memfd_create");
        return {};
    }
    if (ftruncate(fd, size) < 0) {
        close(fd);
        perror("ftruncate");
        return {};
    }
#endif
    if (fd < 0)
        return {};
    return create_from_anon_fd(fd, size);
}

RefPtr<AnonymousBufferImpl> AnonymousBufferImpl::create(int fd, size_t size)
{
    auto* data = mmap(nullptr, round_up_to_power_of_two(size, PAGE_SIZE), PROT_READ | PROT_WRITE, MAP_FILE | MAP_SHARED, fd, 0);
    if (data == MAP_FAILED) {
        perror("mmap");
        return {};
    }
    return adopt_ref(*new AnonymousBufferImpl(fd, size, data));
}

AnonymousBufferImpl::~AnonymousBufferImpl()
{
    if (m_fd != -1) {
        auto rc = close(m_fd);
        VERIFY(rc == 0);
    }
    auto rc = munmap(m_data, round_up_to_power_of_two(m_size, PAGE_SIZE));
    VERIFY(rc == 0);
}

AnonymousBuffer AnonymousBuffer::create_from_anon_fd(int fd, size_t size)
{
    auto impl = AnonymousBufferImpl::create(fd, size);
    if (!impl)
        return {};
    return AnonymousBuffer(impl.release_nonnull());
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
