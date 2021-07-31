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
class GlobalScope : public ScopeBase {
    AK_MAKE_NONCOPYABLE(GlobalScope);
    AK_MAKE_NONMOVABLE(GlobalScope);

public:
    static NonnullOwnPtr<GlobalScope> must_create(const Vector<PhysicalAddress>& aml_table_addresses);

private:
    explicit GlobalScope(const Vector<PhysicalAddress>& aml_table_addresses);
    void parse_encoded_bytes(Span<const u8>);
};

}
