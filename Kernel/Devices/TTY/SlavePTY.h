/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/TTY/TTY.h>
#include <Kernel/FileSystem/InodeIdentifier.h>

namespace Kernel {

class MasterPTY;

class SlavePTY final : public TTY {
public:
    virtual bool unref() const override;
    virtual ~SlavePTY() override;

    void on_master_write(UserOrKernelBuffer const&, size_t);
    unsigned index() const { return m_index; }

    UnixDateTime time_of_last_write() const { return m_time_of_last_write; }

    virtual FileBlockerSet& blocker_set() override;

    UserID uid() const { return m_uid; }
    GroupID gid() const { return m_gid; }

private:
    // ^Device
    virtual bool is_openable_by_jailed_processes() const override { return true; }

    // ^TTY
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_name() const override;
    virtual ErrorOr<size_t> on_tty_write(UserOrKernelBuffer const&, size_t) override;
    virtual void echo(u8) override;

    // ^CharacterDevice
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual StringView class_name() const override { return "SlavePTY"sv; }
    virtual ErrorOr<void> close() override;

    friend class MasterPTY;
    SlavePTY(NonnullRefPtr<MasterPTY>, UserID, GroupID, unsigned index);

    NonnullRefPtr<MasterPTY> const m_master;
    UnixDateTime m_time_of_last_write {};
    unsigned m_index { 0 };

    UserID const m_uid { 0 };
    GroupID const m_gid { 0 };

    mutable IntrusiveListNode<SlavePTY> m_list_node;

public:
    using List = IntrusiveList<&SlavePTY::m_list_node>;
    static SpinlockProtected<SlavePTY::List, LockRank::None>& all_instances();
};

}
