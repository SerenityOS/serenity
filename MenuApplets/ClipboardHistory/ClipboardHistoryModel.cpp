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
    default:
        ASSERT_NOT_REACHED();
    }
}

GUI::Variant ClipboardHistoryModel::data(const GUI::ModelIndex& index, Role) const
{
    auto& data_and_type = m_history_items[index.row()];
    switch (index.column()) {
    case Column::Data:
        if (data_and_type.type.starts_with("text/"))
            return data_and_type.data;
        return "<...>";
    case Column::Type:
        return data_and_type.type;
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
    m_history_items.prepend(item);
    update();
}
