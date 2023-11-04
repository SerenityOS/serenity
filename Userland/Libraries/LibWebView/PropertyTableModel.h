/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IterationDecision.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWebView/ModelIndex.h>

namespace WebView {

class PropertyTableModel {
public:
    enum class Type {
        ARIAProperties,
        StyleProperties,
    };

    enum class Column : int {
        PropertyName,
        PropertyValue,
    };

    PropertyTableModel(Type, JsonValue const&);
    ~PropertyTableModel();

    template<typename Callback>
    void for_each_property_name(Callback&& callback)
    {
        for (size_t i = 0; i < m_values.size(); ++i) {
            ModelIndex index { static_cast<int>(i), to_underlying(WebView::PropertyTableModel::Column::PropertyName) };
            auto const& property_name = m_values[i].name;

            if (callback(index, property_name) == IterationDecision::Break)
                break;
        }
    }

    int row_count(ModelIndex const& parent) const;
    int column_count(ModelIndex const& parent) const;
    ErrorOr<String> column_name(int) const;
    ModelIndex index(int row, int column, ModelIndex const& parent) const;
    String text_for_display(ModelIndex const& index) const;

private:
    struct Value {
        String name;
        String value;
    };
    Vector<Value> m_values;
};

}
