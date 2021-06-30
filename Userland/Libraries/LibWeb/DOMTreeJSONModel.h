/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2020, Adam Hodgen <ant1441@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <LibGUI/Model.h>
#include <LibWeb/Forward.h>

namespace Web {

class DOMTreeJSONModel final : public GUI::Model {
public:
    static NonnullRefPtr<DOMTreeJSONModel> create(StringView dom_tree)
    {
        auto json_or_error = JsonValue::from_string(dom_tree);
        if (!json_or_error.has_value())
            VERIFY_NOT_REACHED();

        return adopt_ref(*new DOMTreeJSONModel(json_or_error.value().as_object()));
    }

    virtual ~DOMTreeJSONModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual void update() override;

private:
    explicit DOMTreeJSONModel(JsonObject);

    ALWAYS_INLINE JsonObject const* find_parent_of_child_with_internal_id(size_t internal_id) const
    {
        return find_parent_of_child_with_internal_id(m_dom_tree, internal_id);
    }

    JsonObject const* find_parent_of_child_with_internal_id(JsonObject const&, size_t) const;

    ALWAYS_INLINE static size_t get_internal_id(const JsonObject& o)
    {
        return o.get("internal_id").as_u32();
    }

    ALWAYS_INLINE static JsonArray const* get_children(const JsonObject& o)
    {
        if (auto const* maybe_children = o.get_ptr("children"); maybe_children)
            return &maybe_children->as_array();
        return nullptr;
    }

    GUI::Icon m_document_icon;
    GUI::Icon m_element_icon;
    GUI::Icon m_text_icon;
    JsonObject m_dom_tree;
};

}
