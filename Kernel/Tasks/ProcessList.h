/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Tasks/Process.h>

namespace Kernel {

class ProcessList : public RefCounted<ProcessList> {
public:
    static ErrorOr<NonnullRefPtr<ProcessList>> create();
    SpinlockProtected<Process::JailProcessList, LockRank::None>& attached_processes() { return m_attached_processes; }
    SpinlockProtected<Process::JailProcessList, LockRank::None> const& attached_processes() const { return m_attached_processes; }

private:
    ProcessList() = default;
    SpinlockProtected<Process::JailProcessList, LockRank::None> m_attached_processes;
};

}
