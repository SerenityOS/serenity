/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibCore/EventReceiver.h>

namespace Core {

class Notifier final : public EventReceiver {
    C_OBJECT(Notifier);

public:
    enum class Type {
        None,
        Read,
        Write,
        Exceptional,
    };

    virtual ~Notifier() override;

    void set_enabled(bool);

    Function<void()> on_activation;

    void close();

    int fd() const { return m_fd; }
    Type type() const { return m_type; }
    void set_type(Type type) { m_type = type; }

    void event(Core::Event&) override;

private:
    Notifier(int fd, Type type, EventReceiver* parent = nullptr);

    int m_fd { -1 };
    Type m_type { Type::None };
};

}
