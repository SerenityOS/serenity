/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/DOMParserPrototype.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/DOMParser.h>
#include <LibWeb/HTML/HTMLDocument.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/XML/XMLDocumentBuilder.h>

namespace Web::HTML {

WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMParser>> DOMParser::construct_impl(JS::Realm& realm)
{
    return realm.heap().allocate<DOMParser>(realm, realm);
}

DOMParser::DOMParser(JS::Realm& realm)
    : PlatformObject(realm)
{
}

DOMParser::~DOMParser() = default;

void DOMParser::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMParserPrototype>(realm, "DOMParser"));
}

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#dom-domparser-parsefromstring
JS::NonnullGCPtr<DOM::Document> DOMParser::parse_from_string(StringView string, Bindings::DOMParserSupportedType type)
{
    // 1. Let document be a new Document, whose content type is type and url is this's relevant global object's associated Document's URL.
    JS::GCPtr<DOM::Document> document;

    // 2. Switch on type:
    if (type == Bindings::DOMParserSupportedType::Text_Html) {
        // -> "text/html"
        // 1. Set document's type to "html".
        document = HTML::HTMLDocument::create(realm(), verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document().url());
        document->set_content_type(Bindings::idl_enum_to_string(type).to_deprecated_string());
        document->set_document_type(DOM::Document::Type::HTML);

        // 2. Create an HTML parser parser, associated with document.
        // 3. Place string into the input stream for parser. The encoding confidence is irrelevant.
        // FIXME: We don't have the concept of encoding confidence yet.
        auto parser = HTMLParser::create(*document, string, "UTF-8");

        // 4. Start parser and let it run until it has consumed all the characters just inserted into the input stream.
        // FIXME: This is to match the default URL. Instead, pass in this's relevant global object's associated Document's URL.
        parser->run("about:blank"sv);
    } else {
        // -> Otherwise
        document = DOM::Document::create(realm(), verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document().url());
        document->set_content_type(Bindings::idl_enum_to_string(type).to_deprecated_string());

        // 1. Create an XML parser parse, associated with document, and with XML scripting support disabled.
        XML::Parser parser(string, { .resolve_external_resource = resolve_xml_resource });
        XMLDocumentBuilder builder { *document, XMLScriptingSupport::Disabled };
        // 2. Parse string using parser.
        auto result = parser.parse_with_listener(builder);
        // 3. If the previous step resulted in an XML well-formedness or XML namespace well-formedness error, then:
        if (result.is_error() || builder.has_error()) {
            // NOTE: The XML parsing can produce nodes before it hits an error, just remove them.
            // 1. Assert: document has no child nodes.
            document->remove_all_children(true);
            // 2. Let root be the result of creating an element given document, "parsererror", and "http://www.mozilla.org/newlayout/xml/parsererror.xml".
            auto root = DOM::create_element(*document, "parsererror", "http://www.mozilla.org/newlayout/xml/parsererror.xml").release_value_but_fixme_should_propagate_errors();
            // FIXME: 3. Optionally, add attributes or children to root to describe the nature of the parsing error.
            // 4. Append root to document.
            MUST(document->append_child(*root));
        }
    }

    // 3. Return document.
    return *document;
}

}
