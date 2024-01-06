/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <LibWebView/SocketPair.h>

template<>
ErrorOr<void> IPC::encode(Encoder& encoder, WebView::SocketPair const& pair)
{
    TRY(encoder.encode(pair.socket));
    TRY(encoder.encode(pair.fd_passing_socket));
    return {};
}

template<>
ErrorOr<WebView::SocketPair> IPC::decode(Decoder& decoder)
{
    auto socket = TRY(decoder.decode<IPC::File>());
    auto fd_passing_socket = TRY(decoder.decode<IPC::File>());

    return WebView::SocketPair { move(socket), move(fd_passing_socket) };
}
