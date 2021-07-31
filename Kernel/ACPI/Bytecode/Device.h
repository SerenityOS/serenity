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
#include <Kernel/ACPI/Bytecode/Scope.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {

class TermObjectEnumerator;
class Device : public Scope {
public:
    virtual NamedObject::Type type() const override { return NamedObject::Type::Device; }
    static NonnullRefPtr<Device> must_create(const TermObjectEnumerator&, Span<u8 const> encoded_name_string);

private:
    explicit Device(Span<u8 const> encoded_name_string);
};

}
