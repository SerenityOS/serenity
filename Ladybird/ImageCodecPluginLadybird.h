/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Platform/ImageCodecPlugin.h>

namespace Ladybird {

class ImageCodecPluginLadybird final : public Web::Platform::ImageCodecPlugin {
public:
    ImageCodecPluginLadybird() = default;
    virtual ~ImageCodecPluginLadybird() override;

    virtual Optional<Web::Platform::DecodedImage> decode_image(ReadonlyBytes data) override;
};

}
