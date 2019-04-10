#include "VBForm.h"
#include <LibGUI/GPainter.h>

VBForm::VBForm(const String& name, GWidget* parent)
    : GWidget(parent)
{
    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);
}

VBForm::~VBForm()
{
}

void VBForm::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    for (int y = 0; y < height(); y += m_grid_size) {
        for (int x = 0; x < width(); x += m_grid_size) {
            painter.set_pixel({ x, y }, Color::Black);
        }
    }
}
