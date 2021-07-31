/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <Kernel/ACPI/Definitions.h>

namespace Kernel::ACPI {

class EncodedObjectOpcode {
public:
    enum class Opcode {
        // Basic Named Modifier Objects
        Alias,
        Name,
        Scope,

        // Named Objects without ExtOpPrefix

        CreateBitField,
        CreateByteField,
        CreateWordField,
        CreateDWordField,
        CreateField,
        CreateQWordField,
        External,

        // Extended Opcodes (encoded with ExtOpPrefix)
        BankField,
        DataRegion,
        OpRegion,
        PowerResource,
        Processor,
        ThermalZone,

        // Note: According to the ACPI spec 6.3A, these are named objects, but they're
        // not defined under the "NamedObj :=" notation!
        Device,
        Event,
        Field,
        IndexField,
        Method,
        Mutex,
    };

public:
    explicit EncodedObjectOpcode(Array<u8, 2> encoded_named_object_opcode);
    Optional<Opcode> opcode() const;

    bool has_extended_prefix() const;

    size_t length() const;

private:
    Array<u8, 2> m_encoded_named_object_opcode;
};

}
