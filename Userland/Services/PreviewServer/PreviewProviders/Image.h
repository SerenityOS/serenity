/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Services/PreviewServer/PreviewProvider.h>

namespace PreviewServer::Providers {

// Provides previews for all image files via ImageDecoderClient.
class Image final : public PreviewServer::PreviewProvider {
public:
    Image() = default;
    virtual ~Image() = default;

    virtual bool can_generate_preview_for([[maybe_unused]] String const& file) override;
    virtual CacheEntry generate_preview([[maybe_unused]] String const& file) override;
};

}
