/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Hex.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>

struct Match {
    u64 offset;
    String value;
};

class SearchResultsModel final : public GUI::Model {
public:
    enum Column {
        Offset,
        Value
    };

    explicit SearchResultsModel(Vector<Match> const&& matches)
        : m_matches(move(matches))
    {
    }

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override
    {
        return m_matches.size();
    }

    virtual int column_count(const GUI::ModelIndex&) const override
    {
        return 2;
    }

    ErrorOr<String> column_name(int column) const override
    {
        switch (column) {
        case Column::Offset:
            return "Offset"_string;
        case Column::Value:
            return "Value"_string;
        }
        VERIFY_NOT_REACHED();
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::TextAlignment)
            return Gfx::TextAlignment::CenterLeft;
        if (role == GUI::ModelRole::Custom) {
            auto& match = m_matches.at(index.row());
            return match.offset;
        }
        if (role == GUI::ModelRole::Display) {
            auto& match = m_matches.at(index.row());
            switch (index.column()) {
            case Column::Offset:
                return ByteString::formatted("{:#08X}", match.offset);
            case Column::Value: {
                Utf8View utf8_view(match.value);
                if (!utf8_view.validate())
                    return {};
                return StringView(match.value);
            }
            }
        }
        return {};
    }

private:
    Vector<Match> m_matches;
};
