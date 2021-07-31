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
#include <Kernel/ACPI/Bytecode/TermObjectEnumerator.h>
#include <Kernel/ACPI/Bytecode/Types.h>
#include <Kernel/KString.h>

namespace Kernel::ACPI {

class ElementsPackage : public RefCounted<ElementsPackage> {
public:
    class Element : public RefCounted<Element> {
        friend class ElementsPackage;

    public:
        enum class Type {
            NameString,
            Integer,
            ByteData,
            WordData,
            DWordData,
            QWordData,
            String,
            Const,
            RevisionOp,
            Buffer,
            Package,
            VariablePackage,
            Unknown,
        };

    public:
        static NonnullRefPtr<Element> must_create(ConstObjectType const_opcode);
        static NonnullRefPtr<Element> must_create(u8 integer);
        static NonnullRefPtr<Element> must_create(u16 integer);
        static NonnullRefPtr<Element> must_create(u32 integer);
        static NonnullRefPtr<Element> must_create(u64 integer);
        static NonnullRefPtr<Element> must_create(const ElementsPackage& package);
        static NonnullRefPtr<Element> must_create(StringView string);
        static NonnullRefPtr<Element> must_create(const NameString& name_string);
        static NonnullRefPtr<Element> must_create(ByteBuffer buffer, Package::DecodingResult size);
        Type type() const { return m_type; }

        ByteBufferPackage as_byte_buffer() const;
        RefPtr<ElementsPackage> as_package() const;
        u64 as_unsigned_integer() const;
        ConstObjectType as_const_object_type() const;
        u64 as_u64() const;
        u32 as_u32() const;
        u16 as_u16() const;
        u8 as_u8() const;
        StringView as_string() const;
        Optional<ConstObjectType> to_const_object_type() const;

        RefPtr<ElementsPackage> to_package() const;
        Optional<u64> to_u64() const;
        Optional<u32> to_u32() const;
        Optional<u16> to_u16() const;
        Optional<u8> to_u8() const;
        Optional<StringView> to_string() const;
        Optional<ByteBufferPackage> to_byte_buffer() const;

    private:
        explicit Element(ConstObjectType const_opcode);
        explicit Element(u8 integer);
        explicit Element(u16 integer);
        explicit Element(u32 integer);
        explicit Element(u64 integer);
        explicit Element(const ElementsPackage& package);
        explicit Element(StringView string);
        explicit Element(const NameString& name_string);
        explicit Element(ByteBuffer buffer, Package::DecodingResult size);

        Type m_type { Type::Unknown };
        RefPtr<NameString> m_possible_name_string;
        RefPtr<ElementsPackage> m_possible_package;
        ByteBufferPackage m_possible_byte_buffer;
        OwnPtr<KString> m_possible_null_terminated_string;
        unsigned m_possible_integer;
        union ConstantData m_possible_data;
        IntrusiveListNode<Element, RefPtr<Element>> list_node {};
    };

public:
    static NonnullRefPtr<ElementsPackage> must_create(size_t package_size, size_t encoding_package_length, Span<const u8> encoded_elements);

    void enumerate_associated_data(Span<const u8> encoded_elements);

    size_t encoded_length() const;

private:
    ElementsPackage(size_t package_size, size_t encoding_package_length);
    IntrusiveList<Element, RefPtr<Element>, &Element::list_node> m_elements;
    size_t m_package_length;
    size_t m_encoding_package_length;
};

}
