/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonValue.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibGUI/Model.h>
#include <LibGUI/TreeView.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWebView/ModelIndex.h>
#include <LibWebView/PropertyTableModel.h>
#include <LibWebView/TreeModel.h>

namespace Browser {

template<typename ModelType>
class ModelAdapter : public GUI::Model {
public:
    using Type = typename ModelType::Type;

    static ErrorOr<NonnullRefPtr<ModelAdapter>> create(Type type, StringView model)
    {
        return adopt_ref(*new ModelAdapter(type, TRY(parse_json_model(model))));
    }

    virtual ~ModelAdapter() = default;

    virtual int row_count(GUI::ModelIndex const& parent) const override
    {
        return m_model.row_count(to_web_view_model_index(parent));
    }

    virtual int column_count(GUI::ModelIndex const& parent) const override
    {
        return m_model.column_count(to_web_view_model_index(parent));
    }

    virtual GUI::ModelIndex index(int row, int column, GUI::ModelIndex const& parent) const override
    {
        auto index = m_model.index(row, column, to_web_view_model_index(parent));
        return to_gui_model_index(index);
    }

    virtual GUI::ModelIndex parent_index(GUI::ModelIndex const& index) const override
    {
        if constexpr (requires { m_model.parent(declval<WebView::ModelIndex const&>()); }) {
            auto parent = m_model.parent(to_web_view_model_index(index));
            return to_gui_model_index(parent);
        } else {
            return {};
        }
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::Display) {
            auto text = m_model.text_for_display(to_web_view_model_index(index));
            return text.to_deprecated_string();
        }

        return {};
    }

    GUI::ModelIndex index_for_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> const& pseudo_element) const
    {
        if constexpr (requires { m_model.index_for_node(node_id, pseudo_element); }) {
            auto parent = m_model.index_for_node(node_id, pseudo_element);
            return to_gui_model_index(parent);
        } else {
            return {};
        }
    }

protected:
    ModelAdapter(Type type, JsonValue model)
        : m_model(type, move(model))
    {
    }

    static ErrorOr<JsonValue> parse_json_model(StringView model)
    {
        auto json_model = TRY(JsonValue::from_string(model));
        if (!json_model.is_object())
            return Error::from_string_literal("Expected model to be a JSON object");

        return json_model;
    }

    ALWAYS_INLINE GUI::ModelIndex to_gui_model_index(WebView::ModelIndex const& index) const
    {
        if (!index.is_valid())
            return {};
        return create_index(index.row, index.column, index.internal_data);
    }

    ALWAYS_INLINE WebView::ModelIndex to_web_view_model_index(GUI::ModelIndex const& index) const
    {
        if (!index.is_valid())
            return {};
        return { index.row(), index.column(), index.internal_data() };
    }

    ModelType m_model;
};

using TreeModel = ModelAdapter<WebView::TreeModel>;

class DOMTreeModel : public TreeModel {
public:
    static ErrorOr<NonnullRefPtr<DOMTreeModel>> create(GUI::TreeView const& tree_view, StringView model)
    {
        auto json_model = TRY(parse_json_model(model));

        // FIXME: Get these from the outside somehow instead of hard-coding paths here.
        auto document_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"sv));
        auto element_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"sv));
        auto text_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-unknown.png"sv));

        return adopt_ref(*new DOMTreeModel(tree_view, move(json_model), move(document_icon), move(element_icon), move(text_icon)));
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        auto const& node = *static_cast<JsonObject const*>(index.internal_data());
        auto node_name = node.get_deprecated_string("name"sv).value_or({});
        auto type = node.get_deprecated_string("type"sv).value_or("unknown");

        // FIXME: This FIXME can go away when the icons are provided from the outside (see constructor).
        if (role == GUI::ModelRole::ForegroundColor) {
            // FIXME: Allow models to return a foreground color *role*. Then we won't need to have a GUI::TreeView& member anymore.
            if (type == "comment"sv || type == "shadow-root"sv)
                return m_tree_view.palette().syntax_comment();
            if (type == "pseudo-element"sv)
                return m_tree_view.palette().syntax_type();
            if (!node.get_bool("visible"sv).value_or(true))
                return m_tree_view.palette().syntax_comment();
            return {};
        }

        // FIXME: This FIXME can go away when the icons are provided from the outside (see constructor).
        if (role == GUI::ModelRole::Icon) {
            if (type == "document")
                return m_document_icon;
            if (type == "element")
                return m_element_icon;
            // FIXME: More node type icons?
            return m_text_icon;
        }

        return TreeModel::data(index, role);
    }

private:
    DOMTreeModel(
        GUI::TreeView const& tree_view,
        JsonValue tree,
        NonnullRefPtr<Gfx::Bitmap> document_icon,
        NonnullRefPtr<Gfx::Bitmap> element_icon,
        NonnullRefPtr<Gfx::Bitmap> text_icon)
        : TreeModel(TreeModel::Type::DOMTree, move(tree))
        , m_tree_view(tree_view)
    {
        m_document_icon.set_bitmap_for_size(16, move(document_icon));
        m_element_icon.set_bitmap_for_size(16, move(element_icon));
        m_text_icon.set_bitmap_for_size(16, move(text_icon));
    }

    GUI::TreeView const& m_tree_view;
    GUI::Icon m_document_icon;
    GUI::Icon m_element_icon;
    GUI::Icon m_text_icon;
};

class PropertyTableModel : public ModelAdapter<WebView::PropertyTableModel> {
public:
    static ErrorOr<NonnullRefPtr<ModelAdapter>> create(Type type, StringView model)
    {
        return adopt_ref(*new PropertyTableModel(type, TRY(parse_json_model(model))));
    }

    virtual ErrorOr<String> column_name(int column_index) const override
    {
        return m_model.column_name(column_index);
    }

    virtual bool is_searchable() const override { return true; }

    virtual Vector<GUI::ModelIndex> matches(StringView searching, unsigned flags, GUI::ModelIndex const&) override
    {
        Vector<GUI::ModelIndex> found_indices;

        m_model.for_each_property_name([&](auto index, auto const& property_name) {
            if (!string_matches(property_name, searching, flags))
                return IterationDecision::Continue;

            found_indices.append(to_gui_model_index(index));

            if ((flags & FirstMatchOnly) != 0)
                return IterationDecision::Break;
            return IterationDecision::Continue;
        });

        return found_indices;
    }

private:
    PropertyTableModel(Type type, JsonValue model)
        : ModelAdapter(type, move(model))
    {
    }
};

}
