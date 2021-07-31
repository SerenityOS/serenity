/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntrusiveList.h>
#include <AK/NeverDestroyed.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/ACPI/Bytecode/ScopeBase.h>
#include <Kernel/ACPI/Bytecode/TermObjectEnumerator.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

void ScopeBase::add_named_object(Badge<TermObjectEnumerator>, NamedObject& object)
{
    MutexLocker locker(m_named_objects.lock(), Mutex::Mode::Shared);
    m_named_objects.resource().append(object);
}

void ScopeBase::add_named_object(Badge<TermObjectEnumerator>, const NamedObject& object)
{
    MutexLocker locker(m_named_objects.lock(), Mutex::Mode::Shared);
    m_named_objects.resource().append(const_cast<NamedObject&>(object));
}
size_t ScopeBase::named_objects_count_slow() const
{
    MutexLocker locker(m_named_objects.lock(), Mutex::Mode::Shared);
    return m_named_objects.resource().size_slow();
}

void ScopeBase::for_each_named_object(Function<void(const NamedObject&)> callback) const
{
    MutexLocker locker(m_named_objects.lock(), Mutex::Mode::Shared);
    for (auto& named_object : m_named_objects.resource())
        callback(named_object);
}

}
