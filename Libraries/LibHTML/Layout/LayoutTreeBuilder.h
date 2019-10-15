#pragma once

#include <AK/RefPtr.h>

class Node;
class LayoutNode;

class LayoutTreeBuilder {
public:
    LayoutTreeBuilder();

    RefPtr<LayoutNode> build(Node&);
};
