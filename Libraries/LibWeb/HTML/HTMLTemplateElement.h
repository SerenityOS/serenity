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

#pragma once

#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTemplateElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTemplateElementWrapper;

    HTMLTemplateElement(DOM::Document&, const FlyString& local_name);
    virtual ~HTMLTemplateElement() override;

    NonnullRefPtr<DOM::DocumentFragment> content() { return *m_content; }
    const NonnullRefPtr<DOM::DocumentFragment> content() const { return *m_content; }

private:
    DOM::Document& appropriate_template_contents_owner_document(DOM::Document&);

    RefPtr<DOM::DocumentFragment> m_content;
};

}

AK_BEGIN_TYPE_TRAITS(Web::HTML::HTMLTemplateElement)
static bool is_type(const Web::DOM::Node& node) { return node.is_html_element() && downcast<Web::HTML::HTMLElement>(node).local_name() == Web::HTML::TagNames::template_; }
AK_END_TYPE_TRAITS()
