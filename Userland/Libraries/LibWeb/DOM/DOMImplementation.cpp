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
    : m_document(document)
{
}

const NonnullRefPtr<Document> DOMImplementation::create_html_document(const String& title) const
{
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

    html_document->set_origin(m_document.origin());

    return html_document;
}

}
