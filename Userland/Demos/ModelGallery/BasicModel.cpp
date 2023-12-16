/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BasicModel.h"

GUI::Variant BasicModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};
    if (!is_within_range(index))
        return {};

    return m_items.at(index.row());
}

GUI::Model::MatchResult BasicModel::data_matches(GUI::ModelIndex const& index, GUI::Variant const& data) const
{
    if (!is_within_range(index))
        return { TriState::False };
    if (!data.is_string())
        return { TriState::False };

    auto& value = m_items.at(index.row());

    if (value.contains(data.as_string()))
        return { TriState::True };

    return { TriState::False };
}

void BasicModel::invalidate()
{
    Model::invalidate();
    if (on_invalidate)
        on_invalidate();
}

GUI::ModelIndex BasicModel::index(int row, int column, GUI::ModelIndex const& parent) const
{
    if (column != 0)
        return {};
    if (parent.is_valid())
        return {};
    if (row < 0 || row >= static_cast<int>(m_items.size()))
        return {};

    return create_index(row, column);
}

void BasicModel::add_item(ByteString const& item)
{
    begin_insert_rows({}, m_items.size(), m_items.size());
    m_items.append(item);
    end_insert_rows();

    did_update(UpdateFlag::DontInvalidateIndices);
}

void BasicModel::remove_item(GUI::ModelIndex const& index)
{
    if (!index.is_valid() || !is_within_range(index))
        return;

    begin_delete_rows({}, index.row(), index.row());
    m_items.remove(index.row());
    end_delete_rows();

    did_update(UpdateFlag::DontInvalidateIndices);
}
