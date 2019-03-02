#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <LibGUI/GShortcut.h>

class GAction : public Retainable<GAction> {
public:
    static Retained<GAction> create(const String& text, Function<void(const GAction&)> callback)
    {
        return adopt(*new GAction(text, move(callback)));
    }
    static Retained<GAction> create(const String& text, const String& custom_data, Function<void(const GAction&)> callback)
    {
        return adopt(*new GAction(text, custom_data, move(callback)));
    }
    static Retained<GAction> create(const String& text, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> callback)
    {
        return adopt(*new GAction(text, move(icon), move(callback)));
    }
    static Retained<GAction> create(const String& text, const GShortcut& shortcut, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> callback)
    {
        return adopt(*new GAction(text, shortcut, move(icon), move(callback)));
    }
    ~GAction();

    String text() const { return m_text; }
    GShortcut shortcut() const { return m_shortcut; }
    String custom_data() const { return m_custom_data; }
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }

    Function<void(GAction&)> on_activation;

    void activate();

private:
    GAction(const String& text, Function<void(const GAction&)> = nullptr);
    GAction(const String& text, const GShortcut&, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> = nullptr);
    GAction(const String& text, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> = nullptr);
    GAction(const String& text, const String& custom_data = String(), Function<void(const GAction&)> = nullptr);

    String m_text;
    String m_custom_data;
    RetainPtr<GraphicsBitmap> m_icon;
    GShortcut m_shortcut;
};

