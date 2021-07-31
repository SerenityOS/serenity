/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/ElementsPackage.h>
#include <Kernel/ACPI/Bytecode/TermObjectEvaluator.h>

namespace Kernel::ACPI {

NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(ConstObjectType const_opcode)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(const_opcode)).release_nonnull();
}
NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(u8 integer)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(integer)).release_nonnull();
}
NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(u16 integer)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(integer)).release_nonnull();
}
NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(u32 integer)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(integer)).release_nonnull();
}
NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(u64 integer)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(integer)).release_nonnull();
}
NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(const ElementsPackage& package)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(package)).release_nonnull();
}
NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(StringView string)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(string)).release_nonnull();
}
NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(const NameString& name_string)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(name_string)).release_nonnull();
}
NonnullRefPtr<ElementsPackage::Element> ElementsPackage::Element::must_create(ByteBuffer buffer, Package::DecodingResult size)
{
    return adopt_ref_if_nonnull(new ElementsPackage::Element(buffer, size)).release_nonnull();
}

ElementsPackage::Element::Element(ConstObjectType const_opcode)
{
    m_possible_data.const_opcode = const_opcode;
    m_type = Type::Const;
}
ElementsPackage::Element::Element(u8 integer)
{
    m_possible_data.byte_data = integer;
    m_type = Type::ByteData;
}
ElementsPackage::Element::Element(u16 integer)
{
    m_possible_data.word_data = integer;
    m_type = Type::WordData;
}
ElementsPackage::Element::Element(u32 integer)
{
    m_possible_data.dword_data = integer;
    m_type = Type::DWordData;
}
ElementsPackage::Element::Element(u64 integer)
{
    m_possible_data.qword_data = integer;
    m_type = Type::QWordData;
}
ElementsPackage::Element::Element(const ElementsPackage& package)
{
    m_possible_package = package;
    m_type = Type::Package;
}
ElementsPackage::Element::Element(StringView string)
{
    m_possible_null_terminated_string = KString::try_create(string);
    m_type = Type::String;
}
ElementsPackage::Element::Element(const NameString& name_string)
{
    m_possible_name_string = name_string;
    m_type = Type::NameString;
}
ElementsPackage::Element::Element(ByteBuffer buffer, Package::DecodingResult size)
{
    m_possible_byte_buffer.size = size;
    m_possible_byte_buffer.data = buffer;
    m_type = Type::Buffer;
}

ByteBufferPackage ElementsPackage::Element::as_byte_buffer() const
{
    VERIFY(m_type == Type::Buffer);
    return m_possible_byte_buffer;
}

RefPtr<ElementsPackage> ElementsPackage::Element::as_package() const
{
    VERIFY(m_type == Type::Package);
    return m_possible_package;
}

u64 ElementsPackage::Element::as_unsigned_integer() const
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

ConstObjectType ElementsPackage::Element::as_const_object_type() const
{
    VERIFY(m_type == Type::Const);
    return m_possible_data.const_opcode;
}

u64 ElementsPackage::Element::as_u64() const
{
    VERIFY(m_type == Type::QWordData);
    return m_possible_data.qword_data;
}

u32 ElementsPackage::Element::as_u32() const
{
    VERIFY(m_type == Type::DWordData);
    return m_possible_data.dword_data;
}

u16 ElementsPackage::Element::as_u16() const
{
    VERIFY(m_type == Type::WordData);
    return m_possible_data.word_data;
}

u8 ElementsPackage::Element::as_u8() const
{
    VERIFY(m_type == Type::ByteData);
    return m_possible_data.byte_data;
}

StringView ElementsPackage::Element::as_string() const
{
    VERIFY(m_type == Type::String);
    return m_possible_null_terminated_string->view();
}

Optional<ConstObjectType> ElementsPackage::Element::to_const_object_type() const
{
    if (m_type != Type::Const)
        return {};
    return m_possible_data.const_opcode;
}

