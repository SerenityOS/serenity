/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/FieldList.h>
#include <Kernel/ACPI/Bytecode/Package.h>

namespace Kernel::ACPI {

FieldList::Element::Element()
    : m_type(FieldList::Element::Type::Reserved)
{
}
FieldList::Element::Element(StringView name_segment, size_t package_size)
    : m_type(FieldList::Element::Type::Named)
{
    m_seg_name = NameString::try_to_create_with_string_view(name_segment);
    m_data.package_size = package_size;
}
FieldList::Element::Element(u8 access_type, u8 access_attribute)
    : m_type(FieldList::Element::Type::Access)
{
    m_data.access_properties.m_access_attribute = access_attribute;
    m_data.access_properties.m_access_type = access_type;
}
FieldList::Element::Element(u8 access_type, u8 access_attribute, u8 access_length)
    : m_type(FieldList::Element::Type::ExtendedAccess)
{
    m_data.extended_access_properties.base.m_access_attribute = access_attribute;
    m_data.extended_access_properties.base.m_access_type = access_type;
    m_data.extended_access_properties.m_access_length = access_length;
}

FieldList::Element::AccessProperties FieldList::Element::access_properties() const
{
    VERIFY(type() == FieldList::Element::Type::Access);
    return m_data.access_properties;
}

FieldList::Element::ExtendedAccessProperties FieldList::Element::extended_access_properties() const
{
    VERIFY(type() == FieldList::Element::Type::ExtendedAccess);
    return m_data.extended_access_properties;
}

const NameString* FieldList::Element::possible_name_string() const
{
    return m_seg_name;
}

Optional<size_t> FieldList::Element::possible_package_size() const
{
    if (type() == FieldList::Element::Type::Named || type() == FieldList::Element::Type::Reserved)
        return m_data.package_size;
    return {};
}

OwnPtr<FieldList> FieldList::create(Span<u8 const> encoded_field_list)
{
    // FIXME: Add a guard to not iterate for a very long and not reasonable lengths.
    auto current_field_list = encoded_field_list;
    Vector<FieldList::Element> elements;
    while (current_field_list.size() > 0) {
        switch (current_field_list[0]) {
        case 0x00: { // Reserved Field
            Vector<u8> package_encoding_other_bytes;
            {
                size_t index = 1;
                while (index < 4) {
                    package_encoding_other_bytes.append(current_field_list[index]);
                    index++;
                }
            }
            auto result = Package::parse_encoded_package_length(current_field_list[1], package_encoding_other_bytes);
            elements.append(FieldList::Element {});
            current_field_list = current_field_list.slice(1 + result.encoding_length);
            break;
        }

        case 0x01: { // Access Field
            elements.append(FieldList::Element { current_field_list[1], current_field_list[2] });
            current_field_list = current_field_list.slice(3);
            break;
        }
        case 0x03: { // Extended Access Field
            elements.append(FieldList::Element { current_field_list[1], current_field_list[2], current_field_list[3] });
            current_field_list = current_field_list.slice(4);
            break;
        }
        case 0x02: { // Connect Field
            TODO();
        }
        default: // Named Field
            auto field_element_name_segment = StringView(current_field_list.trim(4));
            auto field_element_package_length_span = current_field_list.slice(4);
            Vector<u8> package_encoding_other_bytes;
            {
                size_t index = 1;
                while (index < 4 && index < field_element_package_length_span.size()) {
                    package_encoding_other_bytes.append(field_element_package_length_span[index]);
                    index++;
                }
            }
            auto result = Package::parse_encoded_package_length(field_element_package_length_span[0], package_encoding_other_bytes);
            elements.append(FieldList::Element { field_element_name_segment, result.package_size });
            current_field_list = current_field_list.slice(4 + result.encoding_length);
        }
    }
    return adopt_own_if_nonnull(new (nothrow) FieldList(elements, encoded_field_list.size()));
}

FieldList::FieldList(const Vector<FieldList::Element>& elements, size_t elements_encoded_length)
    : m_elements(elements)
    , m_elements_encoded_length(elements_encoded_length)
{
    // FIXME: Verify elements_encoded_length is correct somehow...
}

}
