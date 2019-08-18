#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/CSS/StyledNode.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/Text.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Layout/LayoutText.h>
#include <stdio.h>

void dump_tree(const Node& node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        printf("  ");
    if (node.is_document()) {
        printf("*Document*\n");
    } else if (node.is_element()) {
        printf("<%s", static_cast<const Element&>(node).tag_name().characters());
        static_cast<const Element&>(node).for_each_attribute([](auto& name, auto& value) {
            printf(" %s=%s", name.characters(), value.characters());
        });
        printf(">\n");
    } else if (node.is_text()) {
        printf("\"%s\"\n", static_cast<const Text&>(node).data().characters());
    }
    ++indent;
    if (node.is_parent_node()) {
        static_cast<const ParentNode&>(node).for_each_child([](auto& child) {
            dump_tree(child);
        });
    }
    --indent;
}

void dump_tree(const LayoutNode& layout_node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        printf("  ");

    String tag_name;
    if (layout_node.is_anonymous())
        tag_name = "(anonymous)";
    else if (layout_node.node()->is_text())
        tag_name = "#text";
    else if (layout_node.node()->is_document())
        tag_name = "#document";
    else if (layout_node.node()->is_element())
        tag_name = static_cast<const Element&>(*layout_node.node()).tag_name();
    else
        tag_name = "???";

    printf("%s {%s} at (%d,%d) size %dx%d",
        layout_node.class_name(),
        tag_name.characters(),
        layout_node.rect().x(),
        layout_node.rect().y(),
        layout_node.rect().width(),
        layout_node.rect().height());

    // Dump the horizontal box properties
    printf(" [%d+%d+%d %d %d+%d+%d]",
        layout_node.style().margin().left.to_px(),
        layout_node.style().border().left.to_px(),
        layout_node.style().padding().left.to_px(),
        layout_node.rect().width(),
        layout_node.style().margin().right.to_px(),
        layout_node.style().border().right.to_px(),
        layout_node.style().padding().right.to_px());

    if (layout_node.is_text())
        printf(" \"%s\"", static_cast<const LayoutText&>(layout_node).text().characters());

    printf("\n");
    ++indent;
    layout_node.for_each_child([](auto& child) {
        dump_tree(child);
    });
    --indent;
}

void dump_tree(const StyledNode& styled_node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        printf("    ");

    String tag_name;
    auto& node = *styled_node.node();
    if (node.is_text())
        tag_name = "#text";
    else if (node.is_document())
        tag_name = "#document";
    else if (node.is_element())
        tag_name = static_cast<const Element&>(node).tag_name();
    else
        tag_name = "???";

    printf("%s", tag_name.characters());
    printf("\n");

    styled_node.for_each_property([&](auto& key, auto& value) {
        for (int i = 0; i < indent; ++i)
            printf("    ");
        printf("  (%s: %s)\n", key.characters(), value.to_string().characters());
    });
    ++indent;
    styled_node.for_each_child([](auto& child) {
        dump_tree(child);
    });
    --indent;
}

void dump_rule(const StyleRule& rule)
{
    printf("Rule:\n");
    for (auto& selector : rule.selectors()) {
        printf("  Selector:\n");
        for (auto& component : selector.components()) {
            const char* type_description = "Unknown";
            switch (component.type) {
            case Selector::Component::Type::Invalid:
                type_description = "Invalid";
                break;
            case Selector::Component::Type::Id:
                type_description = "Id";
                break;
            case Selector::Component::Type::Class:
                type_description = "Class";
                break;
            case Selector::Component::Type::TagName:
                type_description = "TagName";
                break;
            }
            printf("    %s:%s\n", type_description, component.value.characters());
        }
    }
    printf("  Declarations:\n");
    for (auto& declaration : rule.declarations()) {
        printf("    '%s': '%s'\n", declaration.property_name().characters(), declaration.value().to_string().characters());
    }
}

void dump_sheet(const StyleSheet& sheet)
{
    printf("StyleSheet{%p}: %d rule(s)\n", &sheet, sheet.rules().size());

    for (auto& rule : sheet.rules()) {
        dump_rule(rule);
    }
}
