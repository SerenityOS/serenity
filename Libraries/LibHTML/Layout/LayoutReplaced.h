#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutNode.h>

class LayoutReplaced : public LayoutNode {
public:
    LayoutReplaced(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutReplaced();

    const Element& node() const { return static_cast<const Element&>(*LayoutNode::node()); }

private:
};
