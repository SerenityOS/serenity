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

#pragma once

#include <AK/InlineLinkedList.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>
#include <Kernel/SpinLock.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcessGroup
    : public RefCounted<ProcessGroup>
    , public Weakable<ProcessGroup>
    , public InlineLinkedListNode<ProcessGroup> {

    AK_MAKE_NONMOVABLE(ProcessGroup);
    AK_MAKE_NONCOPYABLE(ProcessGroup);

    friend InlineLinkedListNode<ProcessGroup>;

public:
    ~ProcessGroup();

    static NonnullRefPtr<ProcessGroup> create(ProcessGroupID);
    static NonnullRefPtr<ProcessGroup> find_or_create(ProcessGroupID);
    static ProcessGroup* from_pgid(ProcessGroupID);

    const ProcessGroupID& pgid() const { return m_pgid; }

private:
    ProcessGroup(ProcessGroupID pgid)
        : m_pgid(pgid)
    {
    }

    ProcessGroup* m_prev { nullptr };
    ProcessGroup* m_next { nullptr };

    ProcessGroupID m_pgid;
};

extern InlineLinkedList<ProcessGroup>* g_process_groups;
extern RecursiveSpinLock g_process_groups_lock;

}
