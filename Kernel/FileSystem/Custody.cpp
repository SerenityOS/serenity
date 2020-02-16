/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/HashTable.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Lock.h>

namespace Kernel {

static Lockable<InlineLinkedList<Custody>>& all_custodies()
{
    static Lockable<InlineLinkedList<Custody>>* list;
    if (!list)
        list = new Lockable<InlineLinkedList<Custody>>;
    return *list;
}

Custody* Custody::get_if_cached(Custody* parent, const StringView& name)
{
    LOCKER(all_custodies().lock());
    for (auto& custody : all_custodies().resource()) {
        if (custody.is_deleted())
            continue;
        if (custody.is_mounted_on())
            continue;
        if (custody.parent() == parent && custody.name() == name)
            return &custody;
    }
    return nullptr;
}

NonnullRefPtr<Custody> Custody::get_or_create(Custody* parent, const StringView& name, Inode& inode, int mount_flags)
{
    if (RefPtr<Custody> cached_custody = get_if_cached(parent, name)) {
        if (&cached_custody->inode() != &inode) {
            dbg() << "WTF! Cached custody for name '" << name << "' has inode=" << cached_custody->inode().identifier() << ", new inode=" << inode.identifier();
        }
        ASSERT(&cached_custody->inode() == &inode);
        return *cached_custody;
    }
    return create(parent, name, inode, mount_flags);
}

Custody::Custody(Custody* parent, const StringView& name, Inode& inode, int mount_flags)
    : m_parent(parent)
    , m_name(name)
    , m_inode(inode)
    , m_mount_flags(mount_flags)
{
    LOCKER(all_custodies().lock());
    all_custodies().resource().append(this);
}

Custody::~Custody()
{
    LOCKER(all_custodies().lock());
    all_custodies().resource().remove(this);
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

void Custody::did_delete(Badge<VFS>)
{
    m_deleted = true;
}

void Custody::did_mount_on(Badge<VFS>)
{
    m_mounted_on = true;
}

void Custody::did_rename(Badge<VFS>, const String& name)
{
    m_name = name;
}

}
