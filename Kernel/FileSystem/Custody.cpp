/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefPtr.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<Custody>> Custody::try_create(RefPtr<Custody> parent, StringView name, Inode& inode, int mount_flags)
{
    auto name_kstring = TRY(KString::try_create(name));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Custody(move(parent), move(name_kstring), inode, mount_flags)));
}

Custody::Custody(RefPtr<Custody> parent, NonnullOwnPtr<KString> name, Inode& inode, int mount_flags)
    : m_parent(move(parent))
    , m_name(move(name))
    , m_inode(inode)
    , m_mount_flags(mount_flags)
{
}

Custody::~Custody() = default;

ErrorOr<NonnullOwnPtr<KString>> Custody::try_serialize_absolute_path() const
{
    if (!parent())
        return KString::try_create("/"sv);

    Vector<Custody const*, 32> custody_chain;
    size_t path_length = 0;
    for (auto const* custody = this; custody; custody = custody->parent()) {
        TRY(custody_chain.try_append(custody));
        path_length += custody->m_name->length() + 1;
    }
    VERIFY(path_length > 0);

    char* buffer;
    auto string = TRY(KString::try_create_uninitialized(path_length - 1, buffer));
    size_t string_index = 0;
    for (size_t custody_index = custody_chain.size() - 1; custody_index > 0; --custody_index) {
        buffer[string_index] = '/';
        ++string_index;
        auto const& custody_name = *custody_chain[custody_index - 1]->m_name;
        __builtin_memcpy(buffer + string_index, custody_name.characters(), custody_name.length());
        string_index += custody_name.length();
    }
    VERIFY(string->length() == string_index);
    buffer[string_index] = 0;
    return string;
}

bool Custody::is_readonly() const
{
    if (m_mount_flags & MS_RDONLY)
        return true;

    return m_inode->fs().is_readonly();
}

}
