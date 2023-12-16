/*
 * Copyright (c) 2021, Conor Byrne <cbyrneee@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <LibGUI/Widget.h>

namespace HackStudio {

class GMLPreviewWidget final : public GUI::Widget {
    C_OBJECT(GMLPreviewWidget)
public:
    void load_gml(ByteString const&);

private:
    explicit GMLPreviewWidget(ByteString const&);
};

}
