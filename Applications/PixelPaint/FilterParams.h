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

#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Filters/BoxBlurFilter.h>
#include <LibGfx/Filters/GenericConvolutionFilter.h>
#include <LibGfx/Filters/LaplacianFilter.h>
#include <LibGfx/Filters/SharpenFilter.h>
#include <LibGfx/Filters/SpatialGaussianBlurFilter.h>

namespace PixelPaint {

template<typename Filter>
struct FilterParameters {
};

template<size_t N>
class GenericConvolutionFilterInputDialog : public GUI::Dialog {
    C_OBJECT(GenericConvolutionFilterInputDialog);

public:
    const Matrix<N, float>& matrix() const { return m_matrix; }
    bool should_wrap() const { return m_should_wrap; }

private:
    explicit GenericConvolutionFilterInputDialog(GUI::Window* parent_window)
        : Dialog(parent_window)
    {
        // FIXME: Help! Make this GUI less ugly.
        StringBuilder builder;
        builder.appendf("%zux%zu", N, N);
        builder.append(" Convolution");
        set_title(builder.string_view());

        resize(200, 250);
        auto& main_widget = set_main_widget<GUI::Frame>();
        main_widget.set_frame_shape(Gfx::FrameShape::Container);
        main_widget.set_frame_shadow(Gfx::FrameShadow::Raised);
        main_widget.set_fill_with_background_color(true);
        auto& layout = main_widget.template set_layout<GUI::VerticalBoxLayout>();
        layout.set_margins({ 4, 4, 4, 4 });

        size_t index = 0;
        size_t columns = N;
        size_t rows = N;

        for (size_t row = 0; row < rows; ++row) {
            auto& horizontal_container = main_widget.template add<GUI::Widget>();
            horizontal_container.template set_layout<GUI::HorizontalBoxLayout>();
            for (size_t column = 0; column < columns; ++column) {
                if (index < columns * rows) {
                    auto& textbox = horizontal_container.template add<GUI::TextBox>();
                    textbox.set_preferred_size({ 30, 50 });
                    textbox.on_change = [&, row = row, column = column] {
                        auto& element = m_matrix.elements()[row][column];
                        char* endptr = nullptr;
                        auto value = strtof(textbox.text().characters(), &endptr);
                        if (endptr != nullptr)
                            element = value;
                        else
                            textbox.set_text("");
                    };
                } else {
                    horizontal_container.template add<GUI::Widget>();
                }
            }
        }

        auto& norm_checkbox = main_widget.template add<GUI::CheckBox>("Normalize");
        norm_checkbox.set_checked(false);

        auto& wrap_checkbox = main_widget.template add<GUI::CheckBox>("Wrap");
        wrap_checkbox.set_checked(m_should_wrap);

        auto& button = main_widget.template add<GUI::Button>("Done");
        button.on_click = [&](auto) {
            m_should_wrap = wrap_checkbox.is_checked();
            if (norm_checkbox.is_checked())
                normalize(m_matrix);
            done(ExecOK);
        };
    }

    Matrix<N, float> m_matrix {};
    bool m_should_wrap { false };
};

template<size_t N>
struct FilterParameters<Gfx::SpatialGaussianBlurFilter<N>> {
    static OwnPtr<typename Gfx::SpatialGaussianBlurFilter<N>::Parameters> get(Gfx::Bitmap& bitmap, const Gfx::IntRect& rect)
    {
        Matrix<N, float> kernel;
        auto sigma = 1.0f;
        auto s = 2.0f * sigma * sigma;

        for (auto x = -(ssize_t)N / 2; x <= (ssize_t)N / 2; x++) {
            for (auto y = -(ssize_t)N / 2; y <= (ssize_t)N / 2; y++) {
                auto r = sqrt(x * x + y * y);
                kernel.elements()[x + 2][y + 2] = (exp(-(r * r) / s)) / (M_PI * s);
            }
        }

        normalize(kernel);

        return make<typename Gfx::GenericConvolutionFilter<N>::Parameters>(bitmap, rect, kernel);
    }
};

template<>
struct FilterParameters<Gfx::SharpenFilter> {
    static OwnPtr<Gfx::GenericConvolutionFilter<3>::Parameters> get(Gfx::Bitmap& bitmap, const Gfx::IntRect& rect)
    {
        return make<Gfx::GenericConvolutionFilter<3>::Parameters>(bitmap, rect, Matrix<3, float>(0, -1, 0, -1, 5, -1, 0, -1, 0));
    }
};

template<>
struct FilterParameters<Gfx::LaplacianFilter> {
    static OwnPtr<Gfx::GenericConvolutionFilter<3>::Parameters> get(Gfx::Bitmap& bitmap, const Gfx::IntRect& rect, bool diagonal)
    {
        if (diagonal)
            return make<Gfx::GenericConvolutionFilter<3>::Parameters>(bitmap, rect, Matrix<3, float>(-1, -1, -1, -1, 8, -1, -1, -1, -1));

        return make<Gfx::GenericConvolutionFilter<3>::Parameters>(bitmap, rect, Matrix<3, float>(0, -1, 0, -1, 4, -1, 0, -1, 0));
    }
};

template<size_t N>
struct FilterParameters<Gfx::GenericConvolutionFilter<N>> {
    static OwnPtr<typename Gfx::GenericConvolutionFilter<N>::Parameters> get(Gfx::Bitmap& bitmap, const Gfx::IntRect& rect, GUI::Window* parent_window)
    {
        auto input = GenericConvolutionFilterInputDialog<N>::construct(parent_window);
        input->exec();
        if (input->result() == GUI::Dialog::ExecOK)
            return make<typename Gfx::GenericConvolutionFilter<N>::Parameters>(bitmap, rect, input->matrix(), input->should_wrap());

        return {};
    }
};

template<size_t N>
struct FilterParameters<Gfx::BoxBlurFilter<N>> {
    static OwnPtr<typename Gfx::GenericConvolutionFilter<N>::Parameters> get(Gfx::Bitmap& bitmap, const Gfx::IntRect& rect)
    {
        Matrix<N, float> kernel;

        for (size_t i = 0; i < N; ++i) {
            for (size_t j = 0; j < N; ++j) {
                kernel.elements()[i][j] = 1;
            }
        }

        normalize(kernel);

        return make<typename Gfx::GenericConvolutionFilter<N>::Parameters>(bitmap, rect, kernel);
    }
};

}
