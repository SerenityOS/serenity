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

KResultOr<NonnullRefPtr<Custody>> Custody::try_create(Custody* parent, StringView name, Inode& inode, int mount_flags)
{
    auto name_kstring = KString::try_create(name);
    if (!name_kstring)
        return ENOMEM;
    auto custody = adopt_ref_if_nonnull(new Custody(parent, name_kstring.release_nonnull(), inode, mount_flags));
    if (!custody)
        return ENOMEM;
    return custody.release_nonnull();
}

Custody::Custody(Custody* parent, NonnullOwnPtr<KString> name, Inode& inode, int mount_flags)
    : m_parent(parent)
    , m_name(move(name))
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
        builder.append(custody_chain[i]->name());
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
