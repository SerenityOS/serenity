#include "PaletteWidget.h"
#include "PaintableWidget.h"
#include <LibGUI/GBoxLayout.h>

class ColorWidget : public GWidget {
public:
    explicit ColorWidget(Color color, PaletteWidget& palette_widget, GWidget* parent)
        : GWidget(parent)
        , m_palette_widget(palette_widget)
        , m_color(color)
    {
    }

    virtual ~ColorWidget() override
    {
    }

    virtual void mousedown_event(GMouseEvent& event) override
    {
        if (event.button() == GMouseButton::Left)
            m_palette_widget.set_primary_color(m_color);
        else if (event.button() == GMouseButton::Right)
            m_palette_widget.set_secondary_color(m_color);
    }

private:
    PaletteWidget& m_palette_widget;
    Color m_color;
};

PaletteWidget::PaletteWidget(PaintableWidget& paintable_widget, GWidget* parent)
    : GFrame(parent)
    , m_paintable_widget(paintable_widget)
{
    set_frame_shape(FrameShape::Panel);
    set_frame_shadow(FrameShadow::Raised);
    set_frame_thickness(1);
    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);

    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size({ 0, 32 });

    m_secondary_color_widget = new GWidget(this);
    m_secondary_color_widget->set_relative_rect({ 2, 2, 60, 28 });
    m_secondary_color_widget->set_fill_with_background_color(true);
    set_secondary_color(paintable_widget.secondary_color());

    m_primary_color_widget = new GWidget(this);
    Rect rect { 0, 0, 38, 14 };
    rect.center_within(m_secondary_color_widget->relative_rect());
    m_primary_color_widget->set_relative_rect(rect);
    m_primary_color_widget->set_fill_with_background_color(true);
    set_primary_color(paintable_widget.primary_color());

    auto* color_container = new GWidget(this);
    color_container->set_relative_rect(m_secondary_color_widget->relative_rect().right() + 2, 2, 500, 28);
    color_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    color_container->layout()->set_spacing(0);

    auto add_color_widget = [&] (Color color) {
        auto* color_widget = new ColorWidget(color, *this, color_container);
        color_widget->set_fill_with_background_color(true);
        color_widget->set_background_color(color);
    };

    add_color_widget(Color::Black);
    add_color_widget(Color::White);
    add_color_widget(Color::Red);
    add_color_widget(Color::Green);
    add_color_widget(Color::Blue);
    add_color_widget(Color::Cyan);
    add_color_widget(Color::Magenta);
    add_color_widget(Color::Yellow);
}

PaletteWidget::~PaletteWidget()
{
}

void PaletteWidget::set_primary_color(Color color)
{
    m_paintable_widget.set_primary_color(color);
    m_primary_color_widget->set_background_color(color);
    m_primary_color_widget->update();
}

void PaletteWidget::set_secondary_color(Color color)
{
    m_paintable_widget.set_secondary_color(color);
    m_secondary_color_widget->set_background_color(color);
    m_secondary_color_widget->update();
}
