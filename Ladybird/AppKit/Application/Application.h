/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>
#include <LibWebView/Forward.h>
#include <LibWebView/SocketPair.h>

#import <System/Cocoa.h>

namespace Ladybird {
class WebViewBridge;
}

@interface Application : NSApplication

- (instancetype)init;

- (ErrorOr<void>)launchRequestServer:(Vector<ByteString> const&)certificates;
- (ErrorOr<NonnullRefPtr<WebView::WebContentClient>>)launchWebContent:(Ladybird::WebViewBridge&)web_view_bridge;
- (ErrorOr<WebView::SocketPair>)launchWebWorker;

@end
