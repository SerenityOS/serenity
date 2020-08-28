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

#include <AK/JsonObject.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibWeb/OutOfProcessWebView.h>

namespace Spreadsheet {

class HelpWindow : public GUI::Window {
    C_OBJECT(HelpWindow);

public:
    static NonnullRefPtr<HelpWindow> the()
    {
        if (s_the)
            return *s_the;

        return *(s_the = adopt(*new HelpWindow));
    }

    virtual ~HelpWindow() override;

    void set_docs(JsonObject&& docs);

private:
    static RefPtr<HelpWindow> s_the;
    String render(const GUI::ModelIndex&);
    HelpWindow(GUI::Window* parent = nullptr);

    JsonObject m_docs;
    RefPtr<Web::OutOfProcessWebView> m_webview;
    RefPtr<GUI::ListView> m_listview;
};

}
