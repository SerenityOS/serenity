/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/RefPtr.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

class Custody final : public ListedRefCounted<Custody, LockType::Spinlock> {
public:
    static ErrorOr<NonnullRefPtr<Custody>> try_create(Custody* parent, StringView name, Inode&, int mount_flags);

    ~Custody();

    RefPtr<Custody> parent() { return m_parent; }
    RefPtr<Custody const> parent() const { return m_parent; }
    Inode& inode() { return *m_inode; }
    Inode const& inode() const { return *m_inode; }
    StringView name() const { return m_name->view(); }
    ErrorOr<NonnullOwnPtr<KString>> try_serialize_absolute_path() const;

    int mount_flags() const { return m_mount_flags; }
    bool is_readonly() const;

private:
    Custody(Custody* parent, NonnullOwnPtr<KString> name, Inode&, int mount_flags);

    RefPtr<Custody> m_parent;
    NonnullOwnPtr<KString> m_name;
    NonnullRefPtr<Inode> const m_inode;
    int m_mount_flags { 0 };

    mutable IntrusiveListNode<Custody> m_all_custodies_list_node;

public:
    using AllCustodiesList = IntrusiveList<&Custody::m_all_custodies_list_node>;
    static SpinlockProtected<Custody::AllCustodiesList, LockRank::None>& all_instances();
};

}
