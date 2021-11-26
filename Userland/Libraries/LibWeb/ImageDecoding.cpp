/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/ImageDecoding.h>

namespace Web {

ImageDecoderClient::Client& image_decoder_client()
{
    static RefPtr<ImageDecoderClient::Client> image_decoder_client;
    if (!image_decoder_client) {
        image_decoder_client = ImageDecoderClient::Client::construct();
        image_decoder_client->on_death = [&] {
            image_decoder_client = nullptr;
        };
    }
    return *image_decoder_client;
}

}
