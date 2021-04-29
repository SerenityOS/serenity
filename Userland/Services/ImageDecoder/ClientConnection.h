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
    : public IPC::ClientConnection<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint>
    , public ImageDecoderServerEndpoint {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

private:
    virtual OwnPtr<Messages::ImageDecoderServer::GreetResponse> handle(const Messages::ImageDecoderServer::Greet&) override;
    virtual OwnPtr<Messages::ImageDecoderServer::DecodeImageResponse> handle(const Messages::ImageDecoderServer::DecodeImage&) override;
};

}
