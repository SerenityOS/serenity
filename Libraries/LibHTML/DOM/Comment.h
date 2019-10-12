#pragma once

#include <AK/String.h>
#include <LibHTML/DOM/CharacterData.h>

class Comment final : public CharacterData {
public:
    explicit Comment(Document&, const String&);
    virtual ~Comment() override;

    virtual String tag_name() const override { return "#comment"; }
};

template<>
inline bool is<Comment>(const Node& node)
{
    return node.is_comment();
}
