#pragma once

#include <LibHTML/LayoutNode.h>
#include <LibHTML/Document.h>

class LayoutDocument : public LayoutNode {
public:
    explicit LayoutDocument(const Document&);
    virtual ~LayoutDocument() override;

    const Document& node() const { return static_cast<const Document&>(*LayoutNode::node()); }

    virtual const char* class_name() const override { return "LayoutDocument"; }

private:
};
