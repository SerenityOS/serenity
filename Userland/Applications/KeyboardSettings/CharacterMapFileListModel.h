/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Model.h>

class CharacterMapFileListModel final : public GUI::Model {
public:
    static NonnullRefPtr<CharacterMapFileListModel> create(Vector<String>& filenames)
    {
        return adopt_ref(*new CharacterMapFileListModel(filenames));
    }

    virtual ~CharacterMapFileListModel() override { }

    virtual int row_count(const GUI::ModelIndex&) const override
    {
        return m_filenames.size();
    }

    virtual int column_count(const GUI::ModelIndex&) const override
    {
        return 1;
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        VERIFY(index.is_valid());
        VERIFY(index.column() == 0);

        if (role == GUI::ModelRole::Display)
            return m_filenames.at(index.row());

        return {};
    }

private:
    explicit CharacterMapFileListModel(Vector<String>& filenames)
        : m_filenames(filenames)
    {
    }

    Vector<String>& m_filenames;
};
