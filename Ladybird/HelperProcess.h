/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Types.h"
#include <AK/Error.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <LibProtocol/RequestClient.h>
#include <LibProtocol/WebSocketClient.h>
#include <LibWebView/ViewImplementation.h>
#include <LibWebView/WebContentClient.h>

ErrorOr<NonnullRefPtr<WebView::WebContentClient>> launch_web_content_process(WebView::ViewImplementation& view,
    ReadonlySpan<String> candidate_web_content_paths,
    WebView::EnableCallgrindProfiling,
    WebView::IsLayoutTestMode,
    Ladybird::UseLagomNetworking);

ErrorOr<NonnullRefPtr<Protocol::RequestClient>> launch_request_server_process(ReadonlySpan<String> candidate_request_server_paths, StringView serenity_resource_root);
ErrorOr<NonnullRefPtr<Protocol::WebSocketClient>> launch_web_socket_process(ReadonlySpan<String> candidate_web_socket_paths, StringView serenity_resource_root);
