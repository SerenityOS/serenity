/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/ItemListModel.h>

namespace GUI {

struct FontWeightNameMapping {
    constexpr FontWeightNameMapping(int w, const char* n)
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

    virtual Variant data(const ModelIndex& index, ModelRole role) const override
    {
        if (role == ModelRole::Custom)
            return m_data.at(index.row());
        if (role == ModelRole::Display)
            return String(weight_to_name(m_data.at(index.row())));
        return ItemListModel::data(index, role);
    }
};

}
