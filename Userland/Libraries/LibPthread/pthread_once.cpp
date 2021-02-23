/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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
            futex(self, FUTEX_WAKE, INT_MAX, nullptr, nullptr, 0);
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
            futex(self, FUTEX_WAIT, state2, nullptr, nullptr, 0);
            // We have been woken up, but that might have been due to a signal
            // or something, so we have to reevaluate. We need acquire ordering
            // here for the same reason as above. Hopefully we'll just see
            // State::DONE this time, but who knows.
            state2 = state.load(AK::memory_order_acquire);
            continue;
        }
    }
}
