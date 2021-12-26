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

#include <AK/String.h>
#include <LibHTML/DOM/ParentNode.h>
#include <LibHTML/Layout/LayoutNode.h>

class LayoutNodeWithStyle;

class Attribute {
public:
    Attribute(const String& name, const String& value)
        : m_name(name)
        , m_value(value)
    {
    }

    const String& name() const { return m_name; }
    const String& value() const { return m_value; }

    void set_value(const String& value) { m_value = value; }

private:
    String m_name;
    String m_value;
};

class Element : public ParentNode {
public:
    Element(Document&, const String& tag_name);
    virtual ~Element() override;

    virtual String tag_name() const final { return m_tag_name; }

    bool has_attribute(const String& name) const { return !attribute(name).is_null(); }
    String attribute(const String& name) const;
    void set_attribute(const String& name, const String& value);

    void set_attributes(Vector<Attribute>&&);

    template<typename Callback>
    void for_each_attribute(Callback callback) const
    {
        for (auto& attribute : m_attributes)
            callback(attribute.name(), attribute.value());
    }

    bool has_class(const StringView&) const;

    virtual void apply_presentational_hints(StyleProperties&) const {}
    virtual void parse_attribute(const String& name, const String& value);

    void recompute_style();

    LayoutNodeWithStyle* layout_node() { return static_cast<LayoutNodeWithStyle*>(Node::layout_node()); }
    const LayoutNodeWithStyle* layout_node() const { return static_cast<const LayoutNodeWithStyle*>(Node::layout_node()); }

    String name() const { return attribute("name"); }

    const StyleProperties* resolved_style() const { return m_resolved_style.ptr(); }
    NonnullRefPtr<StyleProperties> computed_style();

private:
    RefPtr<LayoutNode> create_layout_node(const StyleProperties* parent_style) const override;

    Attribute* find_attribute(const String& name);
    const Attribute* find_attribute(const String& name) const;

    String m_tag_name;
    Vector<Attribute> m_attributes;

    RefPtr<StyleProperties> m_resolved_style;
};

template<>
inline bool is<Element>(const Node& node)
{
    return node.is_element();
}
