/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Types.h>
#include <pthread.h>
#include <serenity.h>

enum State : i32 {
    INITIAL = PTHREAD_ONCE_INIT,
    DONE,
    PERFORMING_NO_WAITERS,
    PERFORMING_WITH_WAITERS,
};

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_once.html
int pthread_once(pthread_once_t* self, void (*callback)(void))
{
    auto& state = reinterpret_cast<Atomic<State>&>(*self);

    // See what the current state is, and at the same time grab the lock if we
    // got here first. We need acquire ordering here because if we see
    // State::DONE, everything we do after that should "happen after" everything
    // the other thread has done before writing the State::DONE.
    State state2 = State::INITIAL;
    bool have_exchanged = state.compare_exchange_strong(
        state2, State::PERFORMING_NO_WAITERS, AK::memory_order_acquire);

    if (have_exchanged) {
        // We observed State::INITIAL and we've changed it to
        // State::PERFORMING_NO_WAITERS, so it's us who should perform the
        // operation.
        callback();
        // Now, record that we're done.
        state2 = state.exchange(State::DONE, AK::memory_order_release);
        switch (state2) {
        case State::INITIAL:
        case State::DONE:
            VERIFY_NOT_REACHED();
        case State::PERFORMING_NO_WAITERS:
            // The fast path: there's no contention, so we don't have to wake
            // anyone.
            break;
        case State::PERFORMING_WITH_WAITERS:
            futex_wake(self, INT_MAX, false);
            break;
        }

        return 0;
    }

    // We did not get there first. Let's see if we have to wait.
    // state2 contains the observed state.
    while (true) {
        switch (state2) {
        case State::INITIAL:
            VERIFY_NOT_REACHED();
        case State::DONE:
            // Awesome, nothing to do then.
            return 0;
        case State::PERFORMING_NO_WAITERS:
            // We're going to wait for it, but we have to record that we're
            // waiting and the other thread should wake us up. We need acquire
            // ordering here for the same reason as above.
            have_exchanged = state.compare_exchange_strong(
                state2, State::PERFORMING_WITH_WAITERS, AK::memory_order_acquire);
            if (!have_exchanged) {
                // Something has changed already, reevaluate without waiting.
                continue;
            }
            state2 = State::PERFORMING_WITH_WAITERS;
            [[fallthrough]];
        case State::PERFORMING_WITH_WAITERS:
            // Let's wait for it.
            futex_wait(self, state2, nullptr, 0, false);
            // We have been woken up, but that might have been due to a signal
            // or something, so we have to reevaluate. We need acquire ordering
            // here for the same reason as above. Hopefully we'll just see
            // State::DONE this time, but who knows.
            state2 = state.load(AK::memory_order_acquire);
            continue;
        }
    }
}
