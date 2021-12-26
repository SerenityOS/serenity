#include "ProfileModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>
#include <ctype.h>
#include <stdio.h>

ProfileModel::ProfileModel(Profile& profile)
    : m_profile(profile)
{
    m_frame_icon.set_bitmap_for_size(16, GraphicsBitmap::load_from_file("/res/icons/16x16/inspector-object.png"));
}

ProfileModel::~ProfileModel()
{
}

GModelIndex ProfileModel::index(int row, int column, const GModelIndex& parent) const
{
    if (!parent.is_valid()) {
        if (m_profile.roots().is_empty())
            return {};
        return create_index(row, column, &m_profile.roots().at(row));
    }
    auto& remote_parent = *static_cast<ProfileNode*>(parent.internal_data());
    return create_index(row, column, remote_parent.children().at(row).ptr());
}

GModelIndex ProfileModel::parent_index(const GModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = *static_cast<ProfileNode*>(index.internal_data());
    if (!node.parent())
        return {};

    // NOTE: If the parent has no parent, it's a root, so we have to look among the remote roots.
    if (!node.parent()->parent()) {
        for (int row = 0; row < m_profile.roots().size(); ++row) {
            if (&m_profile.roots()[row] == node.parent())
                return create_index(row, 0, node.parent());
        }
        ASSERT_NOT_REACHED();
        return {};
    }

    for (int row = 0; row < node.parent()->parent()->children().size(); ++row) {
        if (node.parent()->parent()->children()[row].ptr() == node.parent())
            return create_index(row, 0, node.parent());
    }

    ASSERT_NOT_REACHED();
    return {};
}

int ProfileModel::row_count(const GModelIndex& index) const
{
    if (!index.is_valid())
        return m_profile.roots().size();
    auto& node = *static_cast<ProfileNode*>(index.internal_data());
    return node.children().size();
}

int ProfileModel::column_count(const GModelIndex&) const
{
    return 1;
}

GVariant ProfileModel::data(const GModelIndex& index, Role role) const
{
    auto* node = static_cast<ProfileNode*>(index.internal_data());
    if (role == Role::Icon) {
        return m_frame_icon;
    }
    if (role == Role::Display) {
        return String::format("%s (%u)", node->symbol().characters(), node->sample_count());
    }
    return {};
}

void ProfileModel::update()
{
    did_update();
}
