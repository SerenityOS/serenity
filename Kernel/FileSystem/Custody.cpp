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
    auto custody = adopt_ref_if_nonnull(new (nothrow) Custody(parent, name_kstring.release_nonnull(), inode, mount_flags));
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

OwnPtr<KString> Custody::try_create_absolute_path() const
{
    if (!parent())
        return KString::try_create("/"sv);

    Vector<Custody const*, 32> custody_chain;
    size_t path_length = 0;
    for (auto* custody = this; custody; custody = custody->parent()) {
        custody_chain.append(custody);
        path_length += custody->m_name->length() + 1;
    }
    VERIFY(path_length > 0);

    char* buffer;
    auto string = KString::try_create_uninitialized(path_length - 1, buffer);
    if (!string)
        return string;
    size_t string_index = 0;
    for (size_t custody_index = custody_chain.size() - 1; custody_index > 0; --custody_index) {
        buffer[string_index] = '/';
        ++string_index;
        auto& custody_name = *custody_chain[custody_index - 1]->m_name;
        __builtin_memcpy(buffer + string_index, custody_name.characters(), custody_name.length());
        string_index += custody_name.length();
    }
    VERIFY(string->length() == string_index);
    buffer[string_index] = 0;
    return string;
}

String Custody::absolute_path() const
{
    if (!parent())
        return "/";
    Vector<Custody const*, 32> custody_chain;
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
