/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibConfig/Listener.h>
#include <LibCore/DateTime.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Model.h>

class ClipboardHistoryModel final : public GUI::Model
    , public GUI::Clipboard::ClipboardClient
    , public Config::Listener {
public:
    static NonnullRefPtr<ClipboardHistoryModel> create();

    enum Column {
        Data,
        Type,
        Size,
        Time,
        __Count
    };

    struct ClipboardItem {
        GUI::Clipboard::DataAndType data_and_type;
        Core::DateTime time;
    };

    virtual ~ClipboardHistoryModel() override = default;

    const ClipboardItem& item_at(int index) const { return m_history_items[index]; }
    void remove_item(int index);

    // ^GUI::Model
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    // ^Config::Listener
    virtual void config_string_did_change(String const& domain, String const& group, String const& key, String const& value) override;

private:
    ClipboardHistoryModel();
    void add_item(const GUI::Clipboard::DataAndType& item);

    // ^GUI::Model
    virtual int row_count(const GUI::ModelIndex&) const override { return m_history_items.size(); }
    virtual String column_name(int) const override;
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }

    // ^GUI::Clipboard::ClipboardClient
    virtual void clipboard_content_did_change(const String&) override { add_item(GUI::Clipboard::the().fetch_data_and_type()); }

    Vector<ClipboardItem> m_history_items;
    size_t m_history_limit;
};
