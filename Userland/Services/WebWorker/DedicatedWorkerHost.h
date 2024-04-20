/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibURL/URL.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Scripting/SerializedEnvironmentSettingsObject.h>
#include <LibWeb/HTML/StructuredSerialize.h>

namespace WebWorker {

class DedicatedWorkerHost : public RefCounted<DedicatedWorkerHost> {
public:
    explicit DedicatedWorkerHost(URL::URL url, String type);
    ~DedicatedWorkerHost();

    void run(JS::NonnullGCPtr<Web::Page>, Web::HTML::TransferDataHolder message_port_data, Web::HTML::SerializedEnvironmentSettingsObject const&);

private:
    JS::Handle<Web::HTML::WorkerDebugConsoleClient> m_console;

    URL::URL m_url;
    String m_type;
};

}
