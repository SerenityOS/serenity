/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/ACPI/Bytecode/ElementsPackage.h>
#include <Kernel/ACPI/Bytecode/Package.h>
#include <Kernel/ACPI/Bytecode/Types.h>
#include <Kernel/KString.h>

namespace Kernel::ACPI {
class EvaluatedValue {
public:
    enum class Type {
        NotEvaluated,
        Buffer,
        ThermalZone,
        Processor,
        Package,
        String,
        Device,
        ByteData,
        WordData,
        DWordData,
        QWordData,
        Const,
    };

public:
    explicit EvaluatedValue(ConstObjectType const_opcode)
    {
        m_possible_value.const_opcode = const_opcode;
        m_type = Type::Const;
    }
    explicit EvaluatedValue(u8 integer)
    {
        m_possible_value.byte_data = integer;
        m_type = Type::ByteData;
    }
    explicit EvaluatedValue(u16 integer)
    {
        m_possible_value.word_data = integer;
        m_type = Type::WordData;
    }
    explicit EvaluatedValue(u32 integer)
    {
        m_possible_value.dword_data = integer;
        m_type = Type::DWordData;
    }
    explicit EvaluatedValue(u64 integer)
    {
        m_possible_value.qword_data = integer;
        m_type = Type::QWordData;
    }
    explicit EvaluatedValue(const ElementsPackage& package)
    {
        m_possible_elements_package = package;
        m_type = Type::Package;
    }
    explicit EvaluatedValue(StringView string)
    {
        m_possible_string = KString::try_create(string);
        m_type = Type::String;
    }
    explicit EvaluatedValue(ByteBuffer buffer, Package::DecodingResult size)
    {
        m_possible_buffer.size = size;
        m_possible_buffer.data = buffer;
        m_type = Type::Buffer;
    }

    EvaluatedValue() {};

    Type type() const { return m_type; }

    ByteBufferPackage as_byte_buffer() const
    {
        VERIFY(m_type == Type::Buffer);
        return m_possible_buffer;
    }

    RefPtr<ElementsPackage> as_package() const
    {
        VERIFY(m_type == Type::Package);
        return m_possible_elements_package;
    }

    u64 as_unsigned_integer() const
    {
        if (auto possible_value = to_u64(); possible_value.has_value())
            return possible_value.value();
        if (auto possible_value = to_u32(); possible_value.has_value())
            return possible_value.value();
        if (auto possible_value = to_u16(); possible_value.has_value())
            return possible_value.value();
        if (auto possible_value = to_u8(); possible_value.has_value())
            return possible_value.value();
        if (auto possible_value = to_const_object_type(); possible_value.has_value()) {
            switch (possible_value.value()) {
            case ConstObjectType::One:
                return 1;
            case ConstObjectType::Ones:
                return 0xFF;
            case ConstObjectType::Zero:
                return 0x00;
            default:
                VERIFY_NOT_REACHED();
            }
        }
        VERIFY_NOT_REACHED();
    }

    ConstObjectType as_const_object_type() const
    {
        VERIFY(m_type == Type::Const);
        return m_possible_value.const_opcode;
    }

    u64 as_u64() const
    {
        VERIFY(m_type == Type::QWordData);
        return m_possible_value.qword_data;
    }

    u32 as_u32() const
    {
        VERIFY(m_type == Type::DWordData);
        return m_possible_value.dword_data;
    }

    u16 as_u16() const
    {
        VERIFY(m_type == Type::WordData);
        return m_possible_value.word_data;
    }

    u8 as_u8() const
    {
        VERIFY(m_type == Type::ByteData);
        return m_possible_value.byte_data;
    }

    StringView as_string() const
    {
        VERIFY(m_type == Type::String);
        return m_possible_string->view();
    }

    Optional<ConstObjectType> to_const_object_type() const
    {
        if (m_type != Type::Const)
            return {};
        return m_possible_value.const_opcode;
    }

    RefPtr<ElementsPackage> to_package() const
    {
        if (m_type != Type::Package)
            return {};
        VERIFY(!m_possible_elements_package.is_null());
        return m_possible_elements_package;
    }

    Optional<u64> to_u64() const
    {
        if (m_type != Type::QWordData)
            return {};
        return m_possible_value.qword_data;
    }

    Optional<u32> to_u32() const
    {
        if (m_type != Type::DWordData)
            return {};
        return m_possible_value.dword_data;
    }

    Optional<u16> to_u16() const
    {
        if (m_type != Type::WordData)
            return {};
        return m_possible_value.word_data;
    }

    Optional<u8> to_u8() const
    {
        if (m_type != Type::ByteData)
            return {};
        return m_possible_value.byte_data;
    }

    Optional<StringView> to_string() const
    {
        if (m_type != Type::String)
            return {};
        return m_possible_string->view();
    }

    Optional<ByteBufferPackage> to_byte_buffer() const
    {
        if (m_type != Type::Buffer)
            return {};
        return m_possible_buffer;
    }

    ~EvaluatedValue()
    {
    }

private:
    union ConstantData m_possible_value;
    OwnPtr<KString> m_possible_string;
    RefPtr<ElementsPackage> m_possible_elements_package;
    ByteBufferPackage m_possible_buffer;
    Type m_type { Type::NotEvaluated };
};

}
