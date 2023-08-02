/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Platform/ImageCodecPlugin.h>

namespace Ladybird {

class ImageCodecPlugin final : public Web::Platform::ImageCodecPlugin {
public:
    ImageCodecPlugin() = default;
    virtual ~ImageCodecPlugin() override;

    virtual Optional<Web::Platform::DecodedImage> decode_image(ReadonlyBytes data) override;
};

}
