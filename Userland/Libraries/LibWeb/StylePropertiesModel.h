/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <LibGUI/Model.h>
#include <LibWeb/CSS/StyleProperties.h>

namespace Web {

class StylePropertiesModel final : public GUI::Model {
public:
    enum Column {
        PropertyName,
        PropertyValue,
        __Count
    };

    static NonnullRefPtr<StylePropertiesModel> create(StringView properties)
    {
        auto json_or_error = JsonValue::from_string(properties);
        if (!json_or_error.has_value())
            VERIFY_NOT_REACHED();

        return adopt_ref(*new StylePropertiesModel(json_or_error.value().as_object()));
    }

    virtual ~StylePropertiesModel() override;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;

private:
    explicit StylePropertiesModel(JsonObject);

    JsonObject m_properties;

    struct Value {
        String name;
        String value;
    };
    Vector<Value> m_values;
};

}
