#pragma once

#include <AK/String.h>
#include <LibHTML/DOM/CharacterData.h>

class Text final : public CharacterData {
public:
    explicit Text(Document&, const String&);
    virtual ~Text() override;

    virtual String tag_name() const override { return "#text"; }

private:
    virtual RefPtr<LayoutNode> create_layout_node(const StyleProperties* parent_style) const override;
};

template<>
inline bool is<Text>(const Node& node)
{
    return node.is_text();
}
