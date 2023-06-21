/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Memory/Region.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$get_stack_bounds(Userspace<FlatPtr*> user_stack_base, Userspace<size_t*> user_stack_size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto& regs = Thread::current()->get_register_dump_from_stack();
    FlatPtr stack_pointer = regs.userspace_sp();
    return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
        auto* stack_region = space->find_region_containing(Memory::VirtualRange { VirtualAddress(stack_pointer), 1 });

        // The syscall handler should have killed us if we had an invalid stack pointer.
        VERIFY(stack_region);

        FlatPtr stack_base = stack_region->range().base().get();
        size_t stack_size = stack_region->size();
        TRY(copy_to_user(user_stack_base, &stack_base));
        TRY(copy_to_user(user_stack_size, &stack_size));
        return 0;
    });
}

}
