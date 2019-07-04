#pragma once

#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/DOM/Document.h>

class LayoutDocument final : public LayoutBlock {
public:
    LayoutDocument(const Document&, const StyledNode&);
    virtual ~LayoutDocument() override;

    const Document& node() const { return static_cast<const Document&>(*LayoutNode::node()); }
    virtual const char* class_name() const override { return "LayoutDocument"; }
    virtual void layout() override;
private:
};
