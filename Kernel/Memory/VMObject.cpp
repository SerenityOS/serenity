/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/VMObject.h>

namespace Kernel::Memory {

static Singleton<SpinlockProtected<VMObject::AllInstancesList>> s_all_instances;

SpinlockProtected<VMObject::AllInstancesList>& VMObject::all_instances()
{
    return s_all_instances;
}

ErrorOr<FixedArray<RefPtr<PhysicalPage>>> VMObject::try_clone_physical_pages() const
{
    return m_physical_pages.try_clone();
}

ErrorOr<FixedArray<RefPtr<PhysicalPage>>> VMObject::try_create_physical_pages(size_t size)
{
    return FixedArray<RefPtr<PhysicalPage>>::try_create(ceil_div(size, static_cast<size_t>(PAGE_SIZE)));
}

VMObject::VMObject(FixedArray<RefPtr<PhysicalPage>>&& new_physical_pages)
    : m_physical_pages(move(new_physical_pages))
{
    all_instances().with([&](auto& list) { list.append(*this); });
}

VMObject::~VMObject()
{
    VERIFY(m_regions.is_empty());
}

}
