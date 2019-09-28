#include "ManualSectionNode.h"
#include "ManualPageNode.h"
#include <AK/FileSystemPath.h>
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
        FileSystemPath file_path(dir_iter.next_path());
        if (file_path.extension() != "md")
            continue;
        String page_name = file_path.title();
        NonnullOwnPtr<ManualNode> child = make<ManualPageNode>(*this, move(page_name));
        m_children.append(move(child));
    }
}
