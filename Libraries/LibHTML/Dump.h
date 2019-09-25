#pragma once

class Node;
class LayoutNode;
class StyleRule;
class StyleSheet;

void dump_tree(const Node&);
void dump_tree(const LayoutNode&);
void dump_sheet(const StyleSheet&);
void dump_rule(const StyleRule&);

#undef HTML_DEBUG
