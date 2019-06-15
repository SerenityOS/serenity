#pragma once

#include <AK/AKString.h>
#include <LibHTML/DOM/ParentNode.h>

class LayoutNode;

class Document : public ParentNode {
public:
    Document();
    virtual ~Document() override;

    virtual RetainPtr<LayoutNode> create_layout_node() override;

    void build_layout_tree();

private:
};
