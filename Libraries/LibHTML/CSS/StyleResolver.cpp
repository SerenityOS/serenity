#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <stdio.h>

StyleResolver::StyleResolver(Document& document)
    : m_document(document)
{
}

StyleResolver::~StyleResolver()
{
}

static bool matches(const Selector& selector, const Element& element)
{
    // FIXME: Support compound selectors.
    ASSERT(selector.components().size() == 1);

    auto& component = selector.components().first();
    switch (component.type) {
    case Selector::Component::Type::Id:
        return component.value == element.attribute("id");
    case Selector::Component::Type::Class:
        return element.has_class(component.value);
    case Selector::Component::Type::TagName:
        return component.value == element.tag_name();
    default:
        ASSERT_NOT_REACHED();
    }
}

NonnullRefPtrVector<StyleRule> StyleResolver::collect_matching_rules(const Element& element) const
{
    NonnullRefPtrVector<StyleRule> matching_rules;
    for (auto& sheet : document().stylesheets()) {
        for (auto& rule : sheet.rules()) {
            for (auto& selector : rule.selectors()) {
                if (matches(selector, element)) {
                    matching_rules.append(rule);
                    break;
                }
            }
        }
    }
    printf("Rules matching Element{%p}\n", &element);
    for (auto& rule : matching_rules) {
        dump_rule(rule);
    }
    return matching_rules;
}

StyleProperties StyleResolver::resolve_style(const Element& element)
{
    StyleProperties style_properties;
    auto matching_rules = collect_matching_rules(element);
    for (auto& rule : matching_rules) {
        for (auto& declaration : rule.declarations()) {
            style_properties.set_property(declaration.property_name(), declaration.value());
        }
    }
    return style_properties;
}
