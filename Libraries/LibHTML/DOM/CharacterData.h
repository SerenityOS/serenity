#pragma once

#include <AK/String.h>
#include <LibHTML/DOM/Node.h>

class CharacterData : public Node {
public:
    virtual ~CharacterData() override;

    const String& data() const { return m_data; }

    virtual String text_content() const override { return m_data; }

protected:
    explicit CharacterData(Document&, NodeType, const String&);

private:
    String m_data;
};

template<>
inline bool is<CharacterData>(const Node& node)
{
    return node.is_character_data();
}
