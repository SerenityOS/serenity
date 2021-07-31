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
#include <Kernel/ACPI/Bytecode/NamedObject.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {

class Scope;
class GlobalScope;
class Alias : public NamedObject {
public:
    virtual NamedObject::Type type() const override { return NamedObject::Type::Alias; }
    static NonnullRefPtr<Alias> must_create(Badge<GlobalScope>, Span<const u8> encoded_name_strings);
    static NonnullRefPtr<Alias> must_create(Badge<Scope>, Span<const u8> encoded_name_strings);

private:
    Alias(Span<const u8> encoded_name_string, Span<const u8> encoded_aliased_name_string);
    RefPtr<NameString> m_aliased_name_string;
};

}
