#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Parser/CSSParser.h>
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

#ifdef HTML_DEBUG
    dbgprintf("Rules matching Element{%p}\n", &element);
    for (auto& rule : matching_rules) {
        dump_rule(rule);
    }
#endif

    return matching_rules;
}

static bool is_inherited_property(const StringView& name)
{
    static HashTable<String> inherited_properties;
    if (inherited_properties.is_empty()) {
        inherited_properties.set("border-collapse");
        inherited_properties.set("border-spacing");
        inherited_properties.set("color");
        inherited_properties.set("font-family");
        inherited_properties.set("font-size");
        inherited_properties.set("font-style");
        inherited_properties.set("font-variant");
        inherited_properties.set("font-weight");
        inherited_properties.set("font");
        inherited_properties.set("letter-spacing");
        inherited_properties.set("line-height");
        inherited_properties.set("list-style-image");
        inherited_properties.set("list-style-position");
        inherited_properties.set("list-style-type");
        inherited_properties.set("list-style");
        inherited_properties.set("text-align");
        inherited_properties.set("text-indent");
        inherited_properties.set("text-transform");
        inherited_properties.set("visibility");
        inherited_properties.set("white-space");
        inherited_properties.set("word-spacing");

        // FIXME: This property is not supposed to be inherited, but we currently
        //        rely on inheritance to propagate decorations into line boxes.
        inherited_properties.set("text-decoraton");
    }
    return inherited_properties.contains(name);
}

NonnullRefPtr<StyleProperties> StyleResolver::resolve_style(const Element& element, const StyleProperties* parent_properties) const
{
    auto style_properties = StyleProperties::create();

    if (parent_properties) {
        parent_properties->for_each_property([&](const StringView& name, auto& value) {
            if (is_inherited_property(name))
                style_properties->set_property(name, value);
        });
    }

    element.apply_presentational_hints(*style_properties);

    auto matching_rules = collect_matching_rules(element);
    for (auto& rule : matching_rules) {
        for (auto& property : rule.declaration().properties()) {
            style_properties->set_property(property.name, property.value);
        }
    }

    auto style_attribute = element.attribute("style");
    if (!style_attribute.is_null()) {
        if (auto declaration = parse_css_declaration(style_attribute)) {
            for (auto& property : declaration->properties()) {
                style_properties->set_property(property.name, property.value);
            }
        }
    }

    return style_properties;
}
