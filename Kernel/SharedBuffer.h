#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PurgeableVMObject.h>

struct SharedBuffer {
private:
    struct Reference {
        Reference(pid_t pid)
            : pid(pid)
        {
        }

        pid_t pid;
        unsigned count { 0 };
        Region* region { nullptr };
    };

public:
    SharedBuffer(int id, int size)
        : m_shared_buffer_id(id)
        , m_vmobject(PurgeableVMObject::create_with_size(size))
    {
#ifdef SHARED_BUFFER_DEBUG
        dbgprintf("Created shared buffer %d of size %d\n", m_shared_buffer_id, size);
#endif
    }

    ~SharedBuffer()
    {
#ifdef SHARED_BUFFER_DEBUG
        dbgprintf("Destroyed shared buffer %d of size %d\n", m_shared_buffer_id, size());
#endif
    }

    void sanity_check(const char* what);
    bool is_shared_with(pid_t peer_pid);
    void* ref_for_process_and_get_address(Process& process);
    void share_with(pid_t peer_pid);
    void share_globally() { m_global = true; }
    void deref_for_process(Process& process);
    void disown(pid_t pid);
    size_t size() const { return m_vmobject->size(); }
    void destroy_if_unused();
    void seal();
    PurgeableVMObject& vmobject() { return m_vmobject; }
    const PurgeableVMObject& vmobject() const { return m_vmobject; }
    int id() const { return m_shared_buffer_id; }

    int m_shared_buffer_id { -1 };
    bool m_writable { true };
    bool m_global { false };
    NonnullRefPtr<PurgeableVMObject> m_vmobject;
    Vector<Reference, 2> m_refs;
    unsigned m_total_refs { 0 };
};

Lockable<HashMap<int, NonnullOwnPtr<SharedBuffer>>>& shared_buffers();
