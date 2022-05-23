/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CookiesModel.h"
#include <AK/FuzzyMatch.h>

namespace Browser {

void CookiesModel::set_items(AK::Vector<Web::Cookie::Cookie> items)
{
    begin_insert_rows({}, m_cookies.size(), m_cookies.size());
    m_cookies = move(items);
    end_insert_rows();

    did_update(DontInvalidateIndices);
}

void CookiesModel::clear_items()
{
    begin_insert_rows({}, m_cookies.size(), m_cookies.size());
    m_cookies.clear();
    end_insert_rows();

    did_update(DontInvalidateIndices);
}

int CookiesModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return m_cookies.size();
    return 0;
}

String CookiesModel::column_name(int column) const
{
    switch (column) {
    case Column::Domain:
        return "Domain";
    case Column::Path:
        return "Path";
    case Column::Name:
        return "Name";
    case Column::Value:
        return "Value";
    case Column::ExpiryTime:
        return "Expiry time";
    case Column::__Count:
        return {};
    }

    return {};
}

GUI::ModelIndex CookiesModel::index(int row, int column, GUI::ModelIndex const&) const
{
    if (static_cast<size_t>(row) < m_cookies.size())
        return create_index(row, column, &m_cookies.at(row));
    return {};
}

GUI::Variant CookiesModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};

    auto const& cookie = m_cookies[index.row()];

    switch (index.column()) {
    case Column::Domain:
        return cookie.domain;
    case Column::Path:
        return cookie.path;
    case Column::Name:
        return cookie.name;
    case Column::Value:
        return cookie.value;
    case Column::ExpiryTime:
        return cookie.expiry_time.to_string();
    }

    VERIFY_NOT_REACHED();
}

TriState CookiesModel::data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const
{
    auto needle = term.as_string();
    if (needle.is_empty())
        return TriState::True;

    auto const& cookie = m_cookies[index.row()];
    auto haystack = String::formatted("{} {} {} {}", cookie.domain, cookie.path, cookie.name, cookie.value);
    if (fuzzy_match(needle, haystack).score > 0)
        return TriState::True;
    return TriState::False;
}

}
