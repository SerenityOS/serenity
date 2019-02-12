#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>

class GAction {
public:
    GAction(const String& text, Function<void(const GAction&)> = nullptr);
    GAction(const String& text, const String& custom_data = String(), Function<void(const GAction&)> = nullptr);
    ~GAction();

    String text() const { return m_text; }
    String custom_data() const { return m_custom_data; }

    Function<void(GAction&)> on_activation;

    void activate();

private:
    String m_text;
    String m_custom_data;
};

