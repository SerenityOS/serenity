/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 * Copyright (c) 2022-2023, the SerenityOS developers.
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

        static ErrorOr<ClipboardItem> from_json(JsonObject const& object);
        ErrorOr<JsonObject> to_json() const;
    };

    virtual ~ClipboardHistoryModel() override = default;

    ClipboardItem const& item_at(int index) const { return m_history_items[index]; }
    void add_item(const GUI::Clipboard::DataAndType& item);
    void remove_item(int index);
    void clear();
    bool is_empty() { return m_history_items.is_empty(); }

    ErrorOr<void> read_from_file(ByteString const& path);
    ErrorOr<void> write_to_file();

    ErrorOr<void> invalidate_model_and_file();

    // ^GUI::Model
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    // ^Config::Listener
    virtual void config_i32_did_change(StringView domain, StringView group, StringView key, i32 value) override;

private:
    ClipboardHistoryModel();

    // ^GUI::Model
    virtual int row_count(const GUI::ModelIndex&) const override { return m_history_items.size(); }
    virtual ErrorOr<String> column_name(int) const override;
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }

    // ^GUI::Clipboard::ClipboardClient
    virtual void clipboard_content_did_change(ByteString const&) override;

    Vector<ClipboardItem> m_history_items;
    size_t m_history_limit;

    ByteString m_path;
};
