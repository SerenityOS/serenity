/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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

#include "NetworkHistoryModel.h"
#include <AK/StringBuilder.h>
#include <AK/QuickSort.h>

namespace Web {

NetworkHistoryModel::NetworkHistoryModel(const HashMap<u32, Entry>& history)
{
    // HACK: Hash map does not preserve insertion order,
    //       so I'm sorting the load ids and then copying entries to a vector.
    auto load_ids = history.keys();

    quick_sort(load_ids, [](const auto id0, const auto id1) {
        return id0 < id1;
    });

    m_entries.ensure_capacity(history.size());

    for (auto load_id : load_ids) {
        auto opt_entry = history.get(load_id);
        m_entries.append(opt_entry.value());
    }
}

NetworkHistoryModel::~NetworkHistoryModel()
{
}

int NetworkHistoryModel::row_count(const GUI::ModelIndex &) const
{
    return m_entries.size();
}

String NetworkHistoryModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::Name:
        return "Name";
    case Column::Path:
        return "Path";
    case Column::Host:
        return "Host";
    case Column::Protocol:
        return "Protocol";
    case Column::Time:
        return "Time";
    default:
        ASSERT_NOT_REACHED();
    }
}

GUI::Variant NetworkHistoryModel::data(const GUI::ModelIndex& index, Role role) const
{
    auto& entry = m_entries.at(index.row());

    if (role == Role::Display) {
        if (index.column() == Column::Name) {
            if (entry.url.protocol() == "data")
                return "[data]";

            return entry.url.basename();
        }

        if (index.column() == Column::Path) {
            if (entry.url.protocol() == "data")
                return "N/A";

            return entry.url.path();
        }

        if (index.column() == Column::Host) {
            if (entry.url.protocol() == "data" || entry.url.protocol() == "file")
                return "N/A";

            return entry.url.host();
        }

        if (index.column() == Column::Protocol)
            return entry.url.protocol();

        if (index.column() == Column::Time) {
            if (!entry.complete)
                return "Pending";

            StringBuilder builder;
            builder.appendf("%d ms", entry.time);

            if (entry.cached)
                builder.append(" (cached)");

            return builder.to_string();
        }
    }

    if (role == Role::ForegroundColor) {
        if (entry.complete && !entry.success)
            return Color(Color::Red);
    }

    if (role == Role::Custom) {
        return entry.url.to_string();
    }

    return {};
}

void NetworkHistoryModel::update()
{
    did_update();
}

}