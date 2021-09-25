/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/DOMParser.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>

namespace Web::HTML {

DOMParser::DOMParser()
{
}

DOMParser::~DOMParser()
{
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-domparser-parsefromstring
NonnullRefPtr<DOM::Document> DOMParser::parse_from_string(String const& string, String const& type)
{
    // FIXME: Pass in this's relevant global object's associated Document's URL.
    auto document = DOM::Document::create();
    document->set_content_type(type);

    // NOTE: This isn't a case insensitive match since the DOMParserSupportedType enum enforces an all lowercase type.
    if (type == "text/html") {
        // FIXME: Set document's type to "html".
        HTMLParser parser(document, string, "UTF-8");
        // FIXME: This is to match the default URL. Instead, pass in this's relevant global object's associated Document's URL.
        parser.run("about:blank");
    } else {
        dbgln("DOMParser::parse_from_string: Unimplemented parser for type: {}", type);
        TODO();
    }

    return document;
}

}
