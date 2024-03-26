/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/EventReceiver.h>
#include <LibWebView/Forward.h>

#pragma once

namespace WebView {

enum class ProcessType {
    Chrome,
    WebContent,
    WebWorker,
    SQLServer,
    RequestServer,
    ImageDecoder,
};

struct ProcessInfo {
    ProcessType type;
    pid_t pid;
    u64 memory_usage_kib = 0;
    float cpu_percent = 0.0f;
};

ProcessType process_type_from_name(StringView);
StringView process_name_from_type(ProcessType type);

class ProcessManager {
public:
    static ProcessManager& the();
    static void initialize();

    void add_process(WebView::ProcessType, pid_t);
    void remove_process(pid_t);

    void update_all_processes();
    Vector<ProcessInfo> processes() const { return m_processes; }

private:
    ProcessManager();
    ~ProcessManager();

    Vector<ProcessInfo> m_processes;
};

}
