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

template<typename T>
class ItemListModel final : public GUI::Model {
public:
    static NonnullRefPtr<ItemListModel> create(Vector<T>& data) { return adopt(*new ItemListModel<T>(data)); }

    virtual ~ItemListModel() override {}

    virtual int row_count(const GUI::ModelIndex&) const override
    {
        return m_data.size();
    }

    virtual int column_count(const GUI::ModelIndex&) const override
    {
        return 1;
    }

    virtual String column_name(int) const override
    {
        return "Data";
    }

    virtual ColumnMetadata column_metadata(int) const override
    {
        return { 70, Gfx::TextAlignment::CenterLeft };
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, Role role = Role::Display) const override
    {
        if (role == Role::Display)
            return m_data.at(index.row());

        return {};
    }

    virtual void update() override
    {
        did_update();
    }

private:
    explicit ItemListModel(Vector<T>& data)
        : m_data(data)
    {
    }

    Vector<T>& m_data;
};
