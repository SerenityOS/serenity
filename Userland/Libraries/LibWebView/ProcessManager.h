/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Platform/ProcessStatistics.h>
#include <LibThreading/Mutex.h>
#include <LibWebView/Forward.h>
#include <LibWebView/ProcessInfo.h>

namespace WebView {

ProcessType process_type_from_name(StringView);
StringView process_name_from_type(ProcessType type);

class ProcessManager {
public:
    static ProcessManager& the();
    static void initialize();

    void add_process(WebView::ProcessType, pid_t);
    void remove_process(pid_t);
    ProcessInfo* find_process(pid_t);

#if defined(AK_OS_MACH)
    void add_process(pid_t, Core::MachPort&&);
#endif

    void update_all_processes();
    String generate_html();

private:
    ProcessManager();
    ~ProcessManager();

    Core::Platform::ProcessStatistics m_statistics;
    Threading::Mutex m_lock;
};

}
