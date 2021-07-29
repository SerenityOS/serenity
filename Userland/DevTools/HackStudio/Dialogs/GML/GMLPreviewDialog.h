/*
 * Copyright (c) 2021, Conor Byrne <cbyrneee@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../../GMLPreviewWidget.h"
#include <AK/LexicalPath.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Window.h>

namespace HackStudio {

class GMLPreviewDialog final : public GUI::Dialog {
    C_OBJECT(GMLPreviewDialog)

public:
    void load_gml(String const& content, String const& filename);

private:
    GMLPreviewDialog(String const& content, String const& filename);

    RefPtr<GMLPreviewWidget> m_gml_preview;
};

}
