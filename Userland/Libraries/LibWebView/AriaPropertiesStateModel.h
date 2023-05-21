/*
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <LibGUI/Model.h>

namespace WebView {

class AriaPropertiesStateModel final : public GUI::Model {
public:
    enum Column {
        PropertyName,
        PropertyValue,
        __Count
    };

    static ErrorOr<NonnullRefPtr<AriaPropertiesStateModel>> create(StringView properties_state)
    {
        auto json_or_error = TRY(JsonValue::from_string(properties_state));
        return adopt_nonnull_ref_or_enomem(new AriaPropertiesStateModel(json_or_error.as_object()));
    }

    virtual ~AriaPropertiesStateModel() override;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual bool is_searchable() const override { return true; }
    virtual Vector<GUI::ModelIndex> matches(StringView, unsigned flags, GUI::ModelIndex const&) override;

private:
    explicit AriaPropertiesStateModel(JsonObject);

    JsonObject m_properties_state;

    struct Value {
        DeprecatedString name;
        DeprecatedString value;
    };
    Vector<Value> m_values;
};

}
