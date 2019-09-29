#include "ManualModel.h"
#include "ManualNode.h"
#include "ManualPageNode.h"
#include "ManualSectionNode.h"
#include <LibDraw/PNGLoader.h>

static ManualSectionNode s_sections[] = {
    { "1", "Command-line programs" },
    { "2", "System calls" },
    { "3", "Libraries" },
    { "4", "Special files" },
    { "5", "File formats" },
    { "6", "Games" },
    { "7", "Miscellanea" },
    { "8", "Sysadmin tools" }
};

ManualModel::ManualModel()
{
    // FIXME: need some help from the icon fairy ^)
    m_section_icon.set_bitmap_for_size(16, load_png("/res/icons/16x16/book.png"));
    m_page_icon.set_bitmap_for_size(16, load_png("/res/icons/16x16/filetype-unknown.png"));
}

String ManualModel::page_path(const GModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    if (!node->is_page())
        return {};
    auto* page = static_cast<const ManualPageNode*>(node);
    return page->path();
}

String ManualModel::page_and_section(const GModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    if (!node->is_page())
        return {};
    auto* page = static_cast<const ManualPageNode*>(node);
    auto* section = static_cast<const ManualSectionNode*>(page->parent());
    return String::format("%s(%s)", page->name().characters(), section->section_name().characters());
}

GModelIndex ManualModel::index(int row, int column, const GModelIndex& parent_index) const
{
    if (!parent_index.is_valid())
        return create_index(row, column, &s_sections[row]);
    auto* parent = static_cast<const ManualNode*>(parent_index.internal_data());
    auto* child = &parent->children()[row];
    return create_index(row, column, child);
}

GModelIndex ManualModel::parent_index(const GModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* child = static_cast<const ManualNode*>(index.internal_data());
    auto* parent = child->parent();
    if (parent == nullptr)
        return {};

    if (parent->parent() == nullptr) {
        for (size_t row = 0; row < sizeof(s_sections) / sizeof(s_sections[0]); row++)
            if (&s_sections[row] == parent)
                return create_index(row, 0, parent);
        ASSERT_NOT_REACHED();
    }
    for (int row = 0; row < parent->parent()->children().size(); row++) {
        ManualNode* child_at_row = &parent->parent()->children()[row];
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    ASSERT_NOT_REACHED();
}

int ManualModel::row_count(const GModelIndex& index) const
{
    if (!index.is_valid())
        return sizeof(s_sections) / sizeof(s_sections[0]);
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    return node->children().size();
}

int ManualModel::column_count(const GModelIndex&) const
{
    return 1;
}

GVariant ManualModel::data(const GModelIndex& index, Role role) const
{
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    switch (role) {
    case Role::Display:
        return node->name();
    case Role::Icon:
        if (node->is_page())
            return m_page_icon;
        return m_section_icon;
    default:
        return {};
    }
}

void ManualModel::update()
{
    did_update();
}
