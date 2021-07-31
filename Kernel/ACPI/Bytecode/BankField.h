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
#include <Kernel/KString.h>

namespace Kernel::ACPI {

class BankField : public NamedObject {
public:
    enum AccessType {
        Any = 0,
        Byte = 1,
        Word = 2,
        DoubleWord = 3,
        QuadWord = 4,
        Buffer = 5,
        Reserved = 6,
    };
    enum UpdateRule {
        WriteAsOnes = 1,
        WriteAsZeros = 2,
    };

public:
    // FIXME: Evaluate in runtime the BankValue!

private:
    bool m_requires_lock;
    AccessType m_access_type;
    UpdateRule m_update_rule;
}

}
