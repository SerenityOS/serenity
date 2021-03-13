/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/FlyString.h>
#include <AK/String.h>
#include <LibWeb/DOM/Attribute.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/NonDocumentTypeChildNode.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/TagNames.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/QualifiedName.h>

namespace Web::DOM {

class Element
    : public ParentNode
    , public NonDocumentTypeChildNode<Element> {

public:
    using WrapperType = Bindings::ElementWrapper;

    Element(Document&, QualifiedName);
    virtual ~Element() override;

    virtual FlyString node_name() const final { return m_qualified_name.local_name(); }
    const FlyString& local_name() const { return m_qualified_name.local_name(); }

    // NOTE: This is for the JS bindings
    const FlyString& tag_name() const { return local_name(); }

    const FlyString& namespace_() const { return m_qualified_name.namespace_(); }

    // NOTE: This is for the JS bindings
    const FlyString& namespace_uri() const { return namespace_(); }

    bool has_attribute(const FlyString& name) const { return !attribute(name).is_null(); }
    bool has_attributes() const { return !m_attributes.is_empty(); }
    String attribute(const FlyString& name) const;
    String get_attribute(const FlyString& name) const { return attribute(name); }
    ExceptionOr<void> set_attribute(const FlyString& name, const String& value);
    void remove_attribute(const FlyString& name);

    template<typename Callback>
    void for_each_attribute(Callback callback) const
    {
        for (auto& attribute : m_attributes)
            callback(attribute.name(), attribute.value());
    }

    bool has_class(const FlyString&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    const Vector<FlyString>& class_names() const { return m_classes; }

    virtual void apply_presentational_hints(CSS::StyleProperties&) const { }
    virtual void parse_attribute(const FlyString& name, const String& value);

    void recompute_style();

    Layout::NodeWithStyle* layout_node() { return static_cast<Layout::NodeWithStyle*>(Node::layout_node()); }
    const Layout::NodeWithStyle* layout_node() const { return static_cast<const Layout::NodeWithStyle*>(Node::layout_node()); }

    String name() const { return attribute(HTML::AttributeNames::name); }

    const CSS::StyleProperties* specified_css_values() const { return m_specified_css_values.ptr(); }
    NonnullRefPtr<CSS::StyleProperties> computed_style();

    const CSS::CSSStyleDeclaration* inline_style() const { return m_inline_style; }

    NonnullRefPtr<CSS::CSSStyleDeclaration> style_for_bindings();

    // FIXME: innerHTML also appears on shadow roots. https://w3c.github.io/DOM-Parsing/#dom-innerhtml
    String inner_html() const;
    void set_inner_html(StringView);

    bool is_focused() const;
    virtual bool is_focusable() const { return false; }

    NonnullRefPtrVector<Element> get_elements_by_tag_name(const FlyString&) const;
    NonnullRefPtrVector<Element> get_elements_by_class_name(const FlyString&) const;

    ShadowRoot* shadow_root() { return m_shadow_root; }
    const ShadowRoot* shadow_root() const { return m_shadow_root; }
    void set_shadow_root(RefPtr<ShadowRoot>);

protected:
    RefPtr<Layout::Node> create_layout_node() override;

private:
    Attribute* find_attribute(const FlyString& name);
    const Attribute* find_attribute(const FlyString& name) const;

    QualifiedName m_qualified_name;
    Vector<Attribute> m_attributes;

    RefPtr<CSS::CSSStyleDeclaration> m_inline_style;

    RefPtr<CSS::StyleProperties> m_specified_css_values;

    Vector<FlyString> m_classes;

    RefPtr<ShadowRoot> m_shadow_root;
};

template<>
inline bool Node::fast_is<Element>() const { return is_element(); }

}
