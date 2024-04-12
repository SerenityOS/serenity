/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Application.h"
#include "StringUtils.h"
#include <LibWebView/URL.h>
#include <QFileOpenEvent>

namespace Ladybird {

Application::Application(int& argc, char** argv)
    : QApplication(argc, argv)
{
}

bool Application::event(QEvent* event)
{
    switch (event->type()) {
    case QEvent::FileOpen: {
        if (!on_open_file)
            break;

        auto const& open_event = *static_cast<QFileOpenEvent const*>(event);
        auto file = ak_string_from_qstring(open_event.file());

        if (auto file_url = WebView::sanitize_url(file); file_url.has_value())
            on_open_file(file_url.release_value());
    }

    default:
        break;
    }

    return QApplication::event(event);
}

}
