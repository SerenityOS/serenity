#include <LibHTML/DOM/CharacterData.h>

CharacterData::CharacterData(Document& document, NodeType type, const String& data)
    : Node(document, type)
    , m_data(data)
{
}

CharacterData::~CharacterData()
{
}
