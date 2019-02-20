#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GWidget.h>

//#define GBOXLAYOUT_DEBUG

GBoxLayout::GBoxLayout(Orientation orientation)
    : m_orientation(orientation)
{
}

GBoxLayout::~GBoxLayout()
{
}

#if 0
Size GLayout::compute_preferred_size() const
{

}


static Size compute_preferred_size(GLayout::Entry& entry)
{
    if (entry.layout)
        return entry.layout->compute_preferred_size();
    else {
        return entry.widget->preferred_size();
    }
}
#endif

void GBoxLayout::run(GWidget& widget)
{
    if (m_entries.is_empty())
        return;

    Size available_size = widget.size();
    int number_of_entries_with_fixed_size = 0;

    for (auto& entry : m_entries) {
        if (entry.widget && entry.widget->size_policy(orientation()) == SizePolicy::Fixed) {
            available_size -= entry.widget->preferred_size();
            ++number_of_entries_with_fixed_size;
        }
    }

    int number_of_entries_with_automatic_size = m_entries.size() - number_of_entries_with_fixed_size;

#ifdef GBOXLAYOUT_DEBUG
    dbgprintf("GBoxLayout: available_size=%d, fixed=%d, fill=%d\n", available_size.height(), number_of_entries_with_fixed_size, number_of_entries_with_automatic_size);
#endif

    Size automatic_size;

    if (number_of_entries_with_automatic_size) {
        if (m_orientation == Orientation::Horizontal) {
            automatic_size.set_width(available_size.width() / number_of_entries_with_automatic_size);
            automatic_size.set_height(widget.height());
        } else {
            automatic_size.set_width(widget.width());
            automatic_size.set_height(available_size.height() / number_of_entries_with_automatic_size);
        }
    }

#ifdef GBOXLAYOUT_DEBUG
    dbgprintf("GBoxLayout: automatic_size=%s\n", automatic_size.to_string().characters());
#endif

    // FIXME: We should also respect the bottom and right margins.
    int current_x = margins().left();
    int current_y = margins().top();

    for (auto& entry : m_entries) {
        Rect rect(current_x, current_y, 0, 0);
        if (entry.layout) {
            // FIXME: Implement recursive layout.
            ASSERT_NOT_REACHED();
        }
        ASSERT(entry.widget);
        rect.set_size(automatic_size);
        if (entry.widget->size_policy(Orientation::Vertical) == SizePolicy::Fixed)
            rect.set_height(entry.widget->preferred_size().height());
        if (entry.widget->size_policy(Orientation::Horizontal) == SizePolicy::Fixed)
            rect.set_width(entry.widget->preferred_size().height());

#ifdef GBOXLAYOUT_DEBUG
        dbgprintf("GBoxLayout: apply, %s{%p} <- %s\n", entry.widget->class_name(), entry.widget.ptr(), rect.to_string().characters());
#endif
        entry.widget->set_relative_rect(rect);

        if (orientation() == Orientation::Horizontal)
            current_x += rect.width() + spacing();
        else
            current_y += rect.height() + spacing();
    }
}
