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

#ifdef __serenity__

#include <AK/SharedBuffer.h>
#include <AK/kmalloc.h>
#include <Kernel/Syscall.h>
#include <stdio.h>
#include <serenity.h>

namespace AK {

RefPtr<SharedBuffer> SharedBuffer::create_with_size(int size)
{
    void* data;
    int shbuf_id = shbuf_create(size, &data);
    if (shbuf_id < 0) {
        perror("shbuf_create");
        return nullptr;
    }
    return adopt(*new SharedBuffer(shbuf_id, size, data));
}

bool SharedBuffer::share_with(pid_t peer)
{
    int ret = shbuf_allow_pid(shbuf_id(), peer);
    if (ret < 0) {
        perror("shbuf_allow_pid");
        return false;
    }
    return true;
}

bool SharedBuffer::share_globally()
{
    int ret = shbuf_allow_all(shbuf_id());
    if (ret < 0) {
        perror("shbuf_allow_all");
        return false;
    }
    return true;
}

RefPtr<SharedBuffer> SharedBuffer::create_from_shbuf_id(int shbuf_id)
{
    void* data = shbuf_get(shbuf_id);
    if (data == (void*)-1) {
        perror("shbuf_get");
        return nullptr;
    }
    int size = shbuf_get_size(shbuf_id);
    if (size < 0) {
        perror("shbuf_get_size");
        return nullptr;
    }
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
        int rc = shbuf_release(m_shbuf_id);
        if (rc < 0) {
            perror("shbuf_release");
        }
    }
}

void SharedBuffer::seal()
{
    int rc = shbuf_seal(m_shbuf_id);
    if (rc < 0) {
        perror("shbuf_seal");
        ASSERT_NOT_REACHED();
    }
}

void SharedBuffer::set_volatile()
{
    u32 rc = syscall(SC_shbuf_set_volatile, m_shbuf_id, true);
    ASSERT(rc == 0);
}

bool SharedBuffer::set_nonvolatile()
{
    u32 rc = syscall(SC_shbuf_set_volatile, m_shbuf_id, false);
    if (rc == 0)
        return true;
    if (rc == 1)
        return false;
    ASSERT_NOT_REACHED();
}

}

#endif
