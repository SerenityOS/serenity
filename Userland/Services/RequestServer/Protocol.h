/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/URL.h>
#include <RequestServer/Forward.h>

namespace RequestServer {

class Protocol {
public:
    virtual ~Protocol();

    const String& name() const { return m_name; }
    virtual OwnPtr<Request> start_request(ConnectionFromClient&, const String& method, const URL&, const HashMap<String, String>& headers, ReadonlyBytes body) = 0;

    static Protocol* find_by_name(const String&);

protected:
    explicit Protocol(const String& name);
    struct Pipe {
        int read_fd { -1 };
        int write_fd { -1 };
    };
    static ErrorOr<Pipe> get_pipe_for_request();

private:
    String m_name;
};

}
