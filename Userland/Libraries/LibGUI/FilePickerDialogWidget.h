/*
 * Copyright (c) 2024, Aryan Baburajan <aryanbaburajan2007@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace GUI {

class FilePickerDialogWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(FilePickerDialogWidget)

public:
    static ErrorOr<NonnullRefPtr<FilePickerDialogWidget>> try_create();
    virtual ~FilePickerDialogWidget() override = default;

private:
    FilePickerDialogWidget() = default;
};

}
