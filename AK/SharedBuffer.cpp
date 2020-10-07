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

#if defined(__serenity__) || defined(__linux__)

#    include <AK/SharedBuffer.h>
#    include <AK/kmalloc.h>
#    include <Kernel/API/Syscall.h>
#    include <stdio.h>

#    if defined(__serenity__)
#        include <serenity.h>
#    elif defined(__linux__)
#        include <AK/String.h>
#        include <fcntl.h>
#        include <sys/mman.h>

static String shbuf_shm_name(int shbuf_id)
{
    return String::formatted("/serenity-shm:{}", shbuf_id);
}

#    endif

namespace AK {

RefPtr<SharedBuffer> SharedBuffer::create_with_size(int size)
{
#    if defined(__serenity__)
    void* data;
    int shbuf_id = shbuf_create(size, &data);
    if (shbuf_id < 0) {
        perror("shbuf_create");
        return nullptr;
    }
#    elif defined(__linux__)
    // Not atomic, so don't create shared buffers from many threads too hard under lagom.
    static unsigned g_shm_id = 0;

    int shbuf_id = (getpid() << 8) | (g_shm_id++);
    int fd = shm_open(shbuf_shm_name(shbuf_id).characters(), O_RDWR | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR);
    if (fd < 0) {
        perror("shm_open");
        return nullptr;
    }

    if (ftruncate(fd, size) < 0) {
        perror("ftruncate");
        return nullptr;
    }

    void* data = mmap(nullptr, size + sizeof(size_t), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (data == MAP_FAILED) {
        perror("mmap");
        return nullptr;
    }
    size_t* size_data = reinterpret_cast<size_t*>(data);
    *size_data = size;
    data = reinterpret_cast<u8*>(data) + sizeof(size_t);

    if (close(fd) < 0) {
        perror("close");
        return nullptr;
    }
#    endif
    return adopt(*new SharedBuffer(shbuf_id, size, data));
}

bool SharedBuffer::share_with(pid_t peer)
{
#    if defined(__serenity__)
    int ret = shbuf_allow_pid(shbuf_id(), peer);
    if (ret < 0) {
        perror("shbuf_allow_pid");
        return false;
    }
#    else
    (void)peer;
#    endif
    return true;
}

bool SharedBuffer::share_globally()
{
#    if defined(__serenity__)
    int ret = shbuf_allow_all(shbuf_id());
    if (ret < 0) {
        perror("shbuf_allow_all");
        return false;
    }
#    endif
    return true;
}

RefPtr<SharedBuffer> SharedBuffer::create_from_shbuf_id(int shbuf_id)
{
#    if defined(__serenity__)
    size_t size = 0;
    void* data = shbuf_get(shbuf_id, &size);
    if (data == (void*)-1)
        return nullptr;
#    elif defined(__linux__)
    int fd = shm_open(shbuf_shm_name(shbuf_id).characters(), O_RDWR, S_IRUSR | S_IWUSR);
    if (fd < 0) {
        perror("shm_open");
        return nullptr;
    }

    void* data = mmap(nullptr, sizeof(size_t), PROT_READ, MAP_SHARED, fd, 0);
    if (data == MAP_FAILED) {
        perror("mmap");
        return nullptr;
    }
    size_t* size_data = reinterpret_cast<size_t*>(data);
    size_t size = *size_data;
    if (munmap(data, sizeof(size_t)) < 0) {
        perror("munmap");
        return nullptr;
    }

    data = mmap(nullptr, size + sizeof(size_t), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (data == MAP_FAILED) {
        perror("mmap");
        return nullptr;
    }

    data = reinterpret_cast<u8*>(data) + sizeof(size_t);

    if (close(fd) < 0) {
        perror("close");
        return nullptr;
    }
#    endif

    return adopt(*new SharedBuffer(shbuf_id, size, data));
}

SharedBuffer::SharedBuffer(int shbuf_id, int size, void* data)
    : m_shbuf_id(shbuf_id)
    , m_size(size)
    , m_data(data)
{
}

SharedBuffer::~SharedBuffer()
{
    if (m_shbuf_id >= 0) {
#    if defined(__serenity__)
        int rc = shbuf_release(m_shbuf_id);
        if (rc < 0) {
            perror("shbuf_release");
        }
#    elif defined(__linux__)
        if (munmap(reinterpret_cast<u8*>(m_data) - sizeof(size_t), m_size + sizeof(size_t)) < 0)
            perror("munmap");
        if (shm_unlink(shbuf_shm_name(m_shbuf_id).characters()) < 0)
            perror("unlink");
#    endif
    }
}

void SharedBuffer::seal()
{
#    if defined(__serenity__)
    int rc = shbuf_seal(m_shbuf_id);
    if (rc < 0) {
        perror("shbuf_seal");
        ASSERT_NOT_REACHED();
    }
#    endif
}

void SharedBuffer::set_volatile()
{
#    if defined(__serenity__)
    u32 rc = syscall(SC_shbuf_set_volatile, m_shbuf_id, true);
    ASSERT(rc == 0);
#    endif
}

bool SharedBuffer::set_nonvolatile()
{
#    if defined(__serenity__)
    u32 rc = syscall(SC_shbuf_set_volatile, m_shbuf_id, false);
    if (rc == 0)
        return true;
    ASSERT(rc == 1);
#    endif
    return false;
}

}

#endif
