/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/DOMImplementation.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Origin.h>

namespace Web::DOM {

DOMImplementation::DOMImplementation(Document& document)
    : RefCountForwarder(document)
{
}

// https://dom.spec.whatwg.org/#dom-domimplementation-createdocument
NonnullRefPtr<Document> DOMImplementation::create_document(const String& namespace_, const String& qualified_name) const
{
    // FIXME: This should specifically be an XML document.
    auto xml_document = Document::create();

    xml_document->set_ready_for_post_load_tasks(true);

    RefPtr<Element> element;

    if (!qualified_name.is_empty())
        element = xml_document->create_element_ns(namespace_, qualified_name /* FIXME: and an empty dictionary */);

    // FIXME: If doctype is non-null, append doctype to document.

    if (element)
        xml_document->append_child(element.release_nonnull());

    xml_document->set_origin(document().origin());

    if (namespace_ == Namespace::HTML)
        xml_document->set_content_type("application/xhtml+xml");
    else if (namespace_ == Namespace::SVG)
        xml_document->set_content_type("image/svg+xml");
    else
        xml_document->set_content_type("application/xml");

    return xml_document;
}

// https://dom.spec.whatwg.org/#dom-domimplementation-createhtmldocument
NonnullRefPtr<Document> DOMImplementation::create_html_document(const String& title) const
{
    // FIXME: This should specifically be a HTML document.
    auto html_document = Document::create();

    html_document->set_content_type("text/html");
    html_document->set_ready_for_post_load_tasks(true);

    auto doctype = adopt_ref(*new DocumentType(html_document));
    doctype->set_name("html");
    html_document->append_child(doctype);

    auto html_element = create_element(html_document, HTML::TagNames::html, Namespace::HTML);
    html_document->append_child(html_element);

    auto head_element = create_element(html_document, HTML::TagNames::head, Namespace::HTML);
    html_element->append_child(head_element);

    if (!title.is_null()) {
        auto title_element = create_element(html_document, HTML::TagNames::title, Namespace::HTML);
        head_element->append_child(title_element);

        auto text_node = adopt_ref(*new Text(html_document, title));
        title_element->append_child(text_node);
    }

    auto body_element = create_element(html_document, HTML::TagNames::body, Namespace::HTML);
    html_element->append_child(body_element);

    html_document->set_origin(document().origin());

    return html_document;
}

// https://dom.spec.whatwg.org/#dom-domimplementation-createdocumenttype
NonnullRefPtr<DocumentType> DOMImplementation::create_document_type(String const& qualified_name, String const& public_id, String const& system_id)
{
    // FIXME: Validate qualified_name.
    auto document_type = DocumentType::create(document());
    document_type->set_name(qualified_name);
    document_type->set_public_id(public_id);
    document_type->set_system_id(system_id);
    return document_type;
}

}
