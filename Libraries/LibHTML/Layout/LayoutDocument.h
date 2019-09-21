#pragma once

#include <LibHTML/DOM/Document.h>
#include <LibHTML/Layout/LayoutBlock.h>

class LayoutDocument final : public LayoutBlock {
public:
    LayoutDocument(const Document&, StyleProperties&&);
    virtual ~LayoutDocument() override;

    const Document& node() const { return static_cast<const Document&>(*LayoutNode::node()); }
    virtual const char* class_name() const override { return "LayoutDocument"; }
    virtual void layout() override;

private:
};
