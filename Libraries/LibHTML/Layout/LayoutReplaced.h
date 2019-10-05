#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutNode.h>

class LayoutReplaced : public LayoutNode {
public:
    LayoutReplaced(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutReplaced() override;

    const Element& node() const { return static_cast<const Element&>(*LayoutNode::node()); }

    virtual bool is_replaced() const final { return true; }

private:
    virtual const char* class_name() const override { return "LayoutReplaced"; }

    virtual void split_into_lines(LayoutBlock& container) override;
};
