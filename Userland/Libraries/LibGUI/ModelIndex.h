/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Traits.h>
#include <LibGUI/Forward.h>
#include <LibGUI/ModelRole.h>

namespace GUI {

class ModelIndex {
    friend class Model;

public:
    ModelIndex() { }

    bool is_valid() const { return m_model && m_row != -1 && m_column != -1; }
    int row() const { return m_row; }
    int column() const { return m_column; }

    void* internal_data() const { return m_internal_data; }

    ModelIndex parent() const;
    bool is_parent_of(const ModelIndex&) const;

    bool operator==(const ModelIndex& other) const
    {
        return m_model == other.m_model && m_row == other.m_row && m_column == other.m_column && m_internal_data == other.m_internal_data;
    }

    bool operator!=(const ModelIndex& other) const
    {
        return !(*this == other);
    }

    const Model* model() const { return m_model; }

    Variant data(ModelRole = ModelRole::Display) const;

    ModelIndex sibling(int row, int column) const;
    ModelIndex sibling_at_column(int column) const;

private:
    ModelIndex(const Model& model, int row, int column, void* internal_data)
        : m_model(&model)
        , m_row(row)
        , m_column(column)
        , m_internal_data(internal_data)
    {
    }

    const Model* m_model { nullptr };
    int m_row { -1 };
    int m_column { -1 };
    void* m_internal_data { nullptr };
};

}

namespace AK {

template<>
struct Formatter<GUI::ModelIndex> : Formatter<FormatString> {
    void format(FormatBuilder& builder, const GUI::ModelIndex& value)
    {
        if (value.internal_data())
            return Formatter<FormatString>::format(builder, "ModelIndex({},{},{})", value.row(), value.column(), value.internal_data());
        else
            return Formatter<FormatString>::format(builder, "ModelIndex({},{})", value.row(), value.column());
    }
};

template<>
struct Traits<GUI::ModelIndex> : public GenericTraits<GUI::ModelIndex> {
    static unsigned hash(const GUI::ModelIndex& index) { return pair_int_hash(index.row(), index.column()); }
};

}
