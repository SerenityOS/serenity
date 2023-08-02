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
#include <LibWebView/ViewImplementation.h>
#include <LibWebView/WebContentClient.h>

ErrorOr<NonnullRefPtr<WebView::WebContentClient>> launch_web_content_process(WebView::ViewImplementation& view,
    ReadonlySpan<String> candidate_web_content_paths,
    WebView::EnableCallgrindProfiling,
    WebView::IsLayoutTestMode,
    WebView::UseJavaScriptBytecode,
    Ladybird::UseLagomNetworking);

ErrorOr<NonnullRefPtr<Protocol::RequestClient>> launch_request_server_process(ReadonlySpan<String> candidate_request_server_paths);
