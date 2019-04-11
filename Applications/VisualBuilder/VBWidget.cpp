#include "VBWidget.h"
#include "VBForm.h"
#include <LibGUI/GPainter.h>

VBWidget::VBWidget(VBForm& form)
    : m_form(form)
{
}

VBWidget::~VBWidget()
{
}

bool VBWidget::is_selected() const
{
    return m_form.is_selected(*this);
}

Rect VBWidget::grabber_rect(Direction direction) const
{
    int grabber_size = 5;
    int half_grabber_size = grabber_size / 2;
    switch (direction) {
    case Direction::Left:
        return { m_rect.x() - half_grabber_size, m_rect.center().y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::UpLeft:
        return { m_rect.x() - half_grabber_size, m_rect.y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::Up:
        return { m_rect.center().x() - half_grabber_size, m_rect.y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::UpRight:
        return { m_rect.right() - half_grabber_size, m_rect.y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::Right:
        return { m_rect.right() - half_grabber_size, m_rect.center().y() - half_grabber_size, grabber_size, grabber_size };
    case Direction::DownLeft:
        return { m_rect.x() - half_grabber_size, m_rect.bottom() - half_grabber_size, grabber_size, grabber_size };
    case Direction::Down:
        return { m_rect.center().x() - half_grabber_size, m_rect.bottom() - half_grabber_size, grabber_size, grabber_size };
    case Direction::DownRight:
        return { m_rect.right() - half_grabber_size, m_rect.bottom() - half_grabber_size, grabber_size, grabber_size };
    default:
        ASSERT_NOT_REACHED();
    }
}

Direction VBWidget::grabber_at(const Point& position) const
{
    Direction found_grabber = Direction::None;
    for_each_direction([&] (Direction direction) {
        if (grabber_rect(direction).contains(position))
            found_grabber = direction;
    });
    return found_grabber;
}

void VBWidget::paint(GPainter& painter)
{
    painter.fill_rect(m_rect, Color::White);
    painter.draw_rect(m_rect, Color::Black);
}
