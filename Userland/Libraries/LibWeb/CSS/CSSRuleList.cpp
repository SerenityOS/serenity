/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibWeb/Bindings/CSSRuleListPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSKeyframesRule.h>
#include <LibWeb/CSS/CSSMediaRule.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSRuleList.h>
#include <LibWeb/CSS/CSSSupportsRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

JS::NonnullGCPtr<CSSRuleList> CSSRuleList::create(JS::Realm& realm, JS::MarkedVector<CSSRule*> const& rules)
{
    auto rule_list = realm.heap().allocate<CSSRuleList>(realm, realm);
    for (auto* rule : rules)
        rule_list->m_rules.append(*rule);
    return rule_list;
}

CSSRuleList::CSSRuleList(JS::Realm& realm)
    : Bindings::LegacyPlatformObject(realm)
{
}

JS::NonnullGCPtr<CSSRuleList> CSSRuleList::create_empty(JS::Realm& realm)
{
    return realm.heap().allocate<CSSRuleList>(realm, realm);
}

void CSSRuleList::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSRuleListPrototype>(realm, "CSSRuleList"));
}

void CSSRuleList::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& rule : m_rules)
        visitor.visit(rule);
}

bool CSSRuleList::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of CSSRule objects represented by the collection.
    // If there are no such CSSRule objects, then there are no supported property indices.
    return index < m_rules.size();
}

// https://www.w3.org/TR/cssom/#insert-a-css-rule
WebIDL::ExceptionOr<unsigned> CSSRuleList::insert_a_css_rule(Variant<StringView, CSSRule*> rule, u32 index)
{
    // 1. Set length to the number of items in list.
    auto length = m_rules.size();

    // 2. If index is greater than length, then throw an IndexSizeError exception.
    if (index > length)
        return WebIDL::IndexSizeError::create(realm(), "CSS rule index out of bounds."_fly_string);

    // 3. Set new rule to the results of performing parse a CSS rule on argument rule.
    // NOTE: The insert-a-css-rule spec expects `rule` to be a string, but the CSSStyleSheet.insertRule()
    //       spec calls this algorithm with an already-parsed CSSRule. So, we use a Variant and skip step 3
    //       if that variant holds a CSSRule already.
    CSSRule* new_rule = nullptr;
    if (rule.has<StringView>()) {
        new_rule = parse_css_rule(
            CSS::Parser::ParsingContext { realm() },
            rule.get<StringView>());
    } else {
        new_rule = rule.get<CSSRule*>();
    }

    // 4. If new rule is a syntax error, throw a SyntaxError exception.
    if (!new_rule)
        return WebIDL::SyntaxError::create(realm(), "Unable to parse CSS rule."_fly_string);

    // FIXME: 5. If new rule cannot be inserted into list at the zero-index position index due to constraints specified by CSS, then throw a HierarchyRequestError exception. [CSS21]

    // FIXME: 6. If new rule is an @namespace at-rule, and list contains anything other than @import at-rules, and @namespace at-rules, throw an InvalidStateError exception.

    // 7. Insert new rule into list at the zero-indexed position index.
    m_rules.insert(index, *new_rule);

    // 8. Return index.
    if (on_change)
        on_change();
    return index;
}

// https://www.w3.org/TR/cssom/#remove-a-css-rule
WebIDL::ExceptionOr<void> CSSRuleList::remove_a_css_rule(u32 index)
{
    // 1. Set length to the number of items in list.
    auto length = m_rules.size();

    // 2. If index is greater than or equal to length, then throw an IndexSizeError exception.
    if (index >= length)
        return WebIDL::IndexSizeError::create(realm(), "CSS rule index out of bounds."_fly_string);

    // 3. Set old rule to the indexth item in list.
    CSSRule& old_rule = m_rules[index];

    // FIXME: 4. If old rule is an @namespace at-rule, and list contains anything other than @import at-rules, and @namespace at-rules, throw an InvalidStateError exception.

    // 5. Remove rule old rule from list at the zero-indexed position index.
    m_rules.remove(index);

    // 6. Set old rule’s parent CSS rule and parent CSS style sheet to null.
    old_rule.set_parent_rule(nullptr);
    old_rule.set_parent_style_sheet(nullptr);

    if (on_change)
        on_change();
    return {};
}

