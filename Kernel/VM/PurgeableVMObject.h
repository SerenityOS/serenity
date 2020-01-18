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

#pragma once

#include <Kernel/VM/AnonymousVMObject.h>

class PurgeableVMObject final : public AnonymousVMObject {
public:
    virtual ~PurgeableVMObject() override;

    static NonnullRefPtr<PurgeableVMObject> create_with_size(size_t);
    virtual NonnullRefPtr<VMObject> clone() override;

    int purge();
    int purge_with_interrupts_disabled(Badge<MemoryManager>);

    bool was_purged() const { return m_was_purged; }
    void set_was_purged(bool b) { m_was_purged = b; }

    bool is_volatile() const { return m_volatile; }
    void set_volatile(bool b) { m_volatile = b; }

private:
    explicit PurgeableVMObject(size_t);
    explicit PurgeableVMObject(const PurgeableVMObject&);

    int purge_impl();

    PurgeableVMObject& operator=(const PurgeableVMObject&) = delete;
    PurgeableVMObject& operator=(PurgeableVMObject&&) = delete;
    PurgeableVMObject(PurgeableVMObject&&) = delete;

    virtual bool is_purgeable() const override { return true; }

    bool m_was_purged { false };
    bool m_volatile { false };
};
