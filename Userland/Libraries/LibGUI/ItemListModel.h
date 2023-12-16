/*
 * Copyright (c) 2019-2020, Jesse Buhgaiar <jooster669@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>

namespace GUI {

template<typename T, typename Container = Vector<T>, typename ColumnNameListType = void>
class ItemListModel : public Model {
public:
    static constexpr auto IsTwoDimensional = requires(Container data) {
        requires !IsVoid<ColumnNameListType>;
        data.at(0).at(0);
        data.at(0).size();
    };

    // Substitute 'void' for a dummy u8.
    using ColumnNamesT = Conditional<IsVoid<ColumnNameListType>, u8, ColumnNameListType>;

    static NonnullRefPtr<ItemListModel> create(Container const& data, ColumnNamesT const& column_names, Optional<size_t> const& row_count = {})
    requires(IsTwoDimensional)
    {
        return adopt_ref(*new ItemListModel<T, Container, ColumnNameListType>(data, column_names, row_count));
    }
    static NonnullRefPtr<ItemListModel> create(Container const& data, Optional<size_t> const& row_count = {})
    requires(!IsTwoDimensional)
    {
        return adopt_ref(*new ItemListModel<T, Container>(data, row_count));
    }

    virtual ~ItemListModel() override = default;

    virtual int row_count(ModelIndex const& index) const override
    {
        if (!index.is_valid())
            return m_provided_row_count.has_value() ? *m_provided_row_count : m_data.size();
        return 0;
    }

    virtual int column_count(ModelIndex const& index) const override
    {
        // if it's 2D (e.g. Vector<Vector<T>>)
        if constexpr (IsTwoDimensional) {
            if (index.is_valid())
                return m_data.at(index.row()).size();
            if (m_data.size())
                return m_data.at(0).size();
            return 0;
        }

        // Otherwise, let's just assume it's 1D.
        return 1;
    }

    virtual ErrorOr<String> column_name(int index) const override
    {
        if constexpr (IsTwoDimensional)
            return m_column_names[index];
        return "Data"_string;
    }

    virtual Variant data(ModelIndex const& index, ModelRole role) const override
    {
        if (role == ModelRole::TextAlignment)
            return Gfx::TextAlignment::CenterLeft;
        if (role == ModelRole::Display) {
            if constexpr (IsTwoDimensional)
                return m_data.at(index.row()).at(index.column());
            else
                return m_data.at(index.row());
        }

        return {};
    }

    virtual GUI::Model::MatchResult data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const override
    {
        if (index.data().as_string().contains(term.as_string(), CaseSensitivity::CaseInsensitive))
            return { TriState::True };
        return { TriState::False };
    }

    virtual bool is_searchable() const override { return true; }
    virtual Vector<GUI::ModelIndex> matches(StringView searching, unsigned flags, GUI::ModelIndex const&) override
    {
        Vector<GUI::ModelIndex> found_indices;
        if constexpr (IsTwoDimensional) {
            for (auto it = m_data.begin(); it != m_data.end(); ++it) {
                for (auto it2d = (*it).begin(); it2d != (*it).end(); ++it2d) {
                    GUI::ModelIndex index = this->index(it.index(), it2d.index());
                    if (!string_matches(data(index, ModelRole::Display).to_byte_string(), searching, flags))
                        continue;

                    found_indices.append(index);
                    if (flags & FirstMatchOnly)
                        return found_indices;
                }
            }
        } else {
            for (auto it = m_data.begin(); it != m_data.end(); ++it) {
                GUI::ModelIndex index = this->index(it.index());
                if (!string_matches(data(index, ModelRole::Display).to_byte_string(), searching, flags))
                    continue;

                found_indices.append(index);
                if (flags & FirstMatchOnly)
                    return found_indices;
            }
        }

        return found_indices;
    }

protected:
    explicit ItemListModel(Container const& data, Optional<size_t> row_count = {})
    requires(!IsTwoDimensional)
        : m_data(data)
        , m_provided_row_count(move(row_count))
    {
    }

    explicit ItemListModel(Container const& data, ColumnNamesT const& column_names, Optional<size_t> row_count = {})
    requires(IsTwoDimensional)
        : m_data(data)
        , m_column_names(column_names)
        , m_provided_row_count(move(row_count))
    {
    }

    Container const& m_data;
    ColumnNamesT m_column_names;
    Optional<size_t> m_provided_row_count;
};

}
