/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/ACPI/Bytecode/NameString.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/KString.h>

namespace Kernel::ACPI {

// Named objects are evaluated when we parse the AML bytecode. Everything else is deferred
// to when it used.
class NamedObject : public RefCounted<NamedObject> {
    friend class Scope;
    friend class GlobalScope;
    friend class ScopeBase;

public:
    enum class Type {
        Scope,
        Alias,
        Name,
        Processor,
        OpRegion,
        Method,
        Device,
        Field,
        Mutex,
    };

public:
    virtual ~NamedObject() {};

    virtual Type type() const = 0;
    const NameString& name_string() const { return *m_name_string; }

protected:
    explicit NamedObject(Span<const u8> encoded_name_string);
    explicit NamedObject(const NameString&);

    IntrusiveListNode<NamedObject, RefPtr<NamedObject>> m_list_node;
    RefPtr<NameString> m_name_string;
};

}
