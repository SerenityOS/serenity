/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace ThemeEditor {

class MetricProperty : public GUI::Frame {
    C_OBJECT_ABSTRACT(MetricProperty)

public:
    static ErrorOr<NonnullRefPtr<MetricProperty>> try_create();
    virtual ~MetricProperty() override = default;

private:
    MetricProperty() = default;
};
}
