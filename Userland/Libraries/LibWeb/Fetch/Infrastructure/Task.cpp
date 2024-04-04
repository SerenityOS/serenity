/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/Infrastructure/FetchController.h>
#include <LibWeb/Fetch/Infrastructure/Task.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#queue-a-fetch-task
int queue_fetch_task(JS::Object& task_destination, Function<void()> algorithm)
{
    // FIXME: 1. If taskDestination is a parallel queue, then enqueue algorithm to taskDestination.

    // 2. Otherwise, queue a global task on the networking task source with taskDestination and algorithm.
    return HTML::queue_global_task(HTML::Task::Source::Networking, task_destination, move(algorithm));
}

// AD-HOC: This overload allows tracking the queued task within the fetch controller so that we may cancel queued tasks
//         when the spec indicates that we must stop an ongoing fetch.
int queue_fetch_task(JS::NonnullGCPtr<FetchController> fetch_controller, JS::Object& task_destination, Function<void()> algorithm)
{
    auto fetch_task_id = fetch_controller->next_fetch_task_id();

    int event_id = queue_fetch_task(task_destination, [fetch_controller, fetch_task_id, algorithm = move(algorithm)]() {
        fetch_controller->fetch_task_complete(fetch_task_id);
        algorithm();
    });

    fetch_controller->fetch_task_queued(fetch_task_id, event_id);
    return event_id;
}

}
