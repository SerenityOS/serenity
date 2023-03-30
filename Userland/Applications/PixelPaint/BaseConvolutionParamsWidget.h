/*
 * Copyright (c) 2023, Luiz Gustavo de França Chaves
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <Applications/PixelPaint/Image.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class BaseConvolutionParamsWidget final : public GUI::Frame {
    C_OBJECT(BaseConvolutionParamsWidget);

public:
    virtual ~BaseConvolutionParamsWidget() = default;
    Function<void(bool)> on_wrap_around_checked;

    void set_name_label(StringView name);
    void set_should_wrap(bool should_wrap);

private:
    explicit BaseConvolutionParamsWidget();

    bool m_should_wrap { false };

    GUI::CheckBox* m_should_wrap_checkbox;
    GUI::Label* m_name_label;

    RefPtr<GUI::Widget> m_options_widget { nullptr };
};

}
