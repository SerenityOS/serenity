#include "IconWidget.h"
#include <LibGfx/Bitmap.h>

IconWidget::IconWidget()
{
    set_icon(Gfx::Bitmap::load_from_file("/res/icons/clipboard.png"));
    set_fill_with_background_color(true);
}

IconWidget::~IconWidget()
{
}

void IconWidget::mousedown_event(GUI::MouseEvent&)
{
    if (on_click)
        on_click();
}
