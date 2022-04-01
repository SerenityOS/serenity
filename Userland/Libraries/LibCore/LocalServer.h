/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <LibCore/Stream.h>

namespace Core {

class LocalServer : public Object {
    C_OBJECT(LocalServer)
public:
    virtual ~LocalServer() override;

    ErrorOr<void> take_over_from_system_server(String const& path = String());
    bool is_listening() const { return m_listening; }
    bool listen(String const& address);

    ErrorOr<NonnullOwnPtr<Stream::LocalSocket>> accept();

    Function<void(NonnullOwnPtr<Stream::LocalSocket>)> on_accept;

private:
    explicit LocalServer(Object* parent = nullptr);

    void setup_notifier();

    int m_fd { -1 };
    bool m_listening { false };
    RefPtr<Notifier> m_notifier;
};

}
