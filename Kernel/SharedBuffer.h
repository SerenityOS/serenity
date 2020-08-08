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

#pragma once

#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PurgeableVMObject.h>

namespace Kernel {

class SharedBuffer {
private:
    struct Reference {
        Reference(ProcessID pid)
            : pid(pid)
        {
        }

        ProcessID pid;
        unsigned count { 0 };
        WeakPtr<Region> region;
    };

public:
    SharedBuffer(int id, int size)
        : m_shbuf_id(id)
        , m_vmobject(PurgeableVMObject::create_with_size(size))
    {
#ifdef SHARED_BUFFER_DEBUG
        dbg() << "Created shared buffer " << m_shbuf_id << " of size " << size;
#endif
    }

    ~SharedBuffer()
    {
#ifdef SHARED_BUFFER_DEBUG
        dbg() << "Destroyed shared buffer " << m_shbuf_id << " of size " << size();
#endif
    }

    void sanity_check(const char* what);
    bool is_shared_with(ProcessID peer_pid) const;
    void* ref_for_process_and_get_address(Process& process);
    void share_with(ProcessID peer_pid);
    void share_globally() { m_global = true; }
    void deref_for_process(Process& process);
    void disown(ProcessID pid);
    size_t size() const { return m_vmobject->size(); }
    void destroy_if_unused();
    void seal();
    PurgeableVMObject& vmobject() { return m_vmobject; }
    const PurgeableVMObject& vmobject() const { return m_vmobject; }
    int id() const { return m_shbuf_id; }

private:
    int m_shbuf_id { -1 };
    bool m_writable { true };
    bool m_global { false };
    NonnullRefPtr<PurgeableVMObject> m_vmobject;
    Vector<Reference, 2> m_refs;
    unsigned m_total_refs { 0 };
};

Lockable<HashMap<int, NonnullOwnPtr<SharedBuffer>>>& shared_buffers();

}
