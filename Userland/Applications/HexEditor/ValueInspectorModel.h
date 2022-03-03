/*
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Hex.h>
#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
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
        __Count
    };

    enum Column {
        Type,
        Value
    };

    explicit ValueInspectorModel()
    {
        for (int i = 0; i < ValueType::__Count; i++)
            set_parsed_value(static_cast<ValueType>(i), "");
    }

    void set_parsed_value(ValueType type, String value)
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

    String column_name(int column) const override
    {
        switch (column) {
        case Column::Type:
            return "Type";
        case Column::Value:
            return "Value";
        }
        VERIFY_NOT_REACHED();
    }

    String inspector_value_type_to_string(ValueType type) const
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
                return inspector_value_type_to_string(static_cast<ValueType>(index.row()));
            case Column::Value:
                return m_values.at(index.row());
            }
        }
        if (role == GUI::ModelRole::Custom) {
            ValueType selected_type = static_cast<ValueType>(index.row());
            switch (selected_type) {
            case SignedByte:
            case UnsignedByte:
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
            default:
                return 0;
            }
        }
        return {};
    }

private:
    Array<String, ValueType::__Count> m_values = {};
};
