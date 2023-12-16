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

ErrorOr<String> CookiesModel::column_name(int column) const
{
    switch (column) {
    case Column::Domain:
        return "Domain"_string;
    case Column::Path:
        return "Path"_string;
    case Column::Name:
        return "Name"_string;
    case Column::Value:
        return "Value"_string;
    case Column::ExpiryTime:
        return "Expiry time"_string;
    case Column::SameSite:
        return "SameSite"_string;
    case Column::__Count:
        return String {};
    }

    return String {};
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
        return cookie.expiry_time_to_string();
    case Column::SameSite:
        return Web::Cookie::same_site_to_string(cookie.same_site);
    }

    VERIFY_NOT_REACHED();
}

GUI::Model::MatchResult CookiesModel::data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const
{
    auto needle = term.as_string();
    if (needle.is_empty())
        return { TriState::True };

    auto const& cookie = m_cookies[index.row()];
    auto haystack = ByteString::formatted("{} {} {} {}", cookie.domain, cookie.path, cookie.name, cookie.value);
    auto match_result = fuzzy_match(needle, haystack);
    if (match_result.score > 0)
        return { TriState::True, match_result.score };
    return { TriState::False };
}

Web::Cookie::Cookie CookiesModel::take_cookie(GUI::ModelIndex const& index)
{
    VERIFY(index.is_valid());

    auto cookie = m_cookies.take(index.row());
    did_update(InvalidateAllIndices);

    return cookie;
}

AK::Vector<Web::Cookie::Cookie> CookiesModel::take_all_cookies()
{
    auto cookies = move(m_cookies);
    did_update(InvalidateAllIndices);

    return cookies;
}

}
