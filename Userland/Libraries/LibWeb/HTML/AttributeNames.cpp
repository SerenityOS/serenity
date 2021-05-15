/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/AttributeNames.h>

namespace Web {
namespace HTML {
namespace AttributeNames {

#define __ENUMERATE_HTML_ATTRIBUTE(name) FlyString name;
ENUMERATE_HTML_ATTRIBUTES
#undef __ENUMERATE_HTML_ATTRIBUTE

[[gnu::constructor]] static void initialize()
{
    static bool s_initialized = false;
    if (s_initialized)
        return;

#define __ENUMERATE_HTML_ATTRIBUTE(name) \
    name = #name;
    ENUMERATE_HTML_ATTRIBUTES
#undef __ENUMERATE_HTML_ATTRIBUTE

    // NOTE: Special cases for C++ keywords.
    class_ = "class";
    for_ = "for";
    default_ = "default";
    char_ = "char";

    // NOTE: Special cases for attributes with dashes in them.
    accept_charset = "accept-charset";
    http_equiv = "http-equiv";

    s_initialized = true;
}

}
}
}
