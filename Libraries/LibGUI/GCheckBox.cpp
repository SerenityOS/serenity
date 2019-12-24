#include <Kernel/KeyCode.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GPainter.h>
#include <LibDraw/CharacterBitmap.h>
#include <LibDraw/StylePainter.h>

static const char* s_checked_bitmap_data = {
    "         "
    "       # "
    "      ## "
    "     ### "
    " ## ###  "
    " #####   "
    "  ###    "
    "   #     "
    "         "
};

static CharacterBitmap* s_checked_bitmap;
static const int s_checked_bitmap_width = 9;
static const int s_checked_bitmap_height = 9;
static const int s_box_width = 13;
static const int s_box_height = 13;

GCheckBox::GCheckBox(GWidget* parent)
    : GAbstractButton(parent)
{
}

GCheckBox::GCheckBox(const StringView& text, GWidget* parent)
    : GAbstractButton(text, parent)
{
}

GCheckBox::~GCheckBox()
{
}

void GCheckBox::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    auto text_rect = rect();
    text_rect.set_left(s_box_width + 4);
    text_rect.set_width(font().width(text()));
    text_rect.set_top(height() / 2 - font().glyph_height() / 2);
    text_rect.set_height(font().glyph_height());

    if (fill_with_background_color())
        painter.fill_rect(rect(), background_color());

    Rect box_rect {
        0, height() / 2 - s_box_height / 2 - 1,
        s_box_width, s_box_height
    };
    painter.fill_rect(box_rect, SystemColor::Base);
    StylePainter::paint_frame(painter, box_rect, FrameShape::Container, FrameShadow::Sunken, 2);

    if (is_being_pressed())
        painter.draw_rect(box_rect.shrunken(4, 4), Color::MidGray);

    if (is_checked()) {
        if (!s_checked_bitmap)
            s_checked_bitmap = &CharacterBitmap::create_from_ascii(s_checked_bitmap_data, s_checked_bitmap_width, s_checked_bitmap_height).leak_ref();
        painter.draw_bitmap(box_rect.shrunken(4, 4).location(), *s_checked_bitmap, SystemColor::ButtonText);
    }

    paint_text(painter, text_rect, font(), TextAlignment::TopLeft);
}

void GCheckBox::click()
{
    if (!is_enabled())
        return;
    set_checked(!is_checked());
}
