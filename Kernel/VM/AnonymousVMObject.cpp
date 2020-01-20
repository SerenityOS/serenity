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

#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/PhysicalPage.h>

NonnullRefPtr<AnonymousVMObject> AnonymousVMObject::create_with_size(size_t size)
{
    return adopt(*new AnonymousVMObject(size));
}

NonnullRefPtr<AnonymousVMObject> AnonymousVMObject::create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    return adopt(*new AnonymousVMObject(paddr, size));
}

AnonymousVMObject::AnonymousVMObject(size_t size)
    : VMObject(size)
{
}

AnonymousVMObject::AnonymousVMObject(PhysicalAddress paddr, size_t size)
    : VMObject(size)
{
    ASSERT(paddr.page_base() == paddr);
    for (size_t i = 0; i < page_count(); ++i)
        physical_pages()[i] = PhysicalPage::create(paddr.offset(i * PAGE_SIZE), false, false);
}

AnonymousVMObject::AnonymousVMObject(const AnonymousVMObject& other)
    : VMObject(other)
{
}

AnonymousVMObject::~AnonymousVMObject()
{
}

NonnullRefPtr<VMObject> AnonymousVMObject::clone()
{
    return adopt(*new AnonymousVMObject(*this));
}
