/*
 * Copyright (c) 2022, The SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Traits.h>

namespace WebView {

class ConsoleClient;
class CookieJar;
class Database;
class History;
class OutOfProcessWebView;
class ViewImplementation;
class WebContentClient;

struct CookieStorageKey;

}

namespace AK {

template<>
struct Traits<WebView::CookieStorageKey>;

}
