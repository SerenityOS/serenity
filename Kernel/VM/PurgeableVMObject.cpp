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

#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PurgeableVMObject.h>

namespace Kernel {

NonnullRefPtr<PurgeableVMObject> PurgeableVMObject::create_with_size(size_t size)
{
    return adopt(*new PurgeableVMObject(size));
}

PurgeableVMObject::PurgeableVMObject(size_t size)
    : AnonymousVMObject(size)
{
}

PurgeableVMObject::PurgeableVMObject(const PurgeableVMObject& other)
    : AnonymousVMObject(other)
    , m_was_purged(other.m_was_purged)
    , m_volatile(other.m_volatile)
{
}

PurgeableVMObject::~PurgeableVMObject()
{
}

NonnullRefPtr<VMObject> PurgeableVMObject::clone()
{
    return adopt(*new PurgeableVMObject(*this));
}

int PurgeableVMObject::purge()
{
    LOCKER(m_paging_lock);
    return purge_impl();
}

int PurgeableVMObject::purge_with_interrupts_disabled(Badge<MemoryManager>)
{
    ASSERT_INTERRUPTS_DISABLED();
    if (m_paging_lock.is_locked())
        return 0;
    return purge_impl();
}

int PurgeableVMObject::purge_impl()
{
    if (!m_volatile)
        return 0;
    int purged_page_count = 0;
    for (size_t i = 0; i < m_physical_pages.size(); ++i) {
        if (m_physical_pages[i] && !m_physical_pages[i]->is_shared_zero_page())
            ++purged_page_count;
        m_physical_pages[i] = MM.shared_zero_page();
    }
    m_was_purged = true;

    if (purged_page_count > 0) {
        for_each_region([&](auto& region) {
            if (&region.vmobject() == this)
                region.remap();
        });
    }

    return purged_page_count;
}

}
