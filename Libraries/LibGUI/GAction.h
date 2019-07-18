#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/RefCounted.h>
#include <AK/NonnullRefPtr.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibGUI/GShortcut.h>
#include <LibDraw/GraphicsBitmap.h>

class GActionGroup;
class GButton;
class GMenuItem;
class GWidget;

class GAction : public RefCounted<GAction>
    , public Weakable<GAction> {
public:
    enum class ShortcutScope {
        None,
        ApplicationGlobal,
        WidgetLocal,
    };
    static NonnullRefPtr<GAction> create(const StringView& text, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, move(callback), widget));
    }
    static NonnullRefPtr<GAction> create(const StringView& text, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, move(icon), move(callback), widget));
    }
    static NonnullRefPtr<GAction> create(const StringView& text, const GShortcut& shortcut, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, shortcut, move(callback), widget));
    }
    static NonnullRefPtr<GAction> create(const StringView& text, const GShortcut& shortcut, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, shortcut, move(icon), move(callback), widget));
    }
    ~GAction();

    GWidget* widget() { return m_widget.ptr(); }
    const GWidget* widget() const { return m_widget.ptr(); }

    String text() const { return m_text; }
    GShortcut shortcut() const { return m_shortcut; }
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }

    Function<void(GAction&)> on_activation;

    void activate();

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const
    {
        ASSERT(is_checkable());
        return m_checked;
    }
    void set_checked(bool);

    void register_button(Badge<GButton>, GButton&);
    void unregister_button(Badge<GButton>, GButton&);
    void register_menu_item(Badge<GMenuItem>, GMenuItem&);
    void unregister_menu_item(Badge<GMenuItem>, GMenuItem&);

    const GActionGroup* group() const { return m_action_group.ptr(); }
    void set_group(Badge<GActionGroup>, GActionGroup*);

private:
    GAction(const StringView& text, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const StringView& text, const GShortcut&, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const StringView& text, const GShortcut&, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const StringView& text, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> = nullptr, GWidget* = nullptr);

    template<typename Callback>
    void for_each_toolbar_button(Callback);
    template<typename Callback>
    void for_each_menu_item(Callback);

    String m_text;
    RefPtr<GraphicsBitmap> m_icon;
    GShortcut m_shortcut;
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    ShortcutScope m_scope { ShortcutScope::None };

    HashTable<GButton*> m_buttons;
    HashTable<GMenuItem*> m_menu_items;
    WeakPtr<GWidget> m_widget;
    WeakPtr<GActionGroup> m_action_group;
};
