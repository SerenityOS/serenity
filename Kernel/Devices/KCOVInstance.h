/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/kcov.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel {

#define KCOV_MAX_ENTRIES (10 * 1024 * 1024)

/*
 * 1. When a thread opens /dev/kcov for the first time, a KCOVInstance is
 * allocated and tracked via an OwnPtr on the Kernel::Process object.
 * 2. When a thread in the same process then uses the KCOV_SETBUFSIZE ioctl
 * on the block device, a Memory::Region is allocated and tracked via an
 * OwnPtr on the KCOVInstance.
 * 3. When a thread in the same process then uses the KCOV_ENABLE ioctl on
 * the block device, a flag is set in the Thread object and __sanitizer_cov_trace_pc
 * will start recording this threads visited code paths .
 * 3. When the same thread then uses the KCOV_DISABLE ioctl on the block device,
 * a flag is unset in the Thread object and __sanitizer_cov_trace_pc will
 * no longer record this threads visited code paths.
 * 4. When the Process dies, the KCOVInstance and Memory::Region are GCed.
 */
class KCOVInstance final {
public:
    explicit KCOVInstance(ProcessID pid);

    ErrorOr<void> buffer_allocate(size_t buffer_size_in_entries);
    bool has_buffer() const { return m_buffer != nullptr; }
    void buffer_add_pc(u64 pc);

    Memory::VMObject* vmobject() { return m_vmobject; }

    Spinlock<LockRank::None>& spinlock() { return m_lock; }

private:
    ProcessID m_pid { 0 };
    u64 m_buffer_size_in_entries { 0 };
    size_t m_buffer_size_in_bytes { 0 };
    kcov_pc_t* m_buffer { nullptr };
    LockRefPtr<Memory::AnonymousVMObject> m_vmobject;

    // Here to ensure it's not garbage collected at the end of open()
    OwnPtr<Memory::Region> m_kernel_region;

    Spinlock<LockRank::None> m_lock {};
};

}
