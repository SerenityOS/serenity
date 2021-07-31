/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/EncodedObjectOpcode.h>

namespace Kernel::ACPI {

EncodedObjectOpcode::EncodedObjectOpcode(Array<u8, 2> encoded_named_object_opcode)
{
    m_encoded_named_object_opcode[0] = encoded_named_object_opcode[0];
    m_encoded_named_object_opcode[1] = encoded_named_object_opcode[1];
}

bool EncodedObjectOpcode::has_extended_prefix() const
{
    return m_encoded_named_object_opcode[0] == 0x5B;
}

size_t EncodedObjectOpcode::length() const
{
    return has_extended_prefix() ? 2 : 1;
}

Optional<EncodedObjectOpcode::Opcode> EncodedObjectOpcode::opcode() const
{
    if (!has_extended_prefix()) {
        switch (m_encoded_named_object_opcode[0]) {
        case 0x06:
            return Opcode::Alias;
        case 0x08:
            return Opcode::Name;
        case 0x10:
            return Opcode::Scope;
        case 0x8D:
            return Opcode::CreateBitField;
        case 0x8C:
            return Opcode::CreateByteField;
        case 0x8A:
            return Opcode::CreateDWordField;
        case 0x8F:
            return Opcode::CreateQWordField;
        case 0x8B:
            return Opcode::CreateWordField;
        case 0x15:
            return Opcode::External;

        // Note: According to the ACPI spec, this is a named object, but this is
        // not defined under the "NamedObj :=" notation!
        case 0x14:
            return Opcode::Method;
        };

        return {};
    }
    switch (m_encoded_named_object_opcode[1]) {
    case 0x87:
        return Opcode::BankField;
    case 0x88:
        return Opcode::DataRegion;
    case 0x80:
        return Opcode::OpRegion;
    case 0x84:
        return Opcode::PowerResource;
    case 0x83:
        return Opcode::Processor;
    case 0x85:
        return Opcode::ThermalZone;

    // Note: According to the ACPI spec, these are named objects, but they're
    // not defined under the "NamedObj :=" notation!
    case 0x82:
        return Opcode::Device;
    case 0x02:
        return Opcode::Event;
    case 0x81:
        return Opcode::Field;
    case 0x86:
        return Opcode::IndexField;
    case 0x01:
        return Opcode::Mutex;
    };
    return {};
}

}
