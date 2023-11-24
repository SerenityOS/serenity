/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "StringUtils.h"
#include <AK/JsonValue.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWebView/ModelIndex.h>
#include <LibWebView/PropertyTableModel.h>
#include <QAbstractItemModel>

namespace Ladybird {

template<typename ModelType>
class ModelAdapter : public QAbstractItemModel {
public:
    using Type = typename ModelType::Type;

    static ErrorOr<NonnullOwnPtr<ModelAdapter>> create(Type type, StringView model, QObject* parent = nullptr)
    {
        auto json_model = TRY(JsonValue::from_string(model));
        if (!json_model.is_object())
            return Error::from_string_literal("Expected model to be a JSON object");

        return adopt_own(*new ModelAdapter(type, move(json_model), parent));
    }

    virtual int rowCount(QModelIndex const& parent) const override
    {
        return m_model.row_count(to_web_view_model_index(parent));
    }

    virtual int columnCount(QModelIndex const& parent) const override
    {
        return m_model.column_count(to_web_view_model_index(parent));
    }

    virtual QModelIndex index(int row, int column, QModelIndex const& parent) const override
    {
        auto index = m_model.index(row, column, to_web_view_model_index(parent));
        return to_qt_model_index(index);
    }

    virtual QModelIndex parent(QModelIndex const& index) const override
    {
        if constexpr (requires { m_model.parent(declval<WebView::ModelIndex const&>()); }) {
            auto parent = m_model.parent(to_web_view_model_index(index));
            return to_qt_model_index(parent);
        } else {
            return {};
        }
    }

    virtual QVariant data(QModelIndex const& index, int role) const override
    {
        if (role == Qt::DisplayRole) {
            auto text = m_model.text_for_display(to_web_view_model_index(index));
            return qstring_from_ak_string(text);
        }

        return {};
    }

    QModelIndex index_for_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> const& pseudo_element) const
    {
        if constexpr (requires { m_model.index_for_node(node_id, pseudo_element); }) {
            auto parent = m_model.index_for_node(node_id, pseudo_element);
            return to_qt_model_index(parent);
        } else {
            return {};
        }
    }

private:
    ModelAdapter(Type type, JsonValue model, QObject* parent)
        : QAbstractItemModel(parent)
        , m_model(type, move(model))
    {
    }

    ALWAYS_INLINE QModelIndex to_qt_model_index(WebView::ModelIndex const& index) const
    {
        if (!index.is_valid())
            return {};
        return createIndex(index.row, index.column, index.internal_data);
    }

    ALWAYS_INLINE WebView::ModelIndex to_web_view_model_index(QModelIndex const& index) const
    {
        if (!index.isValid())
            return {};
        return { index.row(), index.column(), index.constInternalPointer() };
    }

    ModelType m_model;
};

using PropertyTableModel = ModelAdapter<WebView::PropertyTableModel>;

}
