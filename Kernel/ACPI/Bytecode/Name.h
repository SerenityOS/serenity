/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/ACPI/Bytecode/ElementsPackage.h>
#include <Kernel/ACPI/Bytecode/EncodedTermOpcode.h>
#include <Kernel/ACPI/Bytecode/EvaluatedValue.h>
#include <Kernel/ACPI/Bytecode/NamedObject.h>
#include <Kernel/ACPI/Bytecode/TermObjectEnumerator.h>
#include <Kernel/ACPI/Bytecode/Types.h>
#include <Kernel/KString.h>

namespace Kernel::ACPI {

class Name : public NamedObject {
public:
    // FIXME: Add support for DefPackage and DefVarPackage
    enum class AssociatedTypeData {
        NotEvaluated,
        TermOpcode,
        NullTerminatedString,
        QWordData,
        DWordData,
        WordData,
        ByteData,
        ConstObject,
        Buffer,
        Package,
    };

public:
    virtual NamedObject::Type type() const override { return NamedObject::Type::Name; }
    static NonnullRefPtr<Name> must_create(const TermObjectEnumerator&, Span<const u8> encoded_name);

    void eval_associated_data(const TermObjectEnumerator&);
    size_t encoded_length() const;

    AssociatedTypeData evaluated_data_type() const { return m_evaluated_data_type; }

    u8 as_byte_data() const;
    u16 as_word_data() const;
    u32 as_dword_data() const;
    u64 as_qword_data() const;
    ConstObjectType as_const_object() const;
    const KString* as_null_terminated_string() const;
    ByteBufferPackage as_byte_buffer() const;
    RefPtr<ElementsPackage> as_elements_package() const;

private:
    explicit Name(Span<const u8> encoded_name);

    AssociatedTypeData m_evaluated_data_type { AssociatedTypeData::NotEvaluated };
    ConstantData m_possible_value;
    Array<u8, 2> m_possible_encoded_term_opcode;
    OwnPtr<KString> m_null_terminated_string;
    ByteBufferPackage m_possible_byte_buffer;
    RefPtr<ElementsPackage> m_possible_elements_package;
};

}
