/*
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Hex.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Utf16View.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>

class ValueInspectorModel final : public GUI::Model {
public:
    enum ValueType {
        SignedByte,
        UnsignedByte,
        SignedShort,
        UnsignedShort,
        SignedInt,
        UnsignedInt,
        SignedLong,
        UnsignedLong,
        Float,
        Double,
        ASCII,
        UTF8,
        UTF16,
        ASCIIString,
        UTF8String,
        UTF16String,
        __Count
    };

    enum Column {
        Type,
        Value
    };

    explicit ValueInspectorModel(bool is_little_endian)
        : m_is_little_endian(is_little_endian)
    {
        for (int i = 0; i < ValueType::__Count; i++)
            set_parsed_value(static_cast<ValueType>(i), "");
    }

    void set_parsed_value(ValueType type, DeprecatedString value)
    {
        m_values[type] = value;
    }

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override
    {
        return m_values.size();
    }

    virtual int column_count(GUI::ModelIndex const&) const override
    {
        return 2;
    }

    ErrorOr<String> column_name(int column) const override
    {
        switch (column) {
        case Column::Type:
            return "Type"_short_string;
        case Column::Value:
            return m_is_little_endian ? "Value (Little Endian)"_string : "Value (Big Endian)"_string;
        }
        VERIFY_NOT_REACHED();
    }

    DeprecatedString inspector_value_type_to_deprecated_string(ValueType type) const
    {
        switch (type) {
        case SignedByte:
            return "Signed Byte";
        case UnsignedByte:
            return "Unsigned Byte";
        case SignedShort:
            return "Signed Short";
        case UnsignedShort:
            return "Unsigned Short";
        case SignedInt:
            return "Signed Int";
        case UnsignedInt:
            return "Unsigned Int";
        case SignedLong:
            return "Signed Long";
        case UnsignedLong:
            return "Unsigned Long";
        case Float:
            return "Float";
        case Double:
            return "Double";
        case ASCII:
            return "ASCII";
        case UTF8:
            return "UTF-8";
        case UTF16:
            return "UTF-16";
        case ASCIIString:
            return "ASCII String";
        case UTF8String:
            return "UTF-8 String";
        case UTF16String:
            return "UTF-16 String";
        default:
            return "";
        }
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::TextAlignment)
            return Gfx::TextAlignment::CenterLeft;
        if (role == GUI::ModelRole::Display) {
            switch (index.column()) {
            case Column::Type:
                return inspector_value_type_to_deprecated_string(static_cast<ValueType>(index.row()));
            case Column::Value:
                return m_values.at(index.row());
            }
        }
        if (role == GUI::ModelRole::Custom) {
            ValueType selected_type = static_cast<ValueType>(index.row());
            switch (selected_type) {
            case SignedByte:
            case UnsignedByte:
            case ASCII:
                return 1;
            case SignedShort:
            case UnsignedShort:
                return 2;
            case SignedInt:
            case UnsignedInt:
            case Float:
                return 4;
            case SignedLong:
            case UnsignedLong:
            case Double:
                return 8;
            case UTF8: {
                auto utf8_view = Utf8View(m_values.at(index.row()));
                if (utf8_view.validate())
                    return static_cast<i32>(utf8_view.byte_length());
                return 0;
            }
            case UTF16: {
                auto utf16_data = utf8_to_utf16(m_values.at(index.row())).release_value_but_fixme_should_propagate_errors();
                if (Utf16View utf16_view { utf16_data }; utf16_view.validate())
                    return static_cast<i32>(utf16_view.length_in_code_units() * 2);
                return 0;
            }
            default:
                return 0;
            }
        }
        return {};
    }

private:
    bool m_is_little_endian = false;
    Array<DeprecatedString, ValueType::__Count> m_values = {};
};
