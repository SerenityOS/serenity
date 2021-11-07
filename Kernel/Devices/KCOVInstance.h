/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel {

// Note: These need to be kept in sync with Userland/Libraries/LibC/sys/kcov.h
typedef volatile u64 kcov_pc_t;
#define KCOV_ENTRY_SIZE sizeof(kcov_pc_t)
#define KCOV_MAX_ENTRIES (10 * 1024 * 1024)

/*
 * One KCOVInstance is allocated per process, when the process opens /dev/kcov
 * for the first time. At this point it is in state OPENED. When a thread in
 * the same process then uses the KCOV_ENABLE ioctl on the block device, the
 * instance enters state TRACING.
 * 
 * A KCOVInstance in state TRACING can return to state OPENED by either the
 * KCOV_DISABLE ioctl or by killing the thread. A KCOVInstance in state OPENED
 * can return to state UNUSED only when the process dies. At this point
 * KCOVDevice::free_process will delete the KCOVInstance.
 */
class KCOVInstance final {
public:
    explicit KCOVInstance(ProcessID pid);

    ErrorOr<void> buffer_allocate(size_t buffer_size_in_entries);
    bool has_buffer() const { return m_buffer != nullptr; }
    void buffer_add_pc(u64 pc);

    enum State {
        UNUSED = 0,
        OPENED = 1,
        TRACING = 2,
    } m_state { UNUSED };

    State state() const { return m_state; }
    void set_state(State state) { m_state = state; }

    Memory::VMObject* vmobject() { return m_vmobject; }

    Spinlock& spinlock() { return m_lock; }

private:
    ProcessID m_pid { 0 };
    u64 m_buffer_size_in_entries { 0 };
    size_t m_buffer_size_in_bytes { 0 };
    kcov_pc_t* m_buffer { nullptr };
    RefPtr<Memory::AnonymousVMObject> m_vmobject;

    // Here to ensure it's not garbage collected at the end of open()
    OwnPtr<Memory::Region> m_kernel_region;

    Spinlock m_lock;
};

}
