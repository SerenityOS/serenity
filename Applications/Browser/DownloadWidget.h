/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/URL.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGUI/ProgressBar.h>
#include <LibGUI/Widget.h>
#include <LibProtocol/Download.h>

namespace Browser {

class DownloadWidget final : public GUI::Widget {
    C_OBJECT(DownloadWidget);

public:
    virtual ~DownloadWidget() override;

private:
    explicit DownloadWidget(const URL&);

    void did_progress(Optional<u32> total_size, u32 downloaded_size);
    void did_finish(bool success, const ByteBuffer& payload, RefPtr<SharedBuffer> payload_storage, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers);

    URL m_url;
    String m_destination_path;
    RefPtr<Protocol::Download> m_download;
    RefPtr<GUI::ProgressBar> m_progress_bar;
    RefPtr<GUI::Label> m_progress_label;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_close_button;
    Core::ElapsedTimer m_elapsed_timer;
};

}
