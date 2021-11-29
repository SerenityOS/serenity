/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <ImageDecoder/Forward.h>
#include <ImageDecoder/ImageDecoderClientEndpoint.h>
#include <ImageDecoder/ImageDecoderServerEndpoint.h>
#include <LibIPC/ClientConnection.h>
#include <LibWeb/Forward.h>

namespace ImageDecoder {

class ClientConnection final
    : public IPC::ClientConnection<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>);

    virtual Messages::ImageDecoderServer::DecodeImageResponse decode_image(Core::AnonymousBuffer const&) override;
};

}
