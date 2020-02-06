/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <LibCore/oreIPCClient.h>
#include <stdio.h>
#include "SimpleEndpoint.h"

class SimpleIPCClient : public IPC::Client::ConnectionNG<SimpleEndpoint> {
    C_OBJECT(SimpleIPCClient)
public:
    SimpleIPCClient()
        : ConnectionNG("/tmp/simple-ipc")
    {}

    virtual void handshake() override {}

    i32 compute_sum(i32 a, i32 b, i32 c)
    {
        return send_sync<Simple::ComputeSum>(a, b, c)->sum();
    }
};

int main(int, char**)
{
    Core::EventLoop event_loop;

    SimpleIPCClient client;

    CTimer timer(100, [&] {
        i32 sum = client.compute_sum(1, 2, 3);
        dbg() << "Sum: " << sum;
    });

    CTimer kill_timer(5000, [&] {
        dbg() << "Timer fired, good-bye! :^)";
        event_loop.quit(0);
    });

    return event_loop.exec();
}
