/*
 * Copyright (c) 2022, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>

namespace Browser {

class StorageModel final : public GUI::Model {
public:
    enum Column {
        Key,
        Value,
        __Count,
    };

    void set_items(OrderedHashMap<String, String> map);
    void clear_items();
    virtual int row_count(GUI::ModelIndex const&) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::ModelIndex index(int row, int column = 0, GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role = GUI::ModelRole::Display) const override;
    virtual GUI::Model::MatchResult data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const override;

private:
    OrderedHashMap<String, String> m_local_storage_entries;
};

}
