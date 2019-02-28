#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Function.h>
#include <AK/HashMap.h>

class GScrollBar;
class ProcessTableModel;

class ProcessView final : public GWidget {
public:
    ProcessView(GWidget* parent);
    virtual ~ProcessView() override;

    void reload();

    Function<void(String)> on_status_message;

    int header_height() const { return 16; }
    int item_height() const { return 16; }
    int item_count() const;

    pid_t selected_pid() const;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void timer_event(GTimerEvent&) override;

    void set_status_message(String&&);
    Rect row_rect(int item_index) const;

    RetainPtr<GraphicsBitmap> m_process_icon;
    GScrollBar* m_scrollbar { nullptr };
    OwnPtr<ProcessTableModel> m_model;
};
