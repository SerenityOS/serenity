/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <Kernel/Forward.h>
#include <Kernel/Heap/SlabAllocator.h>

namespace Kernel {

// FIXME: Custody needs some locking.

class Custody : public RefCounted<Custody> {
    MAKE_SLAB_ALLOCATED(Custody)
public:
    static NonnullRefPtr<Custody> create(Custody* parent, const StringView& name, Inode& inode, int mount_flags)
    {
        return adopt_ref(*new Custody(parent, name, inode, mount_flags));
    }

    ~Custody();

    Custody* parent() { return m_parent.ptr(); }
    const Custody* parent() const { return m_parent.ptr(); }
    Inode& inode() { return *m_inode; }
    const Inode& inode() const { return *m_inode; }
    const String& name() const { return m_name; }
    String absolute_path() const;

    int mount_flags() const { return m_mount_flags; }
    bool is_readonly() const;

private:
    Custody(Custody* parent, const StringView& name, Inode&, int mount_flags);

    RefPtr<Custody> m_parent;
    String m_name;
    NonnullRefPtr<Inode> m_inode;
    int m_mount_flags { 0 };
};

}
