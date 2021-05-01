/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/VM/ContiguousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

NonnullRefPtr<ContiguousVMObject> ContiguousVMObject::create_with_size(size_t size, size_t physical_alignment)
{
    return adopt_ref(*new ContiguousVMObject(size, physical_alignment));
}

ContiguousVMObject::ContiguousVMObject(size_t size, size_t physical_alignment)
    : VMObject(size)
{
    auto contiguous_physical_pages = MM.allocate_contiguous_supervisor_physical_pages(size, physical_alignment);
    for (size_t i = 0; i < page_count(); i++) {
        physical_pages()[i] = contiguous_physical_pages[i];
        dbgln_if(CONTIGUOUS_VMOBJECT_DEBUG, "Contiguous page[{}]: {}", i, physical_pages()[i]->paddr());
    }
}

ContiguousVMObject::ContiguousVMObject(const ContiguousVMObject& other)
    : VMObject(other)
{
}

ContiguousVMObject::~ContiguousVMObject()
{
}

RefPtr<VMObject> ContiguousVMObject::clone()
{
    VERIFY_NOT_REACHED();
}

}
