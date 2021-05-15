/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>

namespace Web::HTML {

HTMLTemplateElement::HTMLTemplateElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    m_content = adopt_ref(*new DOM::DocumentFragment(appropriate_template_contents_owner_document(document)));
    m_content->set_host(*this);
}

HTMLTemplateElement::~HTMLTemplateElement()
{
}

DOM::Document& HTMLTemplateElement::appropriate_template_contents_owner_document(DOM::Document& document)
{
    if (!document.created_for_appropriate_template_contents()) {
        if (!document.associated_inert_template_document()) {
            auto new_document = DOM::Document::create();
            new_document->set_created_for_appropriate_template_contents(true);

            // FIXME: If doc is an HTML document, mark new doc as an HTML document also.

            document.set_associated_inert_template_document(new_document);
        }

        return *document.associated_inert_template_document();
    }

    return document;
}

}
