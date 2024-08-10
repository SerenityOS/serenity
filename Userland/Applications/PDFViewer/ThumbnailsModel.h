/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGUI/Model.h>
#include <LibGfx/Bitmap.h>

class ThumbnailsModel final : public GUI::Model {
public:
    static NonnullRefPtr<ThumbnailsModel> create()
    {
        return adopt_ref(*new ThumbnailsModel());
    }

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return m_thumbnails.size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 1; }
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole = GUI::ModelRole::Display) const override;

    void append_thumbnail(NonnullRefPtr<Gfx::Bitmap const> thumbnail);
    void update_thumbnail(u32 index, NonnullRefPtr<Gfx::Bitmap const> thumbnail);
    ErrorOr<void> reset_thumbnails(u32 page_count);

private:
    ThumbnailsModel() = default;

    Vector<NonnullRefPtr<Gfx::Bitmap const>> m_thumbnails;
};
