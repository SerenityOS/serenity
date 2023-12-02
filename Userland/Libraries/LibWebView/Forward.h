/*
 * Copyright (c) 2022, The SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Traits.h>

namespace WebView {

class CookieJar;
class Database;
class History;
class InspectorClient;
class OutOfProcessWebView;
class ViewImplementation;
class WebContentClient;

struct CookieStorageKey;
struct SearchEngine;

}

namespace AK {

template<>
struct Traits<WebView::CookieStorageKey>;

}
