/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Jail.h>
#include <Kernel/KString.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

class JailManagement {

public:
    JailManagement();
    static JailManagement& the();

    LockRefPtr<Jail> find_jail_by_index(JailIndex);
    LockRefPtr<Jail> find_first_jail_by_name(StringView);

    ErrorOr<NonnullLockRefPtr<Jail>> create_jail(NonnullOwnPtr<KString> name);

    ErrorOr<void> for_each_in_same_jail(Function<ErrorOr<void>(Jail&)>);

private:
    JailIndex generate_jail_id();

    SpinlockProtected<IntrusiveList<&Jail::m_jail_list_node>, LockRank::None> m_jails {};
};

}
