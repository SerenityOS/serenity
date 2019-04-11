#pragma once

#include <SharedGraphics/Rect.h>
#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <AK/Weakable.h>
#include <AK/HashMap.h>
#include <AK/Function.h>
#include "VBWidgetType.h"

class GPainter;
class GWidget;
class VBForm;
class VBProperty;
class VBWidgetPropertyModel;

enum class Direction { None, Left, UpLeft, Up, UpRight, Right, DownRight, Down, DownLeft };
template<typename Callback>
inline void for_each_direction(Callback callback)
{
    callback(Direction::Left);
    callback(Direction::UpLeft);
    callback(Direction::Up);
    callback(Direction::UpRight);
    callback(Direction::Right);
    callback(Direction::DownRight);
    callback(Direction::Down);
    callback(Direction::DownLeft);
}

class VBWidget : public Retainable<VBWidget>, public Weakable<VBWidget> {
    friend class VBWidgetPropertyModel;
public:
    static Retained<VBWidget> create(VBWidgetType type, VBForm& form) { return adopt(*new VBWidget(type, form)); }
    ~VBWidget();

    bool is_selected() const;

    Rect rect() const;
    void set_rect(const Rect&);

    Rect grabber_rect(Direction) const;
    Direction grabber_at(const Point&) const;

    GWidget* gwidget() { return m_gwidget; }

    const VBProperty* property_by_name(const String&) const;
    VBProperty* property_by_name(const String&);

    void for_each_property(Function<void(VBProperty&)>);

    VBWidgetPropertyModel& property_model() { return *m_property_model; }

protected:
    VBWidget(VBWidgetType, VBForm&);

private:
    VBWidgetType m_type { VBWidgetType::None };
    VBForm& m_form;
    GWidget* m_gwidget { nullptr };
    Vector<OwnPtr<VBProperty>> m_properties;
    Retained<VBWidgetPropertyModel> m_property_model;
};
