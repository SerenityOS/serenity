/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClipboardHistoryModel.h"
#include <AK/JsonParser.h>
#include <AK/NumberFormat.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibConfig/Client.h>
#include <LibCore/File.h>

NonnullRefPtr<ClipboardHistoryModel> ClipboardHistoryModel::create()
{
    return adopt_ref(*new ClipboardHistoryModel());
}

ClipboardHistoryModel::ClipboardHistoryModel()
    : m_history_limit(Config::read_i32("ClipboardHistory"sv, "ClipboardHistory"sv, "NumHistoryItems"sv, 20))
{
}

ErrorOr<String> ClipboardHistoryModel::column_name(int column) const
{
    switch (column) {
    case Column::Data:
        return "Data"_string;
    case Column::Type:
        return "Type"_string;
    case Column::Size:
        return "Size"_string;
    case Column::Time:
        return "Time"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

static StringView bpp_for_format_resilient(ByteString format)
{
    unsigned format_uint = format.to_number<unsigned>().value_or(static_cast<unsigned>(Gfx::BitmapFormat::Invalid));
    // Cannot use Gfx::Bitmap::bpp_for_format here, as we have to accept invalid enum values.
    switch (static_cast<Gfx::BitmapFormat>(format_uint)) {
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
            return ByteString::copy(data_and_type.data);
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
            return builder.to_byte_string();
        }
        if (data_and_type.mime_type.starts_with("glyph/"sv)) {
            StringBuilder builder;
            auto count = data_and_type.metadata.get("count").value().to_number<unsigned>().value_or(0);
            auto start = data_and_type.metadata.get("start").value().to_number<unsigned>().value_or(0);
            auto width = data_and_type.metadata.get("width").value().to_number<unsigned>().value_or(0);
            auto height = data_and_type.metadata.get("height").value().to_number<unsigned>().value_or(0);
            if (count > 1) {
                builder.appendff("U+{:04X}..U+{:04X} ({} glyphs) [{}x{}]", start, start + count - 1, count, width, height);
            } else {
                builder.appendff("U+{:04X} (", start);
                builder.append_code_point(start);
                builder.appendff(") [{}x{}]", width, height);
            }
            return builder.to_byte_string();
        }
        return "<...>";
    case Column::Type:
        return data_and_type.mime_type;
    case Column::Size:
        return AK::human_readable_size(data_and_type.data.size());
    case Column::Time:
        return time.to_byte_string();
    default:
        VERIFY_NOT_REACHED();
    }
}

void ClipboardHistoryModel::clipboard_content_did_change(ByteString const&)
{
    auto data_and_type = GUI::Clipboard::the().fetch_data_and_type();
    if (!(data_and_type.data.is_empty() && data_and_type.mime_type.is_empty() && data_and_type.metadata.is_empty()))
        add_item(data_and_type);
}

ErrorOr<void> ClipboardHistoryModel::invalidate_model_and_file()
{
    invalidate();

    TRY(write_to_file());
    return {};
}

void ClipboardHistoryModel::add_item(GUI::Clipboard::DataAndType const& item)
{
    m_history_items.remove_first_matching([&](ClipboardItem& existing) {
        return existing.data_and_type.data == item.data && existing.data_and_type.mime_type == item.mime_type;
    });

    if (m_history_items.size() >= m_history_limit) {
        m_history_items.resize(m_history_limit - 1);
    }

    m_history_items.prepend({ item, Core::DateTime::now() });
    invalidate_model_and_file().release_value_but_fixme_should_propagate_errors();
}

void ClipboardHistoryModel::remove_item(int index)
{
    m_history_items.remove(index);
    invalidate_model_and_file().release_value_but_fixme_should_propagate_errors();
}

void ClipboardHistoryModel::clear()
{
    m_history_items.clear();
    invalidate_model_and_file().release_value_but_fixme_should_propagate_errors();
}

void ClipboardHistoryModel::config_i32_did_change(StringView domain, StringView group, StringView key, i32 value)
{
    if (domain != "ClipboardHistory" || group != "ClipboardHistory")
        return;

    if (key == "NumHistoryItems") {
        if (value < (int)m_history_items.size()) {
            m_history_items.remove(value, m_history_items.size() - value);
            invalidate_model_and_file().release_value_but_fixme_should_propagate_errors();
        }
        m_history_limit = value;
        return;
    }
}

ErrorOr<ClipboardHistoryModel::ClipboardItem> ClipboardHistoryModel::ClipboardItem::from_json(JsonObject const& object)
{
    if (!object.has("data_and_type"sv) && !object.has("time"sv))
        return Error::from_string_literal("JsonObject does not contain necessary fields");

    ClipboardItem result;
    result.data_and_type = TRY(GUI::Clipboard::DataAndType::from_json(*object.get_object("data_and_type"sv)));
    result.time = Core::DateTime::from_timestamp(*object.get_integer<time_t>("time"sv));

    return result;
}

ErrorOr<JsonObject> ClipboardHistoryModel::ClipboardItem::to_json() const
{
    JsonObject object;

    object.set("data_and_type", TRY(data_and_type.to_json()));
    object.set("time", time.timestamp());

    return object;
}

ErrorOr<void> ClipboardHistoryModel::read_from_file(ByteString const& path)
{
    m_path = path;

    auto read_from_file_impl = [this]() -> ErrorOr<void> {
        auto file = TRY(Core::File::open(m_path, Core::File::OpenMode::Read));
        auto contents = TRY(file->read_until_eof());
        auto json = TRY(JsonValue::from_string(contents));
        if (!json.is_array())
            return Error::from_string_literal("File contents is not a JSON array.");
        auto& json_array = json.as_array();
        for (auto const& item : json_array.values()) {
            if (!item.is_object())
                return Error::from_string_literal("JSON entry is not an object.");
            TRY(m_history_items.try_append(TRY(ClipboardItem::from_json(item.as_object()))));
        }
        // Ensure the data is how we expect: Limited to m_history_limit and sorted newest-to-oldest.
        quick_sort(m_history_items, [](ClipboardItem const& a, ClipboardItem const& b) -> bool {
            return a.time > b.time;
        });
        if (m_history_items.size() > m_history_limit)
            m_history_items.resize(m_history_limit);

        return {};
    };

    auto maybe_error = read_from_file_impl();
    if (maybe_error.is_error())
        dbgln("Unable to load clipboard history: {}", maybe_error.release_error());

    return {};
}

ErrorOr<void> ClipboardHistoryModel::write_to_file()
{
    auto file = TRY(Core::File::open(m_path, Core::File::OpenMode::Write | Core::File::OpenMode::Truncate));
    JsonArray array;
    array.ensure_capacity(m_history_items.size());
    for (auto const& item : m_history_items) {
        if (!item.data_and_type.mime_type.starts_with("text/"sv))
            continue;
        TRY(array.append(TRY(item.to_json())));
    }
    auto json_string = array.to_byte_string();
    TRY(file->write_until_depleted(json_string.bytes()));

    return {};
}
