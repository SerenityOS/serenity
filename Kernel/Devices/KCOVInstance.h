/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/SpinLock.h>
#include <Kernel/VM/AnonymousVMObject.h>

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

    KResult buffer_allocate(size_t buffer_size_in_entries);
    bool has_buffer() const { return m_buffer != nullptr; }
    void buffer_add_pc(u64 pc);

    SpinLock<u8> lock;
    enum {
        UNUSED = 0,
        OPENED = 1,
        TRACING = 2,
    } state;

    RefPtr<AnonymousVMObject> vmobject;

private:
    ProcessID m_pid = { 0 };
    u64 m_buffer_size_in_entries = { 0 };
    size_t m_buffer_size_in_bytes = { 0 };
    kcov_pc_t* m_buffer = { nullptr };

    // Here to ensure it's not garbage collected at the end of open()
    OwnPtr<Region> m_kernel_region;
};

}
