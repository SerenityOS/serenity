/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <ImageDecoder/ImageDecoderClientEndpoint.h>
#include <ImageDecoder/ImageDecoderServerEndpoint.h>
#include <LibIPC/ServerConnection.h>

namespace ImageDecoderClient {

struct Frame {
    RefPtr<Gfx::Bitmap> bitmap;
    u32 duration { 0 };
};

struct DecodedImage {
    bool is_animated { false };
    u32 loop_count { 0 };
    Vector<Frame> frames;
};

class Client final
    : public IPC::ServerConnection<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint>
    , public ImageDecoderClientEndpoint {
    C_OBJECT(Client);

public:
    Optional<DecodedImage> decode_image(ReadonlyBytes);

    Function<void()> on_death;

private:
    Client();

    virtual void die() override;
};

}
