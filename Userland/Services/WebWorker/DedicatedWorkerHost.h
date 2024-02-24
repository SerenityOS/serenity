/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/StructuredSerialize.h>

namespace WebWorker {

class DedicatedWorkerHost : public RefCounted<DedicatedWorkerHost> {
public:
    explicit DedicatedWorkerHost(URL url, String type);
    ~DedicatedWorkerHost();

    void run(JS::NonnullGCPtr<Web::Page>, Web::HTML::TransferDataHolder message_port_data);

private:
    RefPtr<Web::HTML::WorkerDebugConsoleClient> m_console;

    URL m_url;
    String m_type;
};

}
