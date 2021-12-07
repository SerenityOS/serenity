/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class StyleSheet
    : public RefCounted<StyleSheet>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::StyleSheetWrapper;

    virtual ~StyleSheet() = default;

    virtual String type() const = 0;

    DOM::Element* owner_node() { return m_owner_node; }
    void set_owner_node(DOM::Element*);

    String title() const { return m_title; }
    void set_title(String title) { m_title = move(title); }

    void set_type(String type) { m_type_string = move(type); }
    void set_media(String media) { m_media_string = move(media); }

    bool is_alternate() const { return m_alternate; }
    void set_alternate(bool alternate) { m_alternate = alternate; }

    void set_origin_clean(bool origin_clean) { m_origin_clean = origin_clean; }

    bool disabled() const { return m_disabled; }
    void set_disabled(bool disabled) { m_disabled = disabled; }

    CSSStyleSheet* parent_style_sheet() { return m_parent_style_sheet; }
    void set_parent_css_style_sheet(CSSStyleSheet*);

protected:
    StyleSheet() = default;

private:
    WeakPtr<DOM::Element> m_owner_node;

    WeakPtr<CSSStyleSheet> m_parent_style_sheet;

    String m_title;
    String m_type_string;
    String m_media_string;

    bool m_disabled { false };
    bool m_alternate { false };
    bool m_origin_clean { true };
};

}
