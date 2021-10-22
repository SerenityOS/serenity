/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <Kernel/API/KResult.h>
#include <Kernel/Forward.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/KString.h>

namespace Kernel {

// FIXME: Custody needs some locking.

class Custody : public RefCountedBase {
    MAKE_SLAB_ALLOCATED(Custody)
public:
    bool unref() const;

    static KResultOr<NonnullRefPtr<Custody>> try_create(Custody* parent, StringView name, Inode&, int mount_flags);

    virtual ~Custody();

    virtual bool root_custody() const { return false; }
    Custody* parent() { return m_parent.ptr(); }
    Custody const* parent() const { return m_parent.ptr(); }
    Inode& inode() { return *m_inode; }
    Inode const& inode() const { return *m_inode; }
    StringView name() const { return m_name->view(); }
    KResultOr<NonnullOwnPtr<KString>> try_serialize_absolute_path() const;
    String absolute_path() const;

    int mount_flags() const { return m_mount_flags; }
    bool is_readonly() const;

protected:
    Custody(Custody* parent, NonnullOwnPtr<KString> name, Inode&, int mount_flags);

private:
    RefPtr<Custody> m_parent;
    NonnullOwnPtr<KString> m_name;
    NonnullRefPtr<Inode> m_inode;

protected:
    int m_mount_flags { 0 };

private:
    mutable IntrusiveListNode<Custody> m_all_custodies_list_node;

public:
    using AllCustodiesList = IntrusiveList<&Custody::m_all_custodies_list_node>;
};

class RootCustody final : public Custody {
public:
    static KResultOr<NonnullRefPtr<RootCustody>> try_create(Badge<VirtualFileSystem>, Inode&);

    virtual bool root_custody() const { return true; }
    void set_mount_flags(Badge<VirtualFileSystem>, int mount_flags) { m_mount_flags = mount_flags; }

private:
    RootCustody(NonnullOwnPtr<KString> name, Inode&);
};

}
