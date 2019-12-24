#include "PaletteWidget.h"
#include "ColorDialog.h"
#include "PaintableWidget.h"
#include <LibGUI/GBoxLayout.h>

class ColorWidget : public GFrame {
    C_OBJECT(ColorWidget)
public:
    explicit ColorWidget(Color color, PaletteWidget& palette_widget, GWidget* parent)
        : GFrame(parent)
        , m_palette_widget(palette_widget)
        , m_color(color)
    {
        set_frame_thickness(2);
        set_frame_shadow(FrameShadow::Sunken);
        set_frame_shape(FrameShape::Container);
    }

    virtual ~ColorWidget() override
    {
    }

    virtual void mousedown_event(GMouseEvent& event) override
    {
        if (event.modifiers() & KeyModifier::Mod_Ctrl && event.button() == GMouseButton::Left) {
            auto dialog = ColorDialog::construct(m_color, window());
            if (dialog->exec() == GDialog::ExecOK) {
                m_color = dialog->color();
                set_background_color(m_color);
                update();
            }
            return;
        }

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
    set_frame_thickness(0);
    set_fill_with_background_color(true);

    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 34);

    m_secondary_color_widget = GFrame::construct(this);
    m_secondary_color_widget->set_frame_thickness(2);
    m_secondary_color_widget->set_frame_shape(FrameShape::Container);
    m_secondary_color_widget->set_frame_shadow(FrameShadow::Sunken);
    m_secondary_color_widget->set_relative_rect({ 2, 2, 60, 31 });
    m_secondary_color_widget->set_fill_with_background_color(true);
    set_secondary_color(paintable_widget.secondary_color());

    m_primary_color_widget = GFrame::construct(this);
    m_primary_color_widget->set_frame_thickness(2);
    m_primary_color_widget->set_frame_shape(FrameShape::Container);
    m_primary_color_widget->set_frame_shadow(FrameShadow::Sunken);
    Rect rect { 0, 0, 38, 15 };
    rect.center_within(m_secondary_color_widget->relative_rect());
    m_primary_color_widget->set_relative_rect(rect);
    m_primary_color_widget->set_fill_with_background_color(true);
    set_primary_color(paintable_widget.primary_color());

    paintable_widget.on_primary_color_change = [this](Color color) {
        set_primary_color(color);
    };

    paintable_widget.on_secondary_color_change = [this](Color color) {
        set_secondary_color(color);
    };

    auto color_container = GWidget::construct(this);
    color_container->set_relative_rect(m_secondary_color_widget->relative_rect().right() + 2, 2, 500, 32);
    color_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    color_container->layout()->set_spacing(1);

    auto top_color_container = GWidget::construct(color_container.ptr());
    top_color_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    top_color_container->layout()->set_spacing(1);

    auto bottom_color_container = GWidget::construct(color_container.ptr());
    bottom_color_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    bottom_color_container->layout()->set_spacing(1);

    auto add_color_widget = [&](GWidget* container, Color color) {
        auto color_widget = ColorWidget::construct(color, *this, container);
        color_widget->set_fill_with_background_color(true);
        color_widget->set_background_color(color);
    };

    add_color_widget(top_color_container, Color::from_rgb(0x000000));
    add_color_widget(top_color_container, Color::from_rgb(0x808080));
    add_color_widget(top_color_container, Color::from_rgb(0x800000));
    add_color_widget(top_color_container, Color::from_rgb(0x808000));
    add_color_widget(top_color_container, Color::from_rgb(0x008000));
    add_color_widget(top_color_container, Color::from_rgb(0x008080));
    add_color_widget(top_color_container, Color::from_rgb(0x000080));
    add_color_widget(top_color_container, Color::from_rgb(0x800080));
    add_color_widget(top_color_container, Color::from_rgb(0x808040));
    add_color_widget(top_color_container, Color::from_rgb(0x004040));
    add_color_widget(top_color_container, Color::from_rgb(0x0080ff));
    add_color_widget(top_color_container, Color::from_rgb(0x004080));
    add_color_widget(top_color_container, Color::from_rgb(0x8000ff));
    add_color_widget(top_color_container, Color::from_rgb(0x804000));

    add_color_widget(bottom_color_container, Color::from_rgb(0xffffff));
    add_color_widget(bottom_color_container, Color::from_rgb(0xc0c0c0));
    add_color_widget(bottom_color_container, Color::from_rgb(0xff0000));
    add_color_widget(bottom_color_container, Color::from_rgb(0xffff00));
    add_color_widget(bottom_color_container, Color::from_rgb(0x00ff00));
    add_color_widget(bottom_color_container, Color::from_rgb(0x00ffff));
    add_color_widget(bottom_color_container, Color::from_rgb(0x0000ff));
    add_color_widget(bottom_color_container, Color::from_rgb(0xff00ff));
    add_color_widget(bottom_color_container, Color::from_rgb(0xffff80));
    add_color_widget(bottom_color_container, Color::from_rgb(0x00ff80));
    add_color_widget(bottom_color_container, Color::from_rgb(0x80ffff));
    add_color_widget(bottom_color_container, Color::from_rgb(0x8080ff));
    add_color_widget(bottom_color_container, Color::from_rgb(0xff0080));
    add_color_widget(bottom_color_container, Color::from_rgb(0xff8040));
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
