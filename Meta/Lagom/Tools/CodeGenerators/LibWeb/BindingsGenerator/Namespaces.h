/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/StringView.h>

namespace IDL {

static constexpr Array libweb_interface_namespaces = {
    "CSS"sv,
    "Crypto"sv,
    "DOM"sv,
    "DOMParsing"sv,
    "Encoding"sv,
    "Fetch"sv,
    "FileAPI"sv,
    "Geometry"sv,
    "HTML"sv,
    "HighResolutionTime"sv,
    "IntersectionObserver"sv,
    "NavigationTiming"sv,
    "RequestIdleCallback"sv,
    "ResizeObserver"sv,
    "SVG"sv,
    "Selection"sv,
    "UIEvents"sv,
    "URL"sv,
    "WebGL"sv,
    "WebIDL"sv,
    "WebSockets"sv,
    "XHR"sv,
};

}
