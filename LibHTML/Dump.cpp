#include <LibHTML/Document.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Element.h>
#include <LibHTML/LayoutNode.h>
#include <LibHTML/LayoutText.h>
#include <LibHTML/Text.h>
#include <stdio.h>

void dump_tree(const Node& node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        printf("   ");
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

void dump_tree(const LayoutNode& node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        printf("   ");
    printf("%s{%p}", node.class_name(), &node);
    if (node.is_text())
        printf(" \"%s\"", static_cast<const LayoutText&>(node).text().characters());
    printf("\n");
    ++indent;
    node.for_each_child([](auto& child) {
        dump_tree(child);
    });
    --indent;
}
