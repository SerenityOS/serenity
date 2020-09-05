/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "ClipboardHistoryModel.h"
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>

NonnullRefPtr<ClipboardHistoryModel> ClipboardHistoryModel::create()
{
    return adopt(*new ClipboardHistoryModel());
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
        ASSERT_NOT_REACHED();
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
            builder.append(data_and_type.metadata.get("bpp").value_or("?"));
            builder.append(" bitmap");
            builder.append("]");
            return builder.to_string();
        }
        return "<...>";
    case Column::Type:
        return data_and_type.mime_type;
    case Column::Size:
        return AK::human_readable_size(data_and_type.data.size());
    default:
        ASSERT_NOT_REACHED();
    }
}

void ClipboardHistoryModel::update()
{
    did_update();
}

void ClipboardHistoryModel::add_item(const GUI::Clipboard::DataAndType& item)
{
    if (m_history_items.size() == m_history_limit)
        m_history_items.take_last();
    m_history_items.prepend(item);
    update();
}
