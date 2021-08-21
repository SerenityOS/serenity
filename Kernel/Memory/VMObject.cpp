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

VMObject::VMObject(VMObject const& other)
    : m_physical_pages(other.m_physical_pages)
{
    all_instances().with([&](auto& list) { list.append(*this); });
}

VMObject::VMObject(size_t size)
    : m_physical_pages(ceil_div(size, static_cast<size_t>(PAGE_SIZE)))
{
    all_instances().with([&](auto& list) { list.append(*this); });
}

VMObject::~VMObject()
{
    VERIFY(m_regions.is_empty());
}

}
