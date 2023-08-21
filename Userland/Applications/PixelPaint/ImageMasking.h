/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Dialog.h>
#include <LibGUI/RangeSlider.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class ColorWheelWidget;

class ImageMasking final : public GUI::Dialog {
    C_OBJECT(ImageMasking);

public:
    enum class MaskingType {
        Luminosity,
        Color,
    };

protected:
    void on_done(GUI::Dialog::ExecResult) override;

private:
    explicit ImageMasking(GUI::Window* parent_window, ImageEditor*, MaskingType masking_type);

    MaskingType m_masking_type;
    Layer::EditMode m_previous_edit_mode;
    ImageEditor* m_editor { nullptr };
    RefPtr<Gfx::Bitmap> m_reference_mask { nullptr };
    bool m_did_change = false;
    Optional<Gfx::IntRect> m_masked_area;

    RefPtr<GUI::RangeSlider> m_full_masking_slider = { nullptr };
    RefPtr<GUI::RangeSlider> m_edge_masking_slider = { nullptr };
    RefPtr<ColorWheelWidget> m_color_wheel_widget = { nullptr };
    RefPtr<GUI::RangeSlider> m_saturation_value_masking_slider = { nullptr };

    ErrorOr<void> ensure_reference_mask();
    void generate_new_mask();
};

class RangeIllustrationWidget final : public GUI::Widget {
    C_OBJECT(RangeIllustrationWidget)
public:
    virtual ~RangeIllustrationWidget() override = default;

protected:
    virtual void paint_event(GUI::PaintEvent&) override;

private:
    RangeIllustrationWidget(RefPtr<GUI::RangeSlider> edge_mask_values, RefPtr<GUI::RangeSlider> full_mask_values)
    {
        m_edge_mask_values = edge_mask_values;
        m_full_mask_values = full_mask_values;
    }
    RefPtr<GUI::RangeSlider> m_edge_mask_values;
    RefPtr<GUI::RangeSlider> m_full_mask_values;
};

class ColorWheelWidget final : public GUI::Widget {
    C_OBJECT(ColorWheelWidget)
public:
    virtual ~ColorWheelWidget() override = default;
    double hue();
    void set_hue(double);
    double color_range();
    void set_color_range(double);
    int hardness();
    void set_hardness(int);
    Function<void(double, double, int)> on_change;

protected:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;

private:
    ColorWheelWidget() = default;
    double m_hue = 0;
    double m_color_range = 0;
    int m_hardness = 0;
    bool m_mouse_pressed = false;

    void calc_hue(Gfx::IntPoint const&);
};

}
