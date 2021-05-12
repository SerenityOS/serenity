/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Span.h>
#include <LibCore/BufferingIODevice.h>
#include <LibCore/FileLikeIODevice.h>
#include <LibCore/Notifier.h>
#include <LibCore/SocketAddress.h>
#include <LibCore/SocketLikeIODevice.h>

namespace Core {

class Socket
    : public virtual SocketLikeIODevice
    , public virtual FileLikeIODevice {
    C_OBJECT(Socket)
public:
    ~Socket() override;

    void set_blocking(bool blocking);

    SocketAddress source_address() const { return m_source_address; }
    int source_port() const { return m_source_port; }

    SocketAddress destination_address() const { return m_destination_address; }
    int destination_port() const { return m_destination_port; }

    // ^IODevice
    size_t read(Bytes) override;
    bool discard_or_error(size_t count) override;
    size_t write(ReadonlyBytes) override;
    RefPtr<AbstractNotifier> make_notifier(unsigned event_mask) override { return make_notifier_impl(event_mask); }
    bool unreliable_eof() const override { return eof(); }

    // ^SocketLikeIODevice
    bool connect(const String& hostname, int port) override;
    bool connect(const SocketAddress&, int port) override;
    bool connect(const SocketAddress&) override;
    bool is_connected() const override { return m_connected; }

    using FileLikeIODevice::read;

    Function<void()> on_connected;
    Function<void()> on_ready_to_read;

protected:
    explicit Socket(Object* parent)
        : IODevice(parent)
        , SocketLikeIODevice(parent)
        , FileLikeIODevice(parent)
    {
        register_property(
            "source_address", [this] { return m_source_address.to_string(); },
            [](auto&) { return false; });

        register_property(
            "destination_address", [this] { return m_destination_address.to_string(); },
            [](auto&) { return false; });

        register_property(
            "source_port", [this] { return m_source_port; },
            [](auto&) { return false; });

        register_property(
            "destination_port", [this] { return m_destination_port; },
            [](auto&) { return false; });

        register_property(
            "connected", [this] { return m_connected; },
            [](auto&) { return false; });
    }

    SocketAddress m_source_address;
    SocketAddress m_destination_address;
    int m_source_port { -1 };
    int m_destination_port { -1 };
    bool m_connected { false };

    virtual void did_update_fd(int) override;
    virtual bool common_connect(const struct sockaddr*, socklen_t);

private:
    RefPtr<Notifier> make_notifier_impl(unsigned event_mask) { return Notifier::construct(fd(), event_mask, this); }
    bool open(OpenMode) override { return false; }
    void ensure_read_notifier();

    RefPtr<Notifier> m_notifier;
    RefPtr<Notifier> m_read_notifier;
};

}

template<>
struct AK::Formatter<Core::Socket> : Formatter<Core::Object> {
};
