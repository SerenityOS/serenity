/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Forward.h>

namespace WebWorker {

class DedicatedWorkerHost : public RefCounted<DedicatedWorkerHost> {
public:
    explicit DedicatedWorkerHost(Web::Page&, AK::URL url, String type);
    ~DedicatedWorkerHost();

    void run();

private:
    NonnullRefPtr<JS::VM> m_worker_vm;
    RefPtr<Web::HTML::WorkerDebugConsoleClient> m_console;
    Web::Page& m_page;

    AK::URL m_url;
    String m_type;
};

}
