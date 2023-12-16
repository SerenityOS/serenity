/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Spreadsheet {

class HelpWindow : public GUI::Window {
    C_OBJECT(HelpWindow);

public:
    static NonnullRefPtr<HelpWindow> the(GUI::Window* window)
    {
        if (s_the)
            return *s_the;

        return *(s_the = adopt_ref(*new HelpWindow(window)));
    }

    virtual ~HelpWindow() override = default;

    void set_docs(JsonObject&& docs);

private:
    static RefPtr<HelpWindow> s_the;
    ByteString render(StringView key);
    HelpWindow(GUI::Window* parent = nullptr);

    JsonObject m_docs;
    RefPtr<WebView::OutOfProcessWebView> m_webview;
    RefPtr<GUI::ListView> m_listview;
};

}
