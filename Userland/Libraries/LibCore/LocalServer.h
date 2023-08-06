/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/EventReceiver.h>
#include <LibCore/Notifier.h>

namespace Core {

class LocalServer : public EventReceiver {
    C_OBJECT(LocalServer)
public:
    virtual ~LocalServer() override;

    ErrorOr<void> take_over_from_system_server(DeprecatedString const& path = DeprecatedString());
    bool is_listening() const { return m_listening; }
    bool listen(DeprecatedString const& address);

    ErrorOr<NonnullOwnPtr<LocalSocket>> accept();

    Function<void(NonnullOwnPtr<LocalSocket>)> on_accept;
    Function<void(Error)> on_accept_error;

private:
    explicit LocalServer(EventReceiver* parent = nullptr);

    void setup_notifier();

    int m_fd { -1 };
    bool m_listening { false };
    RefPtr<Notifier> m_notifier;
};

}
