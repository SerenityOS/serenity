/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/Infrastructure/Task.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#queue-a-fetch-task
void queue_fetch_task(JS::Object& task_destination, JS::SafeFunction<void()> algorithm)
{
    // FIXME: 1. If taskDestination is a parallel queue, then enqueue algorithm to taskDestination.

    // 2. Otherwise, queue a global task on the networking task source with taskDestination and algorithm.
    HTML::queue_global_task(HTML::Task::Source::Networking, task_destination, move(algorithm));
}

}
