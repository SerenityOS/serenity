/*
 * Copyright (c) 2020, Andrew Kaster <andrewdkaster@gmail.com>
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

#include <AK/Atomic.h>
#include <serenity.h>
#include <stdint.h>
#include <sys/types.h>

// Static variable guard logic, using futexes
// Written while looking at libc++ from a distance ;) This is super tricky...
// From itanium spec: Get passed a pointer to a 64 bit guard "object"
// [ 1 guard byte (checked by compiler), 7 bytes to work with  ]
class CxaGuard {
public:
    enum GuardState : uint8_t {
        NotInitialized = 0,
        DoneInit = 1,
        InitInProgress = 2,
        WaitingOnInit = 4
    };

    CxaGuard(uint64_t* guard_pointer)
        : m_full_guard_var(guard_pointer)
        , m_compiler_guard_byte((uint8_t*)guard_pointer)
        , m_local_state_byte(&((uint8_t*)guard_pointer)[1])
    {
    }

    int acquire()
    {
        // Someone else already initailzied this by storing to the 'really done' slot
        if (GuardState::NotInitialized != m_compiler_guard_byte.load(AK::memory_order_acquire))
            return 0;

        // Time to begin the waiting game..
        while (true) {
            // Try to claim initialization for this thread from the initial state
            uint8_t last_state = GuardState::NotInitialized;
            if (m_local_state_byte.compare_exchange_strong(last_state, GuardState::InitInProgress, AK::memory_order_acq_rel))
                return 1; // Tell compiler-generated code to init the variable

            // Someone else set the state to done, we're done here
            if (last_state == DoneInit)
                return 0;

            // Someone else set InitInProgress, time to wait (maybe)
            if (last_state & GuardState::InitInProgress) {
                // Try to set the in progress + done bits
                if ((last_state & GuardState::WaitingOnInit) == 0) {
                    if (!m_local_state_byte.compare_exchange_strong(last_state, (GuardState::InitInProgress | GuardState::WaitingOnInit), AK::memory_order_acq_rel)) {
                        if (last_state == GuardState::DoneInit)
                            return 0;
                        if (last_state == GuardState::NotInitialized)
                            continue; // start over, and try to initialize in this thread
                        // else wait, someone else set the wait bit before us
                    }
                }
                // State is InitInProgress | WaitingOnInit, time to sleep
                wait_on_futex();
            }
        }
    }

    void release()
    {
        // Store the value the compiler will look at, we're done! yay
        m_compiler_guard_byte.store(GuardState::DoneInit, AK::memory_order_release);

        // Set the value other threads chilling in our loop will look at to done
        uint8_t old_local_state = m_local_state_byte.exchange(GuardState::DoneInit, AK::memory_order_acq_rel);

        // Wake up sleepy heads, the variable is initialized!
        if (old_local_state & GuardState::WaitingOnInit)
            wake_futex();
    }

    void abort()
    {
        uint8_t old_local_state = m_local_state_byte.exchange(GuardState::NotInitialized, AK::memory_order_acq_rel);

        // Wake up sleepy heads, someone else gets to try...
        if (old_local_state & GuardState::WaitingOnInit)
            wake_futex();
    }

private:
    void wait_on_futex()
    {
        int futex_value = (GuardState::InitInProgress | GuardState::WaitingOnInit) << 8;
        futex((int32_t*)m_full_guard_var, FUTEX_WAIT, futex_value, nullptr);
    }

    void wake_futex()
    {
        futex((int32_t*)m_full_guard_var, FUTEX_WAKE, INT32_MAX, nullptr);
    }

    void* m_full_guard_var;
    AtomicRef<uint8_t> m_compiler_guard_byte;
    AtomicRef<uint8_t> m_local_state_byte; // we'll load and set this atomically

    // Note that we're not using 6/8 bytes of the guard object...
};

extern "C" {

int __cxa_guard_acquire(uint64_t* guard_object)
{
    CxaGuard g(guard_object);
    return g.acquire();
}

void __cxa_guard_release(uint64_t* guard_object)
{
    CxaGuard g(guard_object);
    g.release();
}

void __cxa_guard_abort(uint64_t* guard_object)
{
    CxaGuard g(guard_object);
    g.abort();
}

} // extern "C"
