#pragma once

#include "VBWidgetType.h"
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibDraw/Rect.h>

class GPainter;
class GVariant;
class GWidget;
class VBForm;
class VBProperty;
class VBWidgetPropertyModel;

enum class Direction {
    None,
    Left,
    UpLeft,
    Up,
    UpRight,
    Right,
    DownRight,
    Down,
    DownLeft
};
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

class VBWidget : public RefCounted<VBWidget>
    , public Weakable<VBWidget> {
    friend class VBWidgetPropertyModel;

public:
    static NonnullRefPtr<VBWidget> create(VBWidgetType type, VBForm& form) { return adopt(*new VBWidget(type, form)); }
    ~VBWidget();

    bool is_selected() const;

    Rect rect() const;
    void set_rect(const Rect&);

    Rect grabber_rect(Direction) const;
    Direction grabber_at(const Point&) const;

    GWidget* gwidget() { return m_gwidget; }

    VBProperty& property(const String&);

    void for_each_property(Function<void(VBProperty&)>);

    VBWidgetPropertyModel& property_model() { return *m_property_model; }

    void setup_properties();
    void synchronize_properties();

    void property_did_change();

    Rect transform_origin_rect() const { return m_transform_origin_rect; }
    void capture_transform_origin_rect();

private:
    VBWidget(VBWidgetType, VBForm&);

    void add_property(const String& name, Function<GVariant(const GWidget&)>&& getter, Function<void(GWidget&, const GVariant&)>&& setter);

    VBWidgetType m_type { VBWidgetType::None };
    VBForm& m_form;
    GWidget* m_gwidget { nullptr };
    NonnullOwnPtrVector<VBProperty> m_properties;
    NonnullRefPtr<VBWidgetPropertyModel> m_property_model;
    Rect m_transform_origin_rect;
};
