#include "ManualSectionNode.h"
#include "ManualPageNode.h"
#include <AK/String.h>
#include <LibCore/CDirIterator.h>

String ManualSectionNode::path() const
{
    return String::format("/usr/share/man/man%s", m_section.characters());
}

void ManualSectionNode::reify_if_needed() const
{
    if (m_reified)
        return;
    m_reified = true;

    CDirIterator dir_iter { path(), CDirIterator::Flags::SkipDots };

    while (dir_iter.has_next()) {
        String file_name = dir_iter.next_path();
        ASSERT(file_name.ends_with(".md"));
        String page_name = file_name.substring(0, file_name.length() - 3);
        NonnullOwnPtr<ManualNode> child = make<ManualPageNode>(*this, move(page_name));
        m_children.append(move(child));
    }
}
