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

#include <Kernel/Process.h>
#include <Kernel/SharedBuffer.h>

//#define SHARED_BUFFER_DEBUG

namespace Kernel {

void Process::disown_all_shared_buffers()
{
    LOCKER(shared_buffers().lock());
    Vector<SharedBuffer*, 32> buffers_to_disown;
    for (auto& it : shared_buffers().resource())
        buffers_to_disown.append(it.value.ptr());
    for (auto* shared_buffer : buffers_to_disown) {
        if (shared_buffer->disown(m_pid)) {
            shared_buffers().resource().remove(shared_buffer->id());
            delete shared_buffer;
        }
    }
}

int Process::sys$shbuf_create(int size, void** buffer)
{
    REQUIRE_PROMISE(shared_buffer);
    if (!size || size < 0)
        return -EINVAL;
    size = PAGE_ROUND_UP(size);

    auto vmobject = AnonymousVMObject::create_with_size(size, AllocationStrategy::Reserve);
    if (!vmobject)
        return -ENOMEM;

    LOCKER(shared_buffers().lock());
    static int s_next_shbuf_id;
    int shbuf_id = ++s_next_shbuf_id;
    auto shared_buffer = make<SharedBuffer>(shbuf_id, vmobject.release_nonnull());
    shared_buffer->share_with(m_pid);

    void* address = shared_buffer->ref_for_process_and_get_address(*this);
    if (!copy_to_user(buffer, &address))
        return -EFAULT;
    ASSERT((int)shared_buffer->size() >= size);
#ifdef SHARED_BUFFER_DEBUG
    klog() << "Created shared buffer " << shbuf_id << " @ " << buffer << " (" << size << " bytes, vmobject is " << shared_buffer->size() << ")";
#endif
    shared_buffers().resource().set(shbuf_id, move(shared_buffer));

    return shbuf_id;
}

int Process::sys$shbuf_allow_pid(int shbuf_id, pid_t peer_pid)
{
    REQUIRE_PROMISE(shared_buffer);
    if (!peer_pid || peer_pid < 0 || ProcessID(peer_pid) == m_pid)
        return -EINVAL;
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shbuf_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    {
        ScopedSpinLock lock(g_processes_lock);
        auto peer = Process::from_pid(peer_pid);
        if (!peer)
            return -ESRCH;
    }
    shared_buffer.share_with(peer_pid);
    return 0;
}

int Process::sys$shbuf_allow_all(int shbuf_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shbuf_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    shared_buffer.share_globally();
    return 0;
}

int Process::sys$shbuf_release(int shbuf_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shbuf_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    klog() << "Releasing shared buffer " << shbuf_id << ", buffer count: " << shared_buffers().resource().size();
#endif
    shared_buffer.deref_for_process(*this);
    return 0;
}

void* Process::sys$shbuf_get(int shbuf_id, Userspace<size_t*> user_size)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shbuf_id);
    if (it == shared_buffers().resource().end())
        return (void*)-EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return (void*)-EPERM;
#ifdef SHARED_BUFFER_DEBUG
    klog() << "Retaining shared buffer " << shbuf_id << ", buffer count: " << shared_buffers().resource().size();
#endif
    if (user_size) {
        size_t size = shared_buffer.size();
        if (!copy_to_user(user_size, &size))
            return (void*)-EFAULT;
    }
    return shared_buffer.ref_for_process_and_get_address(*this);
}

int Process::sys$shbuf_seal(int shbuf_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shbuf_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    klog() << "Sealing shared buffer " << shbuf_id;
#endif
    shared_buffer.seal();
    return 0;
}

int Process::sys$shbuf_set_volatile(int shbuf_id, bool state)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(shbuf_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
#ifdef SHARED_BUFFER_DEBUG
    klog() << "Set shared buffer " << shbuf_id << " volatile: " << state;
#endif

    bool was_purged = false;
    auto set_volatile = [&]() -> int {
        switch (shared_buffer.set_volatile_all(state, was_purged)) {
        case SharedBuffer::SetVolatileError::Success:
            break;
        case SharedBuffer::SetVolatileError::NotPurgeable:
            return -EPERM;
        case SharedBuffer::SetVolatileError::OutOfMemory:
            return -ENOMEM;
        case SharedBuffer::SetVolatileError::NotMapped:
            return -EINVAL;
        }
        return 0;
    };

    if (!state) {
        if (int err = set_volatile())
            return err;
        return was_purged ? 1 : 0;
    }
    return set_volatile();
}

}
