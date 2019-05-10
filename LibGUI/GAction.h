#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <AK/Weakable.h>
#include <AK/WeakPtr.h>
#include <AK/Badge.h>
#include <AK/HashTable.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <LibGUI/GShortcut.h>

class GButton;
class GMenuItem;
class GWidget;

class GAction : public Retainable<GAction>, public Weakable<GAction> {
public:
    enum class ShortcutScope {
        None,
        ApplicationGlobal,
        WidgetLocal,
    };
    static Retained<GAction> create(const String& text, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, move(callback), widget));
    }
    static Retained<GAction> create(const String& text, const String& custom_data, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, custom_data, move(callback), widget));
    }
    static Retained<GAction> create(const String& text, RetainPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, move(icon), move(callback), widget));
    }
    static Retained<GAction> create(const String& text, const GShortcut& shortcut, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, shortcut, move(callback), widget));
    }
    static Retained<GAction> create(const String& text, const GShortcut& shortcut, RetainPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, shortcut, move(icon), move(callback), widget));
    }
    ~GAction();

    GWidget* widget() { return m_widget.ptr(); }
    const GWidget* widget() const { return m_widget.ptr(); }

    String text() const { return m_text; }
    GShortcut shortcut() const { return m_shortcut; }
    String custom_data() const { return m_custom_data; }
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }

    Function<void(GAction&)> on_activation;

    void activate();

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const { ASSERT(is_checkable()); return m_checked; }
    void set_checked(bool);

    void register_button(Badge<GButton>, GButton&);
    void unregister_button(Badge<GButton>, GButton&);
    void register_menu_item(Badge<GMenuItem>, GMenuItem&);
    void unregister_menu_item(Badge<GMenuItem>, GMenuItem&);

private:
    GAction(const String& text, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const String& text, const GShortcut&, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const String& text, const GShortcut&, RetainPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const String& text, RetainPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const String& text, const String& custom_data = String(), Function<void(GAction&)> = nullptr, GWidget* = nullptr);

    template<typename Callback> void for_each_toolbar_button(Callback);
    template<typename Callback> void for_each_menu_item(Callback);

    String m_text;
    String m_custom_data;
    RetainPtr<GraphicsBitmap> m_icon;
    GShortcut m_shortcut;
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    ShortcutScope m_scope { ShortcutScope::None };

    HashTable<GButton*> m_buttons;
    HashTable<GMenuItem*> m_menu_items;
    WeakPtr<GWidget> m_widget;
};
