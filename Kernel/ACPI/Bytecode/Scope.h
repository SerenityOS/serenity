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
#include <Kernel/ACPI/Bytecode/ScopeBase.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {

class TermObjectEnumerator;
class Scope
    : public NamedObject
    , public ScopeBase {
public:
    virtual NamedObject::Type type() const override { return NamedObject::Type::Scope; }
    static NonnullRefPtr<Scope> must_create(const TermObjectEnumerator&, Span<u8 const> encoded_name_string);
    void enumerate(const TermObjectEnumerator&);

protected:
    explicit Scope(const NameString& preloaded_name_string);
    explicit Scope(Span<u8 const> encoded_name_string);
};

}
