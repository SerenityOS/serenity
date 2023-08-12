/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibWeb/HTML/NavigatorID.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-appversion
DeprecatedString NavigatorIDMixin::app_version() const
{
    // Must return the appropriate string that starts with "5.0 (", as follows:

    // Let trail be the substring of default `User-Agent` value that follows the "Mozilla/" prefix.
    auto user_agent_string = ResourceLoader::the().user_agent();

    auto trail = user_agent_string.substring_view(strlen("Mozilla/"), user_agent_string.length() - strlen("Mozilla/"));

    // If the navigator compatibility mode is Chrome or WebKit
    // NOTE: We are using Chrome for now. Make sure to update all APIs if you add a toggle for this.

    // Return trail.
    return trail;

    // If the navigator compatibility mode is Gecko
    //    If trail starts with "5.0 (Windows", then return "5.0 (Windows)".
    //    Otherwise, return the prefix of trail up to but not including the first U+003B (;), concatenated with the
    //        character U+0029 RIGHT PARENTHESIS. For example, "5.0 (Macintosh)", "5.0 (Android 10)", or "5.0 (X11)".
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-platform
DeprecatedString NavigatorIDMixin::platform() const
{
    // Must return a string representing the platform on which the browser is executing (e.g. "MacIntel", "Win32",
    // "Linux x86_64", "Linux armv81") or, for privacy and compatibility, a string that is commonly returned on another
    // platform.

    // FIXME: Use some portion of the user agent string to make spoofing work 100%
    return ResourceLoader::the().platform();
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-useragent
DeprecatedString NavigatorIDMixin::user_agent() const
{
    // Must return the default `User-Agent` value.
    return ResourceLoader::the().user_agent();
}

}
