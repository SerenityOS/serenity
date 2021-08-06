/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClipboardHistoryModel.h"
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>

NonnullRefPtr<ClipboardHistoryModel> ClipboardHistoryModel::create()
{
    return adopt_ref(*new ClipboardHistoryModel());
}

ClipboardHistoryModel::~ClipboardHistoryModel()
{
}

String ClipboardHistoryModel::column_name(int column) const
{
    switch (column) {
    case Column::Data:
        return "Data";
    case Column::Type:
        return "Type";
    case Column::Size:
        return "Size";
    default:
        VERIFY_NOT_REACHED();
    }
}

static const char* bpp_for_format_resilient(String format)
{
    unsigned format_uint = format.to_uint().value_or(static_cast<unsigned>(Gfx::BitmapFormat::Invalid));
    // Cannot use Gfx::Bitmap::bpp_for_format here, as we have to accept invalid enum values.
    switch (static_cast<Gfx::BitmapFormat>(format_uint)) {
    case Gfx::BitmapFormat::Indexed1:
        return "1";
    case Gfx::BitmapFormat::Indexed2:
        return "2";
    case Gfx::BitmapFormat::Indexed4:
        return "4";
    case Gfx::BitmapFormat::Indexed8:
        return "8";
    case Gfx::BitmapFormat::BGRx8888:
    case Gfx::BitmapFormat::BGRA8888:
        return "32";
    case Gfx::BitmapFormat::Invalid:
        /* fall-through */
    default:
        return "?";
    }
}

GUI::Variant ClipboardHistoryModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};
    auto& data_and_type = m_history_items[index.row()];
    switch (index.column()) {
    case Column::Data:
        if (data_and_type.mime_type.starts_with("text/"))
            return String::copy(data_and_type.data);
        if (data_and_type.mime_type == "image/x-serenityos") {
            StringBuilder builder;
            builder.append("[");
            builder.append(data_and_type.metadata.get("width").value_or("?"));
            builder.append('x');
            builder.append(data_and_type.metadata.get("height").value_or("?"));
            builder.append('x');
            builder.append(bpp_for_format_resilient(data_and_type.metadata.get("height").value_or("0")));
            builder.append(" bitmap");
            builder.append("]");
            return builder.to_string();
        }
        if (data_and_type.mime_type.starts_with("glyph/")) {
            StringBuilder builder;
            builder.append("[");
            builder.append(data_and_type.metadata.get("width").value_or("?"));
            builder.append("x");
            builder.append(data_and_type.metadata.get("height").value_or("?"));
            builder.append("] ");
            builder.append("(");
            builder.append(data_and_type.metadata.get("char").value_or(""));
            builder.append(")");
            return builder.to_string();
        }
        return "<...>";
    case Column::Type:
        return data_and_type.mime_type;
    case Column::Size:
        return AK::human_readable_size(data_and_type.data.size());
    default:
        VERIFY_NOT_REACHED();
    }
}

void ClipboardHistoryModel::add_item(const GUI::Clipboard::DataAndType& item)
{
    m_history_items.remove_first_matching([&](GUI::Clipboard::DataAndType& existing) {
        return existing.data == item.data && existing.mime_type == item.mime_type;
    });

    if (m_history_items.size() == m_history_limit)
        m_history_items.take_last();

    m_history_items.prepend(item);
    invalidate();
}

void ClipboardHistoryModel::remove_item(int index)
{
    m_history_items.remove(index);
}
