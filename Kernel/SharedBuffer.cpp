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

#include <AK/Singleton.h>
#include <Kernel/Process.h>
#include <Kernel/SharedBuffer.h>

//#define SHARED_BUFFER_DEBUG

namespace Kernel {

static AK::Singleton<Lockable<HashMap<int, NonnullOwnPtr<SharedBuffer>>>> s_map;

Lockable<HashMap<int, NonnullOwnPtr<SharedBuffer>>>& shared_buffers()
{
    return *s_map;
}

void SharedBuffer::sanity_check(const char* what)
{
    LOCKER(shared_buffers().lock(), Lock::Mode::Shared);

    unsigned found_refs = 0;
    for (const auto& ref : m_refs)
        found_refs += ref.count;

    if (found_refs != m_total_refs) {
        dbgln("{} sanity -- SharedBuffer({}) id: {} has total refs {} but we found {}",
            what,
            this,
            m_shbuf_id,
            m_total_refs,
            found_refs);

        for (const auto& ref : m_refs)
            dbgln("    ref from pid {}: reference count {}", ref.pid.value(), ref.count);
        ASSERT_NOT_REACHED();
    }
}

bool SharedBuffer::is_shared_with(ProcessID peer_pid) const
{
    LOCKER(shared_buffers().lock(), Lock::Mode::Shared);
    if (m_global)
        return true;
    for (auto& ref : m_refs) {
        if (ref.pid == peer_pid) {
            return true;
        }
    }

    return false;
}

void* SharedBuffer::ref_for_process_and_get_address(Process& process)
{
    LOCKER(shared_buffers().lock());
    ASSERT(is_shared_with(process.pid()));
    if (m_global) {
        bool found = false;
        for (auto& ref : m_refs) {
            if (ref.pid == process.pid()) {
                found = true;
                break;
            }
        }
        if (!found)
            m_refs.append(Reference(process.pid()));
    }

    for (auto& ref : m_refs) {
        if (ref.pid == process.pid()) {
            if (!ref.region) {
                auto* region = process.allocate_region_with_vmobject(VirtualAddress(), size(), m_vmobject, 0, "SharedBuffer", PROT_READ | (m_writable ? PROT_WRITE : 0), true);
                if (!region)
                    return (void*)-ENOMEM;
                ref.region = region;
            }
            ref.count++;
            m_total_refs++;
            sanity_check("ref_for_process_and_get_address");
            return ref.region.unsafe_ptr()->vaddr().as_ptr(); // TODO: Region needs to be RefCounted!
        }
    }
    ASSERT_NOT_REACHED();
}

void SharedBuffer::share_with(ProcessID peer_pid)
{
    LOCKER(shared_buffers().lock());
    if (m_global)
        return;
    for (auto& ref : m_refs) {
        if (ref.pid == peer_pid) {
            // don't increment the reference count yet; let them shbuf_get it first.
            sanity_check("share_with (old ref)");
            return;
        }
    }

    m_refs.append(Reference(peer_pid));
    sanity_check("share_with (new ref)");
}

void SharedBuffer::share_all_shared_buffers(Process& from_process, Process& with_process)
{
    LOCKER(shared_buffers().lock());
    for (auto& shbuf : shared_buffers().resource()) {
        auto& shared_buffer = *shbuf.value;
        // We need to clone all references (including for global shared buffers),
        // and the reference counts as well.
        for (auto& ref : shared_buffer.m_refs) {
            if (ref.pid == from_process.pid()) {
                auto ref_count = ref.count;
                shared_buffer.m_refs.append(Reference(with_process.pid(), ref_count));
                // NOTE: ref may become invalid after we appended!
                shared_buffer.m_total_refs += ref_count;
                break;
            }
        }
    }
}

void SharedBuffer::deref_for_process(Process& process)
{
    LOCKER(shared_buffers().lock());
    for (size_t i = 0; i < m_refs.size(); ++i) {
        auto& ref = m_refs[i];
        if (ref.pid == process.pid()) {
            ASSERT(ref.count > 0);
            ref.count--;
            ASSERT(m_total_refs > 0);
            m_total_refs--;
            if (ref.count == 0) {
#ifdef SHARED_BUFFER_DEBUG
                dbg() << "Releasing shared buffer reference on " << m_shbuf_id << " of size " << size() << " by PID " << process.pid().value();
#endif
                process.deallocate_region(*ref.region.unsafe_ptr()); // TODO: Region needs to be RefCounted!
#ifdef SHARED_BUFFER_DEBUG
                dbg() << "Released shared buffer reference on " << m_shbuf_id << " of size " << size() << " by PID " << process.pid().value();
#endif
                sanity_check("deref_for_process");
                destroy_if_unused();
                return;
            }
            return;
        }
    }

    ASSERT_NOT_REACHED();
}

bool SharedBuffer::disown(ProcessID pid)
{
    LOCKER(shared_buffers().lock());
    for (size_t i = 0; i < m_refs.size(); ++i) {
        auto& ref = m_refs[i];
        if (ref.pid == pid) {
#ifdef SHARED_BUFFER_DEBUG
            dbg() << "Disowning shared buffer " << m_shbuf_id << " of size " << size() << " by PID " << pid.value();
#endif
            ASSERT(m_total_refs >= ref.count);
            m_total_refs -= ref.count;
            m_refs.unstable_take(i);
#ifdef SHARED_BUFFER_DEBUG
            dbg() << "Disowned shared buffer " << m_shbuf_id << " of size " << size() << " by PID " << pid.value();
#endif
            destroy_if_unused();
            break;
        }
    }

    return m_total_refs == 0;
}

void SharedBuffer::destroy_if_unused()
{
    LOCKER(shared_buffers().lock());
    sanity_check("destroy_if_unused");
    if (m_total_refs == 0) {
#ifdef SHARED_BUFFER_DEBUG
        dbg() << "Destroying unused SharedBuffer{" << this << "} id: " << m_shbuf_id;
#endif
        auto count_before = shared_buffers().resource().size();
        shared_buffers().resource().remove(m_shbuf_id);
        ASSERT(count_before != shared_buffers().resource().size());
    }
}

void SharedBuffer::seal()
{
    LOCKER(shared_buffers().lock());
    m_writable = false;
    for (auto& ref : m_refs) {
        // TODO: Region needs to be RefCounted!
        if (auto* region = ref.region.unsafe_ptr()) {
            region->set_writable(false);
            region->remap();
        }
    }
}

auto SharedBuffer::set_volatile_all(bool is_volatile, bool& was_purged) -> SetVolatileError
{
    was_purged = false;
    auto pid = Process::current()->pid();
    LOCKER(shared_buffers().lock());
    for (size_t i = 0; i < m_refs.size(); ++i) {
        auto& ref = m_refs[i];
        if (ref.pid == pid) {
            if (Region* region = ref.region.unsafe_ptr()) {
                switch (region->set_volatile(region->vaddr(), region->size(), is_volatile, was_purged)) {
                case Region::SetVolatileError::Success:
                    return SetVolatileError::Success;
                case Region::SetVolatileError::NotPurgeable:
                    return SetVolatileError::NotPurgeable;
                case Region::SetVolatileError::OutOfMemory:
                    return SetVolatileError::OutOfMemory;
                }
            }
        }
    }
    return SetVolatileError::NotMapped;
}

}
