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
#include <LibImageDecoderClient/Client.h>
#include <LibProtocol/RequestClient.h>
#include <LibProtocol/WebSocketClient.h>
#include <LibWeb/Worker/WebWorkerClient.h>
#include <LibWebView/ViewImplementation.h>
#include <LibWebView/WebContentClient.h>

ErrorOr<NonnullRefPtr<WebView::WebContentClient>> launch_web_content_process(
    WebView::ViewImplementation& view,
    ReadonlySpan<String> candidate_web_content_paths,
    Ladybird::WebContentOptions const&);

ErrorOr<NonnullRefPtr<ImageDecoderClient::Client>> launch_image_decoder_process(ReadonlySpan<String> candidate_image_decoder_paths);
ErrorOr<NonnullRefPtr<Web::HTML::WebWorkerClient>> launch_web_worker_process(ReadonlySpan<String> candidate_web_worker_paths);
ErrorOr<NonnullRefPtr<Protocol::RequestClient>> launch_request_server_process(ReadonlySpan<String> candidate_request_server_paths, StringView serenity_resource_root);
ErrorOr<NonnullRefPtr<Protocol::WebSocketClient>> launch_web_socket_process(ReadonlySpan<String> candidate_web_socket_paths, StringView serenity_resource_root);
