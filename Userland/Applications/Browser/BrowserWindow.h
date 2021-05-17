/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BookmarksBarWidget.h"
#include "WindowActions.h"
#include <LibGUI/Window.h>

namespace Browser {

class CookieJar;
class Tab;

class BrowserWindow final : public GUI::Window {
    C_OBJECT(BrowserWindow);

public:
    virtual ~BrowserWindow() override;

    GUI::TabWidget& tab_widget();
    void create_new_tab(URL, bool activate);

private:
    explicit BrowserWindow(CookieJar&, URL);

    void set_window_title_for_tab(Tab const&);

    CookieJar& m_cookie_jar;
    WindowActions m_window_actions;
    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<BookmarksBarWidget> m_bookmarks_bar;
};

}
