/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Iterator.h>
#include <AK/RefPtr.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom/#the-cssrulelist-interface
class CSSRuleList : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(CSSRuleList, Bindings::LegacyPlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<CSSRuleList>> create(JS::Realm&, JS::MarkedVector<CSSRule*> const&);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<CSSRuleList>> create_empty(JS::Realm&);

    ~CSSRuleList() = default;

    CSSRule const* item(size_t index) const
    {
        if (index >= length())
            return nullptr;
        return m_rules[index];
    }

    CSSRule* item(size_t index)
    {
        if (index >= length())
            return nullptr;
        return m_rules[index];
    }

    size_t length() const { return m_rules.size(); }

    auto begin() const { return m_rules.begin(); }
    auto begin() { return m_rules.begin(); }

    auto end() const { return m_rules.end(); }
    auto end() { return m_rules.end(); }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;

    WebIDL::ExceptionOr<void> remove_a_css_rule(u32 index);
    WebIDL::ExceptionOr<unsigned> insert_a_css_rule(Variant<StringView, CSSRule*>, u32 index);

    void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const;
    // Returns whether the match state of any media queries changed after evaluation.
    bool evaluate_media_queries(HTML::Window const&);
    void for_each_effective_keyframes_at_rule(Function<void(CSSKeyframesRule const&)> const& callback) const;

    Function<void()> on_change;

private:
    explicit CSSRuleList(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // ^Bindings::LegacyPlatformObject
    virtual bool supports_indexed_properties() const override { return true; }
    virtual bool supports_named_properties() const override { return false; }
    virtual bool has_indexed_property_setter() const override { return false; }
    virtual bool has_named_property_setter() const override { return false; }
    virtual bool has_named_property_deleter() const override { return false; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const override { return false; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const override { return false; }
    virtual bool has_global_interface_extended_attribute() const override { return false; }
    virtual bool indexed_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_deleter_has_identifier() const override { return false; }

    Vector<JS::NonnullGCPtr<CSSRule>> m_rules;
};

}
