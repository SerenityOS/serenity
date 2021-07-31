/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/ACPI/Bytecode/NameString.h>

namespace Kernel::ACPI {
class FieldList {
public:
    class Element {
    public:
        enum class Type {
            Named,
            Reserved,
            Access,
            ExtendedAccess,
            Connect,
        };

        struct AccessProperties {
            u8 m_access_type;
            u8 m_access_attribute;
        };

        struct ExtendedAccessProperties {
            AccessProperties base;
            u8 m_access_length;
        };

    public:
        Element();                                                      // Reserved Element
        Element(StringView name_segment, size_t package_size);          // Named Element
        Element(u8 access_type, u8 access_attribute);                   // Access Element
        Element(u8 access_type, u8 access_attribute, u8 access_length); // ExtendedAccess Element
        Type type() const { return m_type; }

        AccessProperties access_properties() const;
        ExtendedAccessProperties extended_access_properties() const;
        const NameString* possible_name_string() const;
        Optional<size_t> possible_package_size() const;

    private:
        Type m_type;
        RefPtr<NameString> m_seg_name;
        union {
            size_t package_size;
            AccessProperties access_properties;
            ExtendedAccessProperties extended_access_properties;
        } m_data;
    };

public:
    static OwnPtr<FieldList> create(Span<u8 const> encoded_field_list);
    size_t elements_encoded_length() const { return m_elements_encoded_length; }
    const Vector<Element>& elements() const { return m_elements; }

private:
    FieldList(const Vector<Element>& elements, size_t elements_encoded_length);
    Vector<Element> m_elements;
    size_t m_elements_encoded_length;
};

}
