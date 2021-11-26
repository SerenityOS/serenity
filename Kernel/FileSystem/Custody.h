/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <Kernel/Forward.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/KString.h>

namespace Kernel {

// FIXME: Custody needs some locking.

class Custody : public RefCountedBase {
    MAKE_SLAB_ALLOCATED(Custody)
public:
    bool unref() const;

    static ErrorOr<NonnullRefPtr<Custody>> try_create(Custody* parent, StringView name, Inode&, int mount_flags);

    ~Custody();

    Custody* parent() { return m_parent.ptr(); }
    Custody const* parent() const { return m_parent.ptr(); }
    Inode& inode() { return *m_inode; }
    Inode const& inode() const { return *m_inode; }
    StringView name() const { return m_name->view(); }
    ErrorOr<NonnullOwnPtr<KString>> try_serialize_absolute_path() const;
    String absolute_path() const;

    int mount_flags() const { return m_mount_flags; }
    bool is_readonly() const;

private:
    Custody(Custody* parent, NonnullOwnPtr<KString> name, Inode&, int mount_flags);

    RefPtr<Custody> m_parent;
    NonnullOwnPtr<KString> m_name;
    NonnullRefPtr<Inode> m_inode;
    int m_mount_flags { 0 };

    mutable IntrusiveListNode<Custody> m_all_custodies_list_node;

public:
    using AllCustodiesList = IntrusiveList<&Custody::m_all_custodies_list_node>;
};

}
