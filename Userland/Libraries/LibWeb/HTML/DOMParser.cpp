/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/DOMParserWrapper.h>
#include <LibWeb/HTML/DOMParser.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/XML/XMLDocumentBuilder.h>

namespace Web::HTML {

DOMParser::DOMParser() = default;
DOMParser::~DOMParser() = default;

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-domparser-parsefromstring
NonnullRefPtr<DOM::Document> DOMParser::parse_from_string(String const& string, Bindings::DOMParserSupportedType type)
{
    // 1. Let document be a new Document, whose content type is type and url is this's relevant global object's associated Document's URL.
    // FIXME: Pass in this's relevant global object's associated Document's URL.
    auto document = DOM::Document::create();
    document->set_content_type(Bindings::idl_enum_to_string(type));

    // 2. Switch on type:
    if (type == Bindings::DOMParserSupportedType::Text_Html) {
        // -> "text/html"
        // FIXME: 1. Set document's type to "html".

        // 2. Create an HTML parser parser, associated with document.
        // 3. Place string into the input stream for parser. The encoding confidence is irrelevant.
        // FIXME: We don't have the concept of encoding confidence yet.
        auto parser = HTMLParser::create(document, string, "UTF-8");

        // 4. Start parser and let it run until it has consumed all the characters just inserted into the input stream.
        // FIXME: This is to match the default URL. Instead, pass in this's relevant global object's associated Document's URL.
        parser->run("about:blank");
    } else {
        // -> Otherwise

        // 1. Create an XML parser parse, associated with document, and with XML scripting support disabled.
        XML::Parser parser(string, { .resolve_external_resource = resolve_xml_resource });
        XMLDocumentBuilder builder { document, XMLScriptingSupport::Disabled };
        // 2. Parse string using parser.
        auto result = parser.parse_with_listener(builder);
        // 3. If the previous step resulted in an XML well-formedness or XML namespace well-formedness error, then:
        if (result.is_error() || builder.has_error()) {
            // NOTE: The XML parsing can produce nodes before it hits an error, just remove them.
            // 1. Assert: document has no child nodes.
            document->remove_all_children(true);
            // 2. Let root be the result of creating an element given document, "parsererror", and "http://www.mozilla.org/newlayout/xml/parsererror.xml".
            auto root = DOM::create_element(document, "parsererror", "http://www.mozilla.org/newlayout/xml/parsererror.xml");
            // FIXME: 3. Optionally, add attributes or children to root to describe the nature of the parsing error.
            // 4. Append root to document.
            document->append_child(root);
        }
    }

    // 3. Return document.
    return document;
}

}
