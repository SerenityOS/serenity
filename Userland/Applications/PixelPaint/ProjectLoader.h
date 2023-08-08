/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include <AK/JsonArray.h>
#include <AK/Result.h>
#include <AK/StringView.h>

namespace PixelPaint {

class ProjectLoader {
public:
    ProjectLoader() = default;
    ~ProjectLoader() = default;

    ErrorOr<void> load_from_file(StringView filename, NonnullOwnPtr<Core::File>);

    bool is_raw_image() const { return m_is_raw_image; }
    RefPtr<Image> release_image() const { return move(m_image); }
    JsonArray const& json_metadata() const { return m_json_metadata; }

private:
    RefPtr<Image> m_image { nullptr };
    bool m_is_raw_image { false };
    JsonArray m_json_metadata {};
};

}
