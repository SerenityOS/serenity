/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Socket.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/MessagePort.h>
#include <LibWeb/HTML/Scripting/WorkerEnvironmentSettingsObject.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Worker/WebWorkerClient.h>

namespace Web::HTML {

struct WorkerOptions {
    String type { "classic"_string };
    String credentials { "same-origin"_string };
    String name { String {} };
};

struct WorkerAgent : JS::Cell {
    JS_CELL(Agent, JS::Cell);
    JS_DECLARE_ALLOCATOR(WorkerAgent);

    WorkerAgent(AK::URL url, WorkerOptions const& options);

    RefPtr<Web::HTML::WebWorkerClient> m_worker_ipc;

    Core::BufferedLocalSocket& socket() const { return *m_socket; }

private:
    WorkerOptions m_worker_options;
    AK::URL m_url;

    // FIXME: associate with MessagePorts
    OwnPtr<Core::BufferedLocalSocket> m_socket;
};

}
