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

#include <AK/ScopeGuard.h>
#include <Kernel/Process.h>
#include <Kernel/Ptrace.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PrivateInodeVMObject.h>
#include <Kernel/VM/ProcessPagingScope.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/SharedInodeVMObject.h>

namespace Kernel {

int Process::sys$ptrace(Userspace<const Syscall::SC_ptrace_params*> user_params)
{
    REQUIRE_PROMISE(proc);
    Syscall::SC_ptrace_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;
    auto result = Ptrace::handle_syscall(params, *this);
    return result.is_error() ? result.error() : result.value();
}

/**
 * "Does this process have a thread that is currently being traced by the provided process?"
 */
bool Process::has_tracee_thread(ProcessID tracer_pid) const
{
    bool has_tracee = false;

    for_each_thread([&](Thread& t) {
        if (t.tracer() && t.tracer()->tracer_pid() == tracer_pid) {
            has_tracee = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return has_tracee;
}

KResultOr<u32> Process::peek_user_data(Userspace<const u32*> address)
{
    uint32_t result;

    // This function can be called from the context of another
    // process that called PT_PEEK
    ProcessPagingScope scope(*this);
    if (!copy_from_user(&result, address)) {
        dbg() << "Invalid address for peek_user_data: " << address.ptr();
        return KResult(-EFAULT);
    }

    return result;
}

KResult Process::poke_user_data(Userspace<u32*> address, u32 data)
{
    ProcessPagingScope scope(*this);
    Range range = { VirtualAddress(address), sizeof(u32) };
    auto* region = find_region_containing(range);
    ASSERT(region != nullptr);
    if (region->is_shared()) {
        // If the region is shared, we change its vmobject to a PrivateInodeVMObject
        // to prevent the write operation from changing any shared inode data
        ASSERT(region->vmobject().is_shared_inode());
        region->set_vmobject(PrivateInodeVMObject::create_with_inode(static_cast<SharedInodeVMObject&>(region->vmobject()).inode()));
        region->set_shared(false);
    }
    const bool was_writable = region->is_writable();
    if (!was_writable) {
        region->set_writable(true);
        region->remap();
    }
    ScopeGuard rollback([&]() {
        if (!was_writable) {
            region->set_writable(false);
            region->remap();
        }
    });

    if (!copy_to_user(address, &data)) {
        dbg() << "Invalid address for poke_user_data: " << address.ptr();
        return KResult(-EFAULT);
    }

    return KResult(KSuccess);
}

}
