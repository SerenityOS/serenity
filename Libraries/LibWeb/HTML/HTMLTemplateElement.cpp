/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>

namespace Web::HTML {

HTMLTemplateElement::HTMLTemplateElement(DOM::Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
    m_content = adopt(*new DOM::DocumentFragment(appropriate_template_contents_owner_document(document)));
    m_content->set_host(*this);
}

HTMLTemplateElement::~HTMLTemplateElement()
{
}

DOM::Document& HTMLTemplateElement::appropriate_template_contents_owner_document(DOM::Document& document)
{
    if (!document.created_for_appropriate_template_contents()) {
        if (!document.associated_inert_template_document()) {
            DOM::Document new_document;
            new_document.set_created_for_appropriate_template_contents(true);

            // FIXME: If doc is an HTML document, mark new doc as an HTML document also.

            document.set_associated_inert_template_document(new_document);
        }

        return *document.associated_inert_template_document();
    }

    return document;
}

}
