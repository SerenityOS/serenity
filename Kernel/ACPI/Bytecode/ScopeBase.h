/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/NeverDestroyed.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/ACPI/Bytecode/NamedObject.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {

class TermObjectEnumerator;
class ScopeBase {
public:
    void add_named_object(Badge<TermObjectEnumerator> enumerator, NamedObject&);
    void add_named_object(Badge<TermObjectEnumerator> enumerator, const NamedObject&);

protected:
    IntrusiveList<NamedObject, RefPtr<NamedObject>, &NamedObject::m_list_node> m_named_objects;
};

}
