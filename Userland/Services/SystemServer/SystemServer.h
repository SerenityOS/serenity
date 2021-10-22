/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <LibCore/EventLoop.h>

class SystemServer : public Core::EventLoop {
public:
    enum class Mode {
        User,
        System
    };

    static void initialize(Mode mode);
    static SystemServer& the();

    String socket_directory() const;

private:
    SystemServer(Mode mode)
        : m_mode(mode)
    {
    }

    Mode const m_mode;
};
