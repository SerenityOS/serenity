#pragma once

#include <LibRx/BehaviorSubject.h>
#include <LibGUI/Widget.h>

template<typename TParent, typename TWidget, typename T>
void bind_widget(
    TParent& parent,
    const char* widget_name,
    Function<NonnullRefPtr<Rx::BehaviorSubject<T>>(TWidget& target)> const& target_property_selector,
    NonnullRefPtr<Rx::BehaviorSubject<T>> source_property) requires IsBaseOf<GUI::Widget, TParent>&& IsBaseOf<GUI::Widget, TWidget>
{
    auto widget = parent.template find_descendant_of_type_named<TWidget>(widget_name);
    auto target_property = target_property_selector(*widget);
    source_property->bind(target_property);
}

template<typename TParent, typename TWidget, typename T>
void bind_widget_oneway(
    TParent& parent,
    const char* widget_name,
    Function<NonnullRefPtr<Rx::BehaviorSubject<T>>(TWidget& target)> const& target_property_selector,
    NonnullRefPtr<Rx::Observable<T>> source_property) requires IsBaseOf<GUI::Widget, TParent>&& IsBaseOf<GUI::Widget, TWidget>
{
    auto widget = parent.template find_descendant_of_type_named<TWidget>(widget_name);
    auto target_property = target_property_selector(*widget);
    source_property->bind_oneway(target_property);
}

template<typename TParent, typename TTargetWidget, typename TSourceWidget, typename T>
requires IsBaseOf<GUI::Widget, TParent>&& IsBaseOf<GUI::Widget, TTargetWidget>&& IsBaseOf<GUI::Widget, TSourceWidget> void bind_widget_to_widget(
    TParent& parent,
    const char* target_widget_name,
    Function<NonnullRefPtr<Rx::BehaviorSubject<T>>(TTargetWidget& target)> const& target_property_selector,
    const char* source_widget_name,
    Function<NonnullRefPtr<Rx::BehaviorSubject<T>>(TSourceWidget& source)> const& source_property_selector)
{
    auto source_widget = parent.template find_descendant_of_type_named<TSourceWidget>(source_widget_name);
    auto source_property = source_property_selector(*source_widget);
    bind_widget<TParent, TTargetWidget, T>(parent, target_widget_name, target_property_selector, source_property);
}
