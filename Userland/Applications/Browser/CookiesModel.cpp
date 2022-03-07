/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CookiesModel.h"

namespace Browser {

void CookiesModel::add_item(Web::Cookie::Cookie const& item)
{
    begin_insert_rows({}, m_cookies.size(), m_cookies.size());
    m_cookies.append(item);
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

String CookiesModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    case Column::Value:
        return "Value";
    case Column::Domain:
        return "Domain";
    case Column::Path:
        return "Path";
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

    const auto& cookie = m_cookies[index.row()];

    switch (index.column()) {
    case Column::Name:
        return cookie.name;
    case Column::Value:
        return cookie.value;
    case Column::Domain:
        return cookie.domain;
    case Column::Path:
        return cookie.path;
    case Column::ExpiryTime:
        return cookie.expiry_time.to_string();
    }

    VERIFY_NOT_REACHED();
}

}
