/*
 * Copyright (c) 2021, The SerenityOS developers.
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

#include <Kernel/Process.h>
#include <Kernel/SpinLock.h>
#include <Kernel/WaitQueue.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

WorkQueue* g_io_work;

void WorkQueue::initialize()
{
    g_io_work = new WorkQueue("IO WorkQueue");
}

WorkQueue::WorkQueue(const char* name)
{
    RefPtr<Thread> thread;
    Process::create_kernel_process(thread, name, [this] {
        for (;;) {
            WorkItem* item;
            bool have_more;
            {
                ScopedSpinLock lock(m_lock);
                item = m_items.take_first();
                have_more = !m_items.is_empty();
            }
            if (item) {
                item->function(item->data);
                if (item->free_data)
                    item->free_data(item->data);
                delete item;

                if (have_more)
                    continue;
            }
            [[maybe_unused]] auto result = m_wait_queue.wait_on({});
        }
    });
    // If we can't create the thread we're in trouble...
    m_thread = thread.release_nonnull();
}

void WorkQueue::do_queue(WorkItem* item)
{
    {
        ScopedSpinLock lock(m_lock);
        m_items.append(*item);
    }
    m_wait_queue.wake_one();
}

}
