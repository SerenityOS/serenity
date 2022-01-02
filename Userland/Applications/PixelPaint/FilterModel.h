/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filters/Filter.h"
#include "ImageEditor.h"
#include <AK/NonnullRefPtr.h>
#include <LibGUI/Model.h>

namespace PixelPaint {

class FilterModel final : public GUI::Model {

public:
    class FilterInfo : public RefCounted {
    public:
        enum class Type {
            Category,
            Filter,
        } type;

        String text;

        Filter* filter { nullptr };

        NonnullRefPtrVector<FilterInfo> children;
        FilterInfo* parent;

        template<typename FilterType>
        static NonnullRefPtr<FilterInfo> create_filter(ImageEditor* editor, FilterInfo* parent = nullptr)
        {
            auto filter = static_cast<Filter*>(new FilterType(editor));
            auto filter_info = adopt_ref(*new FilterInfo(Type::Filter, filter->filter_name(), parent));
            filter_info->filter = filter;
            filter_info->ref();
            if (parent)
                parent->children.append(filter_info);
            return filter_info;
        }

        static NonnullRefPtr<FilterInfo> create_category(String const& text, FilterInfo* parent = nullptr)
        {
            auto category = adopt_ref(*new FilterInfo(Type::Category, text, parent));
            category->ref();
            if (parent)
                parent->children.append(category);
            return category;
        }

        FilterInfo(Type type, String text, FilterInfo* parent)
            : type(type)
            , text(move(text))
            , parent(parent)
        {
        }
    };

    static NonnullRefPtr<FilterModel> create(ImageEditor* editor)
    {
        return adopt_ref(*new FilterModel(editor));
    }

    virtual ~FilterModel() override {};

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 1; }
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, const GUI::ModelIndex& = GUI::ModelIndex()) const override;

private:
    FilterModel(ImageEditor* editor);

    NonnullRefPtrVector<FilterInfo> m_filters;
    GUI::Icon m_filter_icon;
};
}
