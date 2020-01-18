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

#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>

class PSClientConnection;

class Download : public RefCounted<Download> {
public:
    virtual ~Download();

    static Download* find_by_id(i32);

    i32 id() const { return m_id; }
    URL url() const { return m_url; }

    size_t total_size() const { return m_total_size; }
    size_t downloaded_size() const { return m_downloaded_size; }
    const ByteBuffer& payload() const { return m_payload; }

    void stop();

protected:
    explicit Download(PSClientConnection&);

    void did_finish(bool success);
    void did_progress(size_t total_size, size_t downloaded_size);
    void set_payload(const ByteBuffer&);

private:
    i32 m_id;
    URL m_url;
    size_t m_total_size { 0 };
    size_t m_downloaded_size { 0 };
    ByteBuffer m_payload;
    WeakPtr<PSClientConnection> m_client;
};
