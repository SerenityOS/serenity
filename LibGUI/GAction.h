#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <SharedGraphics/GraphicsBitmap.h>

class GAction : public Retainable<GAction> {
public:
    static RetainPtr<GAction> create(const String& text, Function<void(const GAction&)> callback)
    {
        return adopt(*new GAction(text, move(callback)));
    }
    static RetainPtr<GAction> create(const String& text, const String& custom_data, Function<void(const GAction&)> callback)
    {
        return adopt(*new GAction(text, custom_data, move(callback)));
    }
    static RetainPtr<GAction> create(const String& text, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> callback)
    {
        return adopt(*new GAction(text, move(icon), move(callback)));
    }
    ~GAction();

    String text() const { return m_text; }
    String custom_data() const { return m_custom_data; }
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }

    Function<void(GAction&)> on_activation;

    void activate();

private:
    GAction(const String& text, Function<void(const GAction&)> = nullptr);
    GAction(const String& text, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> = nullptr);
    GAction(const String& text, const String& custom_data = String(), Function<void(const GAction&)> = nullptr);

    String m_text;
    String m_custom_data;
    RetainPtr<GraphicsBitmap> m_icon;
};

