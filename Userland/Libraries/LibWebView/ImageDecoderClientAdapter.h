/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibWeb/ImageDecoding.h>

namespace ImageDecoderClient {
class Client;
}

namespace WebView {

class ImageDecoderClientAdapter : public Web::ImageDecoding::Decoder {
public:
    static NonnullRefPtr<ImageDecoderClientAdapter> create();

    virtual ~ImageDecoderClientAdapter() override = default;

    virtual Optional<Web::ImageDecoding::DecodedImage> decode_image(ReadonlyBytes) override;

private:
    explicit ImageDecoderClientAdapter() = default;

    RefPtr<ImageDecoderClient::Client> m_client;
};

}
