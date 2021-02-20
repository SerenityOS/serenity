/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    auto doctype = adopt(*new DocumentType(html_document));
    doctype->set_name("html");
    html_document->append_child(doctype);

    auto html_element = create_element(html_document, HTML::TagNames::html, Namespace::HTML);
    html_document->append_child(html_element);

    auto head_element = create_element(html_document, HTML::TagNames::head, Namespace::HTML);
    html_element->append_child(head_element);

    if (!title.is_null()) {
        auto title_element = create_element(html_document, HTML::TagNames::title, Namespace::HTML);
        head_element->append_child(title_element);

        auto text_node = adopt(*new Text(html_document, title));
        title_element->append_child(text_node);
    }

    auto body_element = create_element(html_document, HTML::TagNames::body, Namespace::HTML);
    html_element->append_child(body_element);

    html_document->set_origin(m_document.origin());

    return html_document;
}

}
