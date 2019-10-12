#include <LibHTML/DOM/Comment.h>
#include <LibHTML/Layout/LayoutText.h>

Comment::Comment(Document& document, const String& data)
    : CharacterData(document, NodeType::COMMENT_NODE, data)
{
}

Comment::~Comment()
{
}
