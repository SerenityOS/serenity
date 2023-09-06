/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <LibWeb/DOM/CharacterData.h>

namespace Web::DOM {

class Text : public CharacterData {
    WEB_PLATFORM_OBJECT(Text, CharacterData);

public:
    virtual ~Text() override = default;

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Text>> construct_impl(JS::Realm& realm, String const& data);

    // ^Node
    virtual DeprecatedFlyString node_name() const override { return "#text"; }
    virtual bool is_editable() const override { return m_always_editable || CharacterData::is_editable(); }

    void set_always_editable(bool b) { m_always_editable = b; }

    void set_owner_input_element(Badge<HTML::HTMLInputElement>, HTML::HTMLInputElement&);
    HTML::HTMLInputElement* owner_input_element() { return m_owner_input_element.ptr(); }

    WebIDL::ExceptionOr<JS::NonnullGCPtr<Text>> split_text(size_t offset);

    bool is_password_input() const { return m_is_password_input; }
    void set_is_password_input(Badge<HTML::HTMLInputElement>, bool b) { m_is_password_input = b; }

protected:
    Text(Document&, String const&);
    Text(Document&, NodeType, String const&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

private:
    JS::GCPtr<HTML::HTMLInputElement> m_owner_input_element;

    bool m_always_editable { false };
    bool m_is_password_input { false };
};

template<>
inline bool Node::fast_is<Text>() const { return is_text(); }

}
