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

namespace Web::HTML {

class HTMLInputElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLInputElementWrapper;

    HTMLInputElement(DOM::Document&, const FlyString& local_name);
    virtual ~HTMLInputElement() override;

    virtual RefPtr<LayoutNode> create_layout_node(const CSS::StyleProperties* parent_style) override;

    String type() const { return attribute(HTML::AttributeNames::type); }
    String value() const { return attribute(HTML::AttributeNames::value); }
    String name() const { return attribute(HTML::AttributeNames::name); }

    bool checked() const { return m_checked; }
    void set_checked(bool);

    bool enabled() const;

    void did_click_button(Badge<LayoutButton>);

private:
    bool m_checked { false };
};

}

AK_BEGIN_TYPE_TRAITS(Web::HTML::HTMLInputElement)
static bool is_type(const Web::DOM::Node& node) { return node.is_html_element() && downcast<Web::HTML::HTMLElement>(node).local_name() == Web::HTML::TagNames::input; }
AK_END_TYPE_TRAITS()
