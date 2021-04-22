/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

Custody::Custody(Custody* parent, const StringView& name, Inode& inode, int mount_flags)
    : m_parent(parent)
    , m_name(name)
    , m_inode(inode)
    , m_mount_flags(mount_flags)
{
}

Custody::~Custody()
{
}

String Custody::absolute_path() const
{
    if (!parent())
        return "/";
    Vector<const Custody*, 32> custody_chain;
    for (auto* custody = this; custody; custody = custody->parent())
        custody_chain.append(custody);
    StringBuilder builder;
    for (int i = custody_chain.size() - 2; i >= 0; --i) {
        builder.append('/');
        builder.append(custody_chain[i]->name().characters());
    }
    return builder.to_string();
}

bool Custody::is_readonly() const
{
    if (m_mount_flags & MS_RDONLY)
        return true;

    return m_inode->fs().is_readonly();
}

}
