/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Memory/MMIOVMObject.h>

namespace Kernel::Memory {

ErrorOr<NonnullLockRefPtr<MMIOVMObject>> MMIOVMObject::try_create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    if (paddr.offset(size) < paddr) {
        dbgln("Shenanigans! MMIOVMObject::try_create_for_physical_range({}, {}) would wrap around", paddr, size);
        // Since we can't wrap around yet, let's pretend to OOM.
        return ENOMEM;
    }

    // FIXME: We have to make this allocation because VMObject determines the size of the VMObject based on the physical pages array
    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(size));

    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) MMIOVMObject(paddr, move(new_physical_pages)));
}

MMIOVMObject::MMIOVMObject(PhysicalAddress paddr, FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages)
    : VMObject(move(new_physical_pages))
    , m_base_address(paddr)
{
    VERIFY(paddr.page_base() == paddr);
}

}
