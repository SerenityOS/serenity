/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "ProcessGroup.h"

namespace Kernel {

RecursiveSpinLock g_process_groups_lock;
InlineLinkedList<ProcessGroup>* g_process_groups;

ProcessGroup::~ProcessGroup()
{
    ScopedSpinLock lock(g_process_groups_lock);
    g_process_groups->remove(this);
}

NonnullRefPtr<ProcessGroup> ProcessGroup::create(ProcessGroupID pgid)
{
    auto process_group = adopt(*new ProcessGroup(pgid));
    {
        ScopedSpinLock lock(g_process_groups_lock);
        g_process_groups->prepend(process_group);
    }

    return process_group;
}

NonnullRefPtr<ProcessGroup> ProcessGroup::find_or_create(ProcessGroupID pgid)
{
    if (auto existing = from_pgid(pgid))
        return *existing;

    return create(pgid);
}

ProcessGroup* ProcessGroup::from_pgid(ProcessGroupID pgid)
{
    ScopedSpinLock lock(g_process_groups_lock);

    for (auto& group : *g_process_groups) {
        if (group.pgid() == pgid)
            return &group;
    }
    return nullptr;
}

}
