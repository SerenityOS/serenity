/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>

namespace Web::HTML {

class NavigatorIDMixin {
public:
    // WARNING: Any information in this API that varies from user to user can be used to profile the user. In fact, if
    // enough such information is available, a user can actually be uniquely identified. For this reason, user agent
    // implementers are strongly urged to include as little information in this API as possible.

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-appcodename
    DeprecatedString app_code_name() const { return "Mozilla"sv; }

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-appcodename
    DeprecatedString app_name() const { return "Netscape"sv; }

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-appversion
    DeprecatedString app_version() const;

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-platform
    DeprecatedString platform() const;

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-product
    DeprecatedString product() const { return "Gecko"sv; }

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-productsub
    DeprecatedString product_sub() const { return "20030107"sv; } // Compatibility mode "Chrome"

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-useragent
    DeprecatedString user_agent() const;

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-vendor
    DeprecatedString vendor() const { return "Google Inc."sv; } // Compatibility mode "Chrome"

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-vendorsub
    DeprecatedString vendor_sub() const { return ""sv; }

    // NOTE: If the navigator compatibility mode is Gecko, then the user agent must also support the following partial interface:
    //       bool taint_enabled()
    //       DeprecatedString oscpu()
};

}
