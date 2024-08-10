/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThumbnailsModel.h"

GUI::Variant ThumbnailsModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::TextAlignment)
        return Gfx::TextAlignment::BottomCenter;
    if (role != GUI::ModelRole::Display || !is_within_range(index))
        return {};
    return m_thumbnails.at(index.row());
}

void ThumbnailsModel::update_thumbnail(u32 index, NonnullRefPtr<Gfx::Bitmap const> thumbnail)
{
    if (index < m_thumbnails.size())
        m_thumbnails[index] = thumbnail;
    did_update(UpdateFlag::DontInvalidateIndices);
}

ErrorOr<void> ThumbnailsModel::reset_thumbnails(u32 page_count)
{
    auto blank_thumbnail = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { 1, 1 }, 1));
    m_thumbnails.clear();
    for (u32 i = 0; i < page_count; i++)
        m_thumbnails.append(blank_thumbnail);
    did_update(UpdateFlag::DontInvalidateIndices);
    return {};
}
