#pragma once

class Node;
class LayoutNode;
class StyleSheet;

void dump_tree(const Node&);
void dump_tree(const LayoutNode&);
void dump_sheet(const StyleSheet&);
