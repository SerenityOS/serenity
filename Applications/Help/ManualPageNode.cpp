#include "ManualPageNode.h"
#include "ManualSectionNode.h"

const ManualNode* ManualPageNode::parent() const
{
    return &m_section;
}

NonnullOwnPtrVector<ManualNode>& ManualPageNode::children() const
{
    static NonnullOwnPtrVector<ManualNode> empty_vector;
    return empty_vector;
}

String ManualPageNode::path() const
{
    return String::format("%s/%s.md", m_section.path().characters(), m_page.characters());
}
