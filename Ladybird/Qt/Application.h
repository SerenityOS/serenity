/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibURL/URL.h>
#include <QApplication>

namespace Ladybird {

class Application : public QApplication {
    Q_OBJECT

public:
    Application(int& argc, char** argv);

    virtual bool event(QEvent* event) override;

    Function<void(URL::URL)> on_open_file;
};

}
