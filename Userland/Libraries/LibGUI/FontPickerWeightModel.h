/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ItemListModel.h>

namespace GUI {

struct FontWeightNameMapping {
    constexpr FontWeightNameMapping(int w, char const* n)
        : weight(w)
        , name(n)
    {
    }
    int weight { 0 };
    StringView name;
};

static constexpr FontWeightNameMapping font_weight_names[] = {
    { 100, "Thin" },
    { 200, "Extra Light" },
    { 300, "Light" },
    { 400, "Regular" },
    { 500, "Medium" },
    { 600, "Semi Bold" },
    { 700, "Bold" },
    { 800, "Extra Bold" },
    { 900, "Black" },
    { 950, "Extra Black" },
};

static constexpr StringView weight_to_name(int weight)
{
    for (auto& it : font_weight_names) {
        if (it.weight == weight)
            return it.name;
    }
    return {};
}

static constexpr int name_to_weight(StringView name)
{
    for (auto& it : font_weight_names) {
        if (it.name == name)
            return it.weight;
    }
    return {};
}

class FontWeightListModel : public ItemListModel<int> {
public:
    FontWeightListModel(const Vector<int>& weights)
        : ItemListModel(weights)
    {
    }

    virtual Variant data(ModelIndex const& index, ModelRole role) const override
    {
        if (role == ModelRole::Custom)
            return m_data.at(index.row());
        if (role == ModelRole::Display)
            return String(weight_to_name(m_data.at(index.row())));
        return ItemListModel::data(index, role);
    }
};

}
