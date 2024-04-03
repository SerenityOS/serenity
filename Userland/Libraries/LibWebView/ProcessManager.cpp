/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumberFormat.h>
#include <AK/String.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibWebView/ProcessManager.h>

namespace WebView {

static sig_atomic_t s_received_sigchld = 0;

ProcessType process_type_from_name(StringView name)
{
    if (name == "Chrome"sv)
        return ProcessType::Chrome;
    if (name == "WebContent"sv)
        return ProcessType::WebContent;
    if (name == "WebWorker"sv)
        return ProcessType::WebWorker;
    if (name == "SQLServer"sv)
        return ProcessType::SQLServer;
    if (name == "RequestServer"sv)
        return ProcessType::RequestServer;
    if (name == "ImageDecoder"sv)
        return ProcessType::ImageDecoder;

    dbgln("Unknown process type: '{}'", name);
    VERIFY_NOT_REACHED();
}

StringView process_name_from_type(ProcessType type)
{
    switch (type) {
    case ProcessType::Chrome:
        return "Chrome"sv;
    case ProcessType::WebContent:
        return "WebContent"sv;
    case ProcessType::WebWorker:
        return "WebWorker"sv;
    case ProcessType::SQLServer:
        return "SQLServer"sv;
    case ProcessType::RequestServer:
        return "RequestServer"sv;
    case ProcessType::ImageDecoder:
        return "ImageDecoder"sv;
    }
    VERIFY_NOT_REACHED();
}

ProcessManager::ProcessManager()
{
}

ProcessManager::~ProcessManager()
{
}

ProcessManager& ProcessManager::the()
{
    static ProcessManager s_the;
    return s_the;
}

void ProcessManager::initialize()
{
    // FIXME: Should we change this to call EventLoop::register_signal?
    //        Note that only EventLoopImplementationUnix has a working register_signal

    struct sigaction action { };
    action.sa_flags = SA_RESTART;
    action.sa_sigaction = [](int, auto*, auto) {
        s_received_sigchld = 1;
    };

    MUST(Core::System::sigaction(SIGCHLD, &action, nullptr));

    the().add_process(WebView::ProcessType::Chrome, getpid());
}

void ProcessManager::add_process(ProcessType type, pid_t pid)
{
    dbgln("ProcessManager::add_process({}, {})", process_name_from_type(type), pid);
    m_statistics.processes.append({ type, pid, 0, 0 });
}

void ProcessManager::remove_process(pid_t pid)
{
    m_statistics.processes.remove_first_matching([&](auto& info) {
        if (info.pid == pid) {
            dbgln("ProcessManager: Remove process {} ({})", process_name_from_type(info.type), pid);
            return true;
        }
        return false;
    });
}

void ProcessManager::update_all_processes()
{
    if (s_received_sigchld) {
        s_received_sigchld = 0;
        auto result = Core::System::waitpid(-1, WNOHANG);
        while (!result.is_error() && result.value().pid > 0) {
            auto& [pid, status] = result.value();
            if (WIFEXITED(status) || WIFSIGNALED(status)) {
                remove_process(pid);
            }
            result = Core::System::waitpid(-1, WNOHANG);
        }
    }

    (void)update_process_statistics(m_statistics);
}

String ProcessManager::generate_html()
{
    StringBuilder builder;
    auto processes = m_statistics.processes;

    builder.append(R"(
        <html>
        <head>
        <style>
                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                th {
                    text-align: left;
                    border-bottom: 1px solid #aaa;
                }
                td, th {
                    padding: 4px;
                    border: 1px solid #aaa;
                }
                tr:nth-child(odd) {
                    background: #f7f7f7;
                }
        </style>
        </head>
        <body>
        <table>
                <thead>
                <tr>
                        <th>Type</th>
                        <th>PID</th>
                        <th>Memory Usage</th>
                        <th>CPU %</th>
                </tr>
                </thead>
                <tbody>
    )"sv);

    for (auto& process : processes) {
        builder.append("<tr>"sv);
        builder.append("<td>"sv);
        builder.append(WebView::process_name_from_type(process.type));
        builder.append("</td>"sv);
        builder.append("<td>"sv);
        builder.append(MUST(String::number(process.pid)));
        builder.append("</td>"sv);
        builder.append("<td>"sv);
        builder.append(human_readable_size(process.memory_usage_bytes));
        builder.append("</td>"sv);
        builder.append("<td>"sv);
        builder.append(MUST(String::formatted("{:.1f}", process.cpu_percent)));
        builder.append("</td>"sv);
        builder.append("</tr>"sv);
    }

    builder.append(R"(
                </tbody>
                </table>
                </body>
                </html>
    )"sv);

    return builder.to_string_without_validation();
}

}
