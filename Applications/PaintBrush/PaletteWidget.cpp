#include "PaletteWidget.h"
#include "PaintableWidget.h"

PaletteWidget::PaletteWidget(PaintableWidget& paintable_widget, GWidget* parent)
    : GFrame(parent)
{
    set_frame_shape(FrameShape::Panel);
    set_frame_shadow(FrameShadow::Raised);
    set_frame_thickness(1);
    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);

    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size({ 0, 32 });

    auto* secondary_color_widget = new GWidget(this);
    secondary_color_widget->set_relative_rect({ 2, 2, 60, 28 });
    secondary_color_widget->set_fill_with_background_color(true);
    secondary_color_widget->set_background_color(paintable_widget.secondary_color());

    auto* primary_color_widget = new GWidget(this);
    Rect rect { 0, 0, 38, 14 };
    rect.center_within(secondary_color_widget->relative_rect());
    primary_color_widget->set_relative_rect(rect);
    primary_color_widget->set_fill_with_background_color(true);
    primary_color_widget->set_background_color(paintable_widget.primary_color());
}

PaletteWidget::~PaletteWidget()
{
}
