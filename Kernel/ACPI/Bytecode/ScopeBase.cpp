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
    m_named_objects.append(object);
}

void ScopeBase::add_named_object(Badge<TermObjectEnumerator>, const NamedObject& object)
{
    m_named_objects.append(const_cast<NamedObject&>(object));
}
}
