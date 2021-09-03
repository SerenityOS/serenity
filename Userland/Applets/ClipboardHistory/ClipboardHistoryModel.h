/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Vector.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Model.h>

class ClipboardHistoryModel final : public GUI::Model
    , public GUI::Clipboard::ClipboardClient {
public:
    static NonnullRefPtr<ClipboardHistoryModel> create();

    enum Column {
        Data,
        Type,
        Size,
        __Count
    };

    virtual ~ClipboardHistoryModel() override;

    const GUI::Clipboard::DataAndType& item_at(int index) const { return m_history_items[index]; }
    void remove_item(int index);

private:
    void add_item(const GUI::Clipboard::DataAndType& item);

    // ^GUI::Model
    virtual int row_count(const GUI::ModelIndex&) const override { return m_history_items.size(); }
    virtual String column_name(int) const override;
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    // ^GUI::Clipboard::ClipboardClient
    virtual void clipboard_content_did_change(const String&) override { add_item(GUI::Clipboard::the().data_and_type()); }

    Vector<GUI::Clipboard::DataAndType> m_history_items;
    size_t m_history_limit { 20 };
};
