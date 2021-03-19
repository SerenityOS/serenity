/*
 * Copyright (c) 2019-2020, Jesse Buhgaiar <jooster669@gmail.com>
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

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>

namespace GUI {

template<typename T, typename Container = Vector<T>, typename ColumnNameListType = void>
class ItemListModel : public Model {
public:
    static constexpr auto IsTwoDimensional = requires(Container data)
    {
        requires !IsVoid<ColumnNameListType>::value;
        data.at(0).at(0);
        data.at(0).size();
    };

    // Substitute 'void' for a dummy u8.
    using ColumnNamesT = typename Conditional<IsVoid<ColumnNameListType>::value, u8, ColumnNameListType>::Type;

    static NonnullRefPtr<ItemListModel> create(const Container& data, const ColumnNamesT& column_names, const Optional<size_t>& row_count = {}) requires(IsTwoDimensional)
    {
        return adopt(*new ItemListModel<T, Container, ColumnNameListType>(data, column_names, row_count));
    }
    static NonnullRefPtr<ItemListModel> create(const Container& data, const Optional<size_t>& row_count = {}) requires(!IsTwoDimensional)
    {
        return adopt(*new ItemListModel<T, Container>(data, row_count));
    }

    virtual ~ItemListModel() override { }

    virtual int row_count(const ModelIndex&) const override
    {
        return m_provided_row_count.has_value() ? *m_provided_row_count : m_data.size();
    }

    virtual int column_count(const ModelIndex& index) const override
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

    virtual String column_name(int index) const override
    {
        if constexpr (IsTwoDimensional)
            return m_column_names[index];
        return "Data";
    }

    virtual Variant data(const ModelIndex& index, ModelRole role) const override
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

    virtual void update() override
    {
        did_update();
    }

protected:
    explicit ItemListModel(const Container& data, Optional<size_t> row_count = {}) requires(!IsTwoDimensional)
        : m_data(data)
        , m_provided_row_count(move(row_count))
    {
    }

    explicit ItemListModel(const Container& data, const ColumnNamesT& column_names, Optional<size_t> row_count = {}) requires(IsTwoDimensional)
        : m_data(data)
        , m_column_names(column_names)
        , m_provided_row_count(move(row_count))
    {
    }

    const Container& m_data;
    ColumnNamesT m_column_names;
    Optional<size_t> m_provided_row_count;
};

}
