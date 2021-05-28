/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/VM/ContiguousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

RefPtr<ContiguousVMObject> ContiguousVMObject::create_with_size(size_t size, size_t physical_alignment)
{
    auto contiguous_physical_pages = MM.allocate_contiguous_supervisor_physical_pages(size, physical_alignment);
    if (contiguous_physical_pages.is_empty())
        return {};
    return adopt_ref_if_nonnull(new ContiguousVMObject(size, contiguous_physical_pages));
}

ContiguousVMObject::ContiguousVMObject(size_t size, NonnullRefPtrVector<PhysicalPage>& contiguous_physical_pages)
    : VMObject(size)
{
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
