#include <LibHTML/Document.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Element.h>
#include <LibHTML/Text.h>
#include <stdio.h>

void dump_tree(Node& node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        printf("   ");
    if (node.is_document()) {
        printf("*Document*\n");
    } else if (node.is_element()) {
        printf("<%s>\n", static_cast<Element&>(node).tag_name().characters());
    } else if (node.is_text()) {
        printf("\"%s\"\n", static_cast<Text&>(node).data().characters());
    }
    ++indent;
    if (node.is_parent_node()) {
        static_cast<ParentNode&>(node).for_each_child([](Node& child) {
            dump_tree(child);
        });
    }
    --indent;
}
