/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibGUI/TreeView.h>
#include <LibPDF/Document.h>

class OutlineModel final : public GUI::Model {
public:
    static NonnullRefPtr<OutlineModel> create(const NonnullRefPtr<PDF::OutlineDict>& outline);

    void set_index_open_state(const GUI::ModelIndex& index, bool is_open);

    virtual int row_count(const GUI::ModelIndex&) const override;
    virtual int column_count(const GUI::ModelIndex&) const override;
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex&) const override;

private:
    OutlineModel(const NonnullRefPtr<PDF::OutlineDict>& outline);

    GUI::Icon m_closed_item_icon;
    GUI::Icon m_open_item_icon;
    NonnullRefPtr<PDF::OutlineDict> m_outline;
    HashTable<PDF::OutlineItem*> m_open_outline_items;
};