void CSSRuleList::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    for (auto const& rule : m_rules) {
        switch (rule->type()) {
        case CSSRule::Type::FontFace:
            break;
        case CSSRule::Type::Import: {
            auto const& import_rule = static_cast<CSSImportRule const&>(*rule);
            if (import_rule.loaded_style_sheet())
                import_rule.loaded_style_sheet()->for_each_effective_style_rule(callback);
            break;
        }
        case CSSRule::Type::Media:
            static_cast<CSSMediaRule const&>(*rule).for_each_effective_style_rule(callback);
            break;
        case CSSRule::Type::Style:
            callback(static_cast<CSSStyleRule const&>(*rule));
            break;
        case CSSRule::Type::Supports:
            static_cast<CSSSupportsRule const&>(*rule).for_each_effective_style_rule(callback);
            break;
        case CSSRule::Type::Keyframe:
        case CSSRule::Type::Keyframes:
        case CSSRule::Type::Namespace:
            break;
        }
    }
}

void CSSRuleList::for_each_effective_keyframes_at_rule(Function<void(CSSKeyframesRule const&)> const& callback) const
{
    for (auto const& rule : m_rules) {
        switch (rule->type()) {
        case CSSRule::Type::FontFace:
            break;
        case CSSRule::Type::Import: {
            auto const& import_rule = static_cast<CSSImportRule const&>(*rule);
            if (import_rule.loaded_style_sheet())
                import_rule.loaded_style_sheet()->for_each_effective_keyframes_at_rule(callback);
            break;
        }
        case CSSRule::Type::Media:
            static_cast<CSSMediaRule const&>(*rule).for_each_effective_keyframes_at_rule(callback);
            break;
        case CSSRule::Type::Style:
            break;
        case CSSRule::Type::Supports:
            static_cast<CSSSupportsRule const&>(*rule).for_each_effective_keyframes_at_rule(callback);
            break;
        case CSSRule::Type::Keyframe:
            break;
        case CSSRule::Type::Keyframes:
            callback(static_cast<CSSKeyframesRule const&>(*rule));
            break;
        case CSSRule::Type::Namespace:
            break;
        }
    }
}

bool CSSRuleList::evaluate_media_queries(HTML::Window const& window)
{
    bool any_media_queries_changed_match_state = false;

    for (auto& rule : m_rules) {
        switch (rule->type()) {
        case CSSRule::Type::FontFace:
            break;
        case CSSRule::Type::Import: {
            auto& import_rule = verify_cast<CSSImportRule>(*rule);
            if (import_rule.loaded_style_sheet() && import_rule.loaded_style_sheet()->evaluate_media_queries(window))
                any_media_queries_changed_match_state = true;
            break;
        }
        case CSSRule::Type::Media: {
            auto& media_rule = verify_cast<CSSMediaRule>(*rule);
            bool did_match = media_rule.condition_matches();
            bool now_matches = media_rule.evaluate(window);
            if (did_match != now_matches)
                any_media_queries_changed_match_state = true;
            if (now_matches && media_rule.css_rules().evaluate_media_queries(window))
                any_media_queries_changed_match_state = true;
            break;
        }
        case CSSRule::Type::Style:
            break;
        case CSSRule::Type::Supports: {
            auto& supports_rule = verify_cast<CSSSupportsRule>(*rule);
            if (supports_rule.condition_matches() && supports_rule.css_rules().evaluate_media_queries(window))
                any_media_queries_changed_match_state = true;
            break;
        }
        case CSSRule::Type::Keyframe:
        case CSSRule::Type::Keyframes:
        case CSSRule::Type::Namespace:
            break;
        }
    }

    return any_media_queries_changed_match_state;
}

WebIDL::ExceptionOr<JS::Value> CSSRuleList::item_value(size_t index) const
{
    return item(index);
}

}
