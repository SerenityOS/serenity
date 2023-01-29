/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClipboardHistoryModel.h"
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>
#include <LibConfig/Client.h>

NonnullRefPtr<ClipboardHistoryModel> ClipboardHistoryModel::create()
{
    return adopt_ref(*new ClipboardHistoryModel());
}

ClipboardHistoryModel::ClipboardHistoryModel()
    : m_history_limit(Config::read_i32("ClipboardHistory"sv, "ClipboardHistory"sv, "NumHistoryItems"sv, 20))
{
}

DeprecatedString ClipboardHistoryModel::column_name(int column) const
{
    switch (column) {
    case Column::Data:
        return "Data";
    case Column::Type:
        return "Type";
    case Column::Size:
        return "Size";
    case Column::Time:
        return "Time";
    default:
        VERIFY_NOT_REACHED();
    }
}

static StringView bpp_for_format_resilient(DeprecatedString format)
{
    unsigned format_uint = format.to_uint().value_or(static_cast<unsigned>(Gfx::BitmapFormat::Invalid));
    // Cannot use Gfx::Bitmap::bpp_for_format here, as we have to accept invalid enum values.
    switch (static_cast<Gfx::BitmapFormat>(format_uint)) {
    case Gfx::BitmapFormat::Indexed1:
        return "1"sv;
    case Gfx::BitmapFormat::Indexed2:
        return "2"sv;
    case Gfx::BitmapFormat::Indexed4:
        return "4"sv;
    case Gfx::BitmapFormat::Indexed8:
        return "8"sv;
    case Gfx::BitmapFormat::BGRx8888:
    case Gfx::BitmapFormat::BGRA8888:
        return "32"sv;
    case Gfx::BitmapFormat::Invalid:
        /* fall-through */
    default:
        return "?"sv;
    }
}

GUI::Variant ClipboardHistoryModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};
    auto& item = m_history_items[index.row()];
    auto& data_and_type = item.data_and_type;
    auto& time = item.time;
    switch (index.column()) {
    case Column::Data:
        if (data_and_type.mime_type.starts_with("text/"sv))
            return DeprecatedString::copy(data_and_type.data);
        if (data_and_type.mime_type == "image/x-serenityos") {
            StringBuilder builder;
            builder.append('[');
            builder.append(data_and_type.metadata.get("width").value_or("?"));
            builder.append('x');
            builder.append(data_and_type.metadata.get("height").value_or("?"));
            builder.append('x');
            builder.append(bpp_for_format_resilient(data_and_type.metadata.get("format").value_or("0")));
            builder.append(']');
            builder.append(" bitmap"sv);
            return builder.to_deprecated_string();
        }
        if (data_and_type.mime_type.starts_with("glyph/"sv)) {
            StringBuilder builder;
            auto count = data_and_type.metadata.get("count").value().to_uint().value_or(0);
            auto start = data_and_type.metadata.get("start").value().to_uint().value_or(0);
            auto width = data_and_type.metadata.get("width").value().to_uint().value_or(0);
            auto height = data_and_type.metadata.get("height").value().to_uint().value_or(0);
            if (count > 1) {
                builder.appendff("U+{:04X}..U+{:04X} ({} glyphs) [{}x{}]", start, start + count - 1, count, width, height);
            } else {
                builder.appendff("U+{:04X} (", start);
                builder.append_code_point(start);
                builder.appendff(") [{}x{}]", width, height);
            }
            return builder.to_deprecated_string();
        }
        return "<...>";
    case Column::Type:
        return data_and_type.mime_type;
    case Column::Size:
        return AK::human_readable_size(data_and_type.data.size());
    case Column::Time:
        return time.to_deprecated_string();
    default:
        VERIFY_NOT_REACHED();
    }
}

void ClipboardHistoryModel::clipboard_content_did_change(DeprecatedString const&)
{
    auto data_and_type = GUI::Clipboard::the().fetch_data_and_type();
    if (!(data_and_type.data.is_empty() && data_and_type.mime_type.is_empty() && data_and_type.metadata.is_empty()))
        add_item(data_and_type);
}

void ClipboardHistoryModel::add_item(const GUI::Clipboard::DataAndType& item)
{
    m_history_items.remove_first_matching([&](ClipboardItem& existing) {
        return existing.data_and_type.data == item.data && existing.data_and_type.mime_type == item.mime_type;
    });

    if (m_history_items.size() == m_history_limit)
        m_history_items.take_last();

    m_history_items.prepend({ item, Core::DateTime::now() });
    invalidate();
}

void ClipboardHistoryModel::remove_item(int index)
{
    m_history_items.remove(index);
    invalidate();
}

void ClipboardHistoryModel::clear()
{
    m_history_items.clear();
    invalidate();
}

void ClipboardHistoryModel::config_string_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& value_string)
{
    if (domain != "ClipboardHistory" || group != "ClipboardHistory")
        return;

    // FIXME: Once we can get notified for `i32` changes, we can use that instead of this hack.
    if (key == "NumHistoryItems") {
        auto value_or_error = value_string.to_int();
        if (!value_or_error.has_value())
            return;
        auto value = value_or_error.value();
        if (value < (int)m_history_items.size()) {
            m_history_items.remove(value, m_history_items.size() - value);
            invalidate();
        }
        m_history_limit = value;
        return;
    }
}
