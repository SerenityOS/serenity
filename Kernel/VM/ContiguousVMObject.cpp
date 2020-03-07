/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <Kernel/VM/ContiguousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

//#define CONTIGUOUS_VMOBJECT_DEBUG

NonnullRefPtr<ContiguousVMObject> ContiguousVMObject::create_with_size(size_t size)
{
    return adopt(*new ContiguousVMObject(size));
}

ContiguousVMObject::ContiguousVMObject(size_t size)
    : VMObject(size)
{
    auto contiguous_physical_pages = MM.allocate_contiguous_supervisor_physical_pages(size);
    for (size_t i = 0; i < page_count(); i++) {
        physical_pages()[i] = contiguous_physical_pages[i];
#ifdef CONTIGUOUS_VMOBJECT_DEBUG
        dbg() << "Contiguous page[" << i << "]: " << physical_pages()[i]->paddr();
#endif
    }
}

ContiguousVMObject::ContiguousVMObject(const ContiguousVMObject& other)
    : VMObject(other)
{
}

ContiguousVMObject::~ContiguousVMObject()
{
}

NonnullRefPtr<VMObject> ContiguousVMObject::clone()
{
    ASSERT_NOT_REACHED();
}

}
