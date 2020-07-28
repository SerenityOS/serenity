/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Loader/Resource.h>

namespace Web::HTML {

class HTMLLinkElement final
    : public HTMLElement
    , public ResourceClient {
public:
    using WrapperType = Bindings::HTMLLinkElementWrapper;

    HTMLLinkElement(DOM::Document&, const FlyString& local_name);
    virtual ~HTMLLinkElement() override;

    virtual void inserted_into(Node&) override;

    String rel() const { return attribute(HTML::AttributeNames::rel); }
    String type() const { return attribute(HTML::AttributeNames::type); }
    String href() const { return attribute(HTML::AttributeNames::href); }

private:
    // ^ResourceClient
    virtual void resource_did_fail() override;
    virtual void resource_did_load() override;

    void parse_attribute(const FlyString&, const String&) override;

    void load_stylesheet(const URL&);

    struct Relationship {
        enum {
            Alternate = 1 << 0,
            Stylesheet = 1 << 1,
        };
    };

    unsigned m_relationship { 0 };
    RefPtr<CSS::StyleSheet> m_style_sheet;
};

}

AK_BEGIN_TYPE_TRAITS(Web::HTML::HTMLLinkElement)
static bool is_type(const Web::DOM::Node& node) { return node.is_html_element() && downcast<Web::HTML::HTMLElement>(node).local_name() == Web::HTML::TagNames::link; }
AK_END_TYPE_TRAITS()
