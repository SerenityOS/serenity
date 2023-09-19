/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <LibGfx/Filters/GrayscaleFilter.h>
#include <LibGfx/Filters/InvertFilter.h>
#include <LibGfx/Filters/LaplacianFilter.h>
#include <LibGfx/Filters/SepiaFilter.h>
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
    Matrix<N, float> const& matrix() const { return m_matrix; }
    bool should_wrap() const { return m_should_wrap; }

private:
    explicit GenericConvolutionFilterInputDialog(GUI::Window* parent_window)
        : Dialog(parent_window)
    {
        // FIXME: Help! Make this GUI less ugly.
        StringBuilder builder;
        builder.appendff("{}x{}", N, N);
        builder.append(" Convolution"sv);
        set_title(builder.string_view());

        resize(200, 250);
        auto main_widget = set_main_widget<GUI::Frame>();
        main_widget->set_frame_style(Gfx::FrameStyle::RaisedContainer);
        main_widget->set_fill_with_background_color(true);
        main_widget->template set_layout<GUI::VerticalBoxLayout>(4);

        size_t index = 0;
        size_t columns = N;
        size_t rows = N;

        for (size_t row = 0; row < rows; ++row) {
            auto& horizontal_container = main_widget->template add<GUI::Widget>();
            horizontal_container.template set_layout<GUI::HorizontalBoxLayout>();
            for (size_t column = 0; column < columns; ++column) {
                if (index < columns * rows) {
                    auto& textbox = horizontal_container.template add<GUI::TextBox>();
                    textbox.set_min_width(22);
                    textbox.on_change = [&, row = row, column = column] {
                        auto& element = m_matrix.elements()[row][column];
                        char* endptr = nullptr;
                        auto value = strtof(textbox.text().characters(), &endptr);
                        if (endptr != nullptr)
                            element = value;
                        else
                            textbox.set_text(""sv);
                    };
                } else {
                    horizontal_container.template add<GUI::Widget>();
                }
            }
        }

        auto& norm_checkbox = main_widget->template add<GUI::CheckBox>("Normalize"_string);
        norm_checkbox.set_checked(false);

        auto& wrap_checkbox = main_widget->template add<GUI::CheckBox>("Wrap"_string);
        wrap_checkbox.set_checked(m_should_wrap);

        auto& button = main_widget->template add<GUI::Button>("Done"_string);
        button.on_click = [&](auto) {
            m_should_wrap = wrap_checkbox.is_checked();
            if (norm_checkbox.is_checked())
                normalize(m_matrix);
            done(ExecResult::OK);
        };
    }

    Matrix<N, float> m_matrix {};
    bool m_should_wrap { false };
};

template<size_t N>
struct FilterParameters<Gfx::SpatialGaussianBlurFilter<N>> {
    static OwnPtr<typename Gfx::SpatialGaussianBlurFilter<N>::Parameters> get()
    {
        constexpr static ssize_t offset = N / 2;
        Matrix<N, float> kernel;
        auto sigma = 1.0f;
        auto s = 2.0f * sigma * sigma;

        for (auto x = -offset; x <= offset; x++) {
            for (auto y = -offset; y <= offset; y++) {
                auto r = sqrtf(x * x + y * y);
                kernel.elements()[x + offset][y + offset] = (expf(-(r * r) / s)) / (float { M_PI } * s);
            }
        }

        normalize(kernel);

        return make<typename Gfx::GenericConvolutionFilter<N>::Parameters>(kernel);
    }
};

template<>
struct FilterParameters<Gfx::SharpenFilter> {
    static OwnPtr<Gfx::GenericConvolutionFilter<3>::Parameters> get()
    {
        return make<Gfx::GenericConvolutionFilter<3>::Parameters>(Matrix<3, float>(0, -1, 0, -1, 5, -1, 0, -1, 0));
    }
};

template<>
struct FilterParameters<Gfx::LaplacianFilter> {
    static OwnPtr<Gfx::GenericConvolutionFilter<3>::Parameters> get(bool diagonal)
    {
        if (diagonal)
            return make<Gfx::GenericConvolutionFilter<3>::Parameters>(Matrix<3, float>(-1, -1, -1, -1, 8, -1, -1, -1, -1));

        return make<Gfx::GenericConvolutionFilter<3>::Parameters>(Matrix<3, float>(0, -1, 0, -1, 4, -1, 0, -1, 0));
    }
};

template<size_t N>
struct FilterParameters<Gfx::GenericConvolutionFilter<N>> {
    static OwnPtr<typename Gfx::GenericConvolutionFilter<N>::Parameters> get(GUI::Window* parent_window)
    {
        auto input = GenericConvolutionFilterInputDialog<N>::construct(parent_window);
        input->exec();
        if (input->result() == GUI::Dialog::ExecResult::OK)
            return make<typename Gfx::GenericConvolutionFilter<N>::Parameters>(input->matrix(), input->should_wrap());

        return {};
    }
};

template<size_t N>
struct FilterParameters<Gfx::BoxBlurFilter<N>> {
    static OwnPtr<typename Gfx::GenericConvolutionFilter<N>::Parameters> get()
    {
        Matrix<N, float> kernel;

        for (size_t i = 0; i < N; ++i) {
            for (size_t j = 0; j < N; ++j) {
                kernel.elements()[i][j] = 1;
            }
        }

        normalize(kernel);

        return make<typename Gfx::GenericConvolutionFilter<N>::Parameters>(kernel);
    }
};

}
