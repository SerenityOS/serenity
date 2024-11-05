/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Selector.h>

namespace Web::CSS {

class CSSRule : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(CSSRule, Bindings::PlatformObject);

public:
    virtual ~CSSRule() = default;

    // https://drafts.csswg.org/cssom/#dom-cssrule-type
    enum class Type : u16 {
        Style = 1,
        Import = 3,
        Media = 4,
        FontFace = 5,
        Keyframes = 7,
        Keyframe = 8,
        Namespace = 10,
        Supports = 12,
        // AD-HOC: These are not included in the spec, but we need them internally. So, their numbers are arbitrary.
        LayerBlock = 100,
        LayerStatement = 101,
    };

    virtual Type type() const = 0;

    String css_text() const;
    void set_css_text(StringView);

    CSSRule* parent_rule() { return m_parent_rule.ptr(); }
    CSSRule const* parent_rule() const { return m_parent_rule.ptr(); }
    void set_parent_rule(CSSRule*);

    CSSStyleSheet* parent_style_sheet() { return m_parent_style_sheet.ptr(); }
    virtual void set_parent_style_sheet(CSSStyleSheet*);

    template<typename T>
    bool fast_is() const = delete;

protected:
    explicit CSSRule(JS::Realm&);

    virtual String serialized() const = 0;

    virtual void visit_edges(Cell::Visitor&) override;

    [[nodiscard]] FlyString const& parent_layer_internal_qualified_name() const
    {
        if (!m_cached_layer_name.has_value())
            return parent_layer_internal_qualified_name_slow_case();
        return m_cached_layer_name.value();
    }

    [[nodiscard]] FlyString const& parent_layer_internal_qualified_name_slow_case() const;

    JS::GCPtr<CSSRule> m_parent_rule;
    JS::GCPtr<CSSStyleSheet> m_parent_style_sheet;

    mutable Optional<FlyString> m_cached_layer_name;
};

}
