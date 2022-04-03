/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibGUI/Model.h>

namespace GUI {

class JsonArrayModel final : public Model {
public:
    struct FieldSpec {
        FieldSpec(String const& a_column_name, Gfx::TextAlignment a_text_alignment, Function<Variant(JsonObject const&)>&& a_massage_for_display, Function<Variant(JsonObject const&)>&& a_massage_for_sort = {}, Function<Variant(JsonObject const&)>&& a_massage_for_custom = {})
            : column_name(a_column_name)
            , text_alignment(a_text_alignment)
            , massage_for_display(move(a_massage_for_display))
            , massage_for_sort(move(a_massage_for_sort))
            , massage_for_custom(move(a_massage_for_custom))
        {
        }

        FieldSpec(String const& a_json_field_name, String const& a_column_name, Gfx::TextAlignment a_text_alignment)
            : json_field_name(a_json_field_name)
            , column_name(a_column_name)
            , text_alignment(a_text_alignment)
        {
        }

        String json_field_name;
        String column_name;
        Gfx::TextAlignment text_alignment;
        Function<Variant(JsonObject const&)> massage_for_display;
        Function<Variant(JsonObject const&)> massage_for_sort;
        Function<Variant(JsonObject const&)> massage_for_custom;
    };

    static NonnullRefPtr<JsonArrayModel> create(String const& json_path, Vector<FieldSpec>&& fields)
    {
        return adopt_ref(*new JsonArrayModel(json_path, move(fields)));
    }

    virtual ~JsonArrayModel() override = default;

    virtual int row_count(ModelIndex const& = ModelIndex()) const override { return m_array.size(); }
    virtual int column_count(ModelIndex const& = ModelIndex()) const override { return m_fields.size(); }
    virtual String column_name(int column) const override { return m_fields[column].column_name; }
    virtual Variant data(ModelIndex const&, ModelRole = ModelRole::Display) const override;
    virtual void invalidate() override;
    virtual void update();

    String const& json_path() const { return m_json_path; }
    void set_json_path(String const& json_path);

    bool add(Vector<JsonValue> const&& fields);
    bool set(int row, Vector<JsonValue>&& fields);
    bool remove(int row);
    bool store();

private:
    JsonArrayModel(String const& json_path, Vector<FieldSpec>&& fields)
        : m_json_path(json_path)
        , m_fields(move(fields))
    {
    }

    String m_json_path;
    Vector<FieldSpec> m_fields;
    JsonArray m_array;
};

}
