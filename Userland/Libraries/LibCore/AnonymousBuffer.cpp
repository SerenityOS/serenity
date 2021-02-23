/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/AnonymousBuffer.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <LibIPC/File.h>
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
    return adopt(*new AnonymousBufferImpl(fd, size, data));
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

AnonymousBuffer::~AnonymousBuffer()
{
}

}
