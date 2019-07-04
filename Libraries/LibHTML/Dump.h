#pragma once

class Node;
class LayoutNode;
class StyleRule;
class StyleSheet;
class StyledNode;

void dump_tree(const Node&);
void dump_tree(const StyledNode&);
void dump_tree(const LayoutNode&);
void dump_sheet(const StyleSheet&);
void dump_rule(const StyleRule&);
