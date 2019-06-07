#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GWindow.h>

GSplitter::GSplitter(Orientation orientation, GWidget* parent)
    : GFrame(parent)
    , m_orientation(orientation)
{
    set_layout(make<GBoxLayout>(orientation));
    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);
    layout()->set_spacing(4);
}

GSplitter::~GSplitter()
{
}

void GSplitter::enter_event(CEvent&)
{
    set_background_color(Color::from_rgb(0xd6d2ce));
    window()->set_override_cursor(m_orientation == Orientation::Horizontal ? GStandardCursor::ResizeHorizontal : GStandardCursor::ResizeVertical);
    update();
}

void GSplitter::leave_event(CEvent&)
{
    set_background_color(Color::LightGray);
    if (!m_resizing)
        window()->set_override_cursor(GStandardCursor::None);
    update();
}

void GSplitter::mousedown_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    m_resizing = true;
    int x_or_y = m_orientation == Orientation::Horizontal ? event.x() : event.y();
    GWidget* first_resizee { nullptr };
    GWidget* second_resizee { nullptr };
    int fudge = layout()->spacing();
    for_each_child_widget([&](auto& child) {
        int child_start = m_orientation == Orientation::Horizontal ? child.relative_rect().left() : child.relative_rect().top();
        int child_end = m_orientation == Orientation::Horizontal ? child.relative_rect().right() : child.relative_rect().bottom();
        if (x_or_y > child_end && (x_or_y - fudge) <= child_end)
            first_resizee = &child;
        if (x_or_y < child_start && (x_or_y + fudge) >= child_start)
            second_resizee = &child;
        return IterationDecision::Continue;
    });
    ASSERT(first_resizee && second_resizee);
    m_first_resizee = first_resizee->make_weak_ptr();
    m_second_resizee = second_resizee->make_weak_ptr();
    m_first_resizee_start_size = first_resizee->size();
    m_second_resizee_start_size = second_resizee->size();
    m_resize_origin = event.position();
}

void GSplitter::mousemove_event(GMouseEvent& event)
{
    if (!m_resizing)
        return;
    auto delta = event.position() - m_resize_origin;
    if (!m_first_resizee || !m_second_resizee) {
        // One or both of the resizees were deleted during an ongoing resize, screw this.
        m_resizing = false;
        return;
        ;
    }
    int minimum_size = 0;
    auto new_first_resizee_size = m_first_resizee_start_size;
    auto new_second_resizee_size = m_second_resizee_start_size;
    if (m_orientation == Orientation::Horizontal) {
        new_first_resizee_size.set_width(new_first_resizee_size.width() + delta.x());
        new_second_resizee_size.set_width(new_second_resizee_size.width() - delta.x());

        if (new_first_resizee_size.width() < minimum_size) {
            int correction = minimum_size - new_first_resizee_size.width();
            new_first_resizee_size.set_width(new_first_resizee_size.width() + correction);
            new_second_resizee_size.set_width(new_second_resizee_size.width() - correction);
        }
        if (new_second_resizee_size.width() < minimum_size) {
            int correction = minimum_size - new_second_resizee_size.width();
            new_second_resizee_size.set_width(new_second_resizee_size.width() + correction);
            new_first_resizee_size.set_width(new_first_resizee_size.width() - correction);
        }
    } else {
        new_first_resizee_size.set_height(new_first_resizee_size.height() + delta.y());
        new_second_resizee_size.set_height(new_second_resizee_size.height() - delta.y());

        if (new_first_resizee_size.height() < minimum_size) {
            int correction = minimum_size - new_first_resizee_size.height();
            new_first_resizee_size.set_height(new_first_resizee_size.height() + correction);
            new_second_resizee_size.set_height(new_second_resizee_size.height() - correction);
        }
        if (new_second_resizee_size.height() < minimum_size) {
            int correction = minimum_size - new_second_resizee_size.height();
            new_second_resizee_size.set_height(new_second_resizee_size.height() + correction);
            new_first_resizee_size.set_height(new_first_resizee_size.height() - correction);
        }
    }
    m_first_resizee->set_preferred_size(new_first_resizee_size);
    m_second_resizee->set_preferred_size(new_second_resizee_size);

    invalidate_layout();
}

void GSplitter::mouseup_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    m_resizing = false;
    if (!rect().contains(event.position()))
        window()->set_override_cursor(GStandardCursor::None);
}
