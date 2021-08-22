/*
 * Copyright (c) 2021, Robin Allen <r@foon.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGUI/Model.h>

#include "Dictionary.h"

namespace Dictionary {

class DictionaryModel final : public GUI::Model {
public:
    static NonnullRefPtr<DictionaryModel> create()
    {
        return adopt_ref(*new DictionaryModel);
    }

    virtual int row_count(GUI::ModelIndex const& index = GUI::ModelIndex()) const override;

    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole = GUI::ModelRole::Display) const override;

public:
    void set_query(String query);

    String definition_of(int index) const;

private:
    Dictionary m_dictionary;
    String m_query;
    int m_first_index;
    int m_last_index;

    DictionaryModel();
    void set_range(int start, int end);
};

}