RefPtr<ElementsPackage> ElementsPackage::Element::to_package() const
{
    if (m_type != Type::Package)
        return {};
    VERIFY(!m_possible_package.is_null());
    return m_possible_package;
}

Optional<u64> ElementsPackage::Element::to_u64() const
{
    if (m_type != Type::QWordData)
        return {};
    return m_possible_data.qword_data;
}

Optional<u32> ElementsPackage::Element::to_u32() const
{
    if (m_type != Type::DWordData)
        return {};
    return m_possible_data.dword_data;
}

Optional<u16> ElementsPackage::Element::to_u16() const
{
    if (m_type != Type::WordData)
        return {};
    return m_possible_data.word_data;
}

Optional<u8> ElementsPackage::Element::to_u8() const
{
    if (m_type != Type::ByteData)
        return {};
    return m_possible_data.byte_data;
}

Optional<StringView> ElementsPackage::Element::to_string() const
{
    if (m_type != Type::String)
        return {};
    return m_possible_null_terminated_string->view();
}

Optional<ByteBufferPackage> ElementsPackage::Element::to_byte_buffer() const
{
    if (m_type != Type::Buffer)
        return {};
    return m_possible_byte_buffer;
}

NonnullRefPtr<ElementsPackage> ElementsPackage::must_create(size_t package_size, size_t encoding_package_length, Span<const u8> encoded_elements)
{
    auto package = adopt_ref_if_nonnull(new ElementsPackage(package_size, encoding_package_length)).release_nonnull();
    package->enumerate_associated_data(encoded_elements);
    return package;
}

void ElementsPackage::enumerate_associated_data(Span<const u8> encoded_elements)
{
    auto current_encoded_elements = encoded_elements;
    while (current_encoded_elements.size() > 0) {
        TermObjectEvaluator evaluator(current_encoded_elements);
        auto possible_value = evaluator.try_to_evaluate_value();
        size_t add_to_get_to_next_object = 0;
        switch (possible_value.type()) {
        case EvaluatedValue::Type::Package: {
            auto package = possible_value.as_package();
            add_to_get_to_next_object = package->encoded_length();
            m_elements.append(Element::must_create(*package));
            break;
        }
        case EvaluatedValue::Type::ByteData: {
            add_to_get_to_next_object = 2;
            m_elements.append(Element::must_create(possible_value.as_u8()));
            break;
        }
        case EvaluatedValue::Type::WordData: {
            add_to_get_to_next_object = 3;
            m_elements.append(Element::must_create(possible_value.as_u16()));
            break;
        }
        case EvaluatedValue::Type::DWordData: {
            add_to_get_to_next_object = 5;
            m_elements.append(Element::must_create(possible_value.as_u32()));
            break;
        }
        case EvaluatedValue::Type::QWordData: {
            add_to_get_to_next_object = 9;
            m_elements.append(Element::must_create(possible_value.as_u64()));
            break;
        }
        case EvaluatedValue::Type::Const: {
            add_to_get_to_next_object = 1;
            m_elements.append(Element::must_create(possible_value.as_const_object_type()));
            break;
        }
        case EvaluatedValue::Type::NotEvaluated: {
            // Note: If evaluation failed, maybe it's a NameString!
            auto name_string = NameString::try_to_evaluate_with_validation(current_encoded_elements);
            VERIFY(name_string);
            dbgln_if(ACPI_AML_DEBUG, "Found package element with name {}", name_string->full_name());
            m_elements.append(Element::must_create(*name_string));
            add_to_get_to_next_object = name_string->encoded_length();
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }
        VERIFY(add_to_get_to_next_object != 0);
        current_encoded_elements = current_encoded_elements.slice(add_to_get_to_next_object);
    }
}

size_t ElementsPackage::encoded_length() const
{
    return m_package_length;
}

ElementsPackage::ElementsPackage(size_t package_size, size_t encoding_package_length)
    : m_package_length(package_size)
    , m_encoding_package_length(encoding_package_length)
{
}

}
