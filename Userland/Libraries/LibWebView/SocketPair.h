/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/File.h>

namespace WebView {

struct SocketPair {
    IPC::File socket;
    IPC::File fd_passing_socket;
};

}

namespace IPC {

template<>
ErrorOr<void> encode(Encoder&, WebView::SocketPair const&);

template<>
ErrorOr<WebView::SocketPair> decode(Decoder&);

}
