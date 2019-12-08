#pragma once

#include <LibCore/CEventLoop.h>
#include <LibCore/CObject.h>

class GWindowServerConnection;

class GDragOperation : public CObject {
    C_OBJECT(GDragOperation)
public:
    enum class Outcome {
        None,
        Accepted,
        Cancelled,
    };

    virtual ~GDragOperation() override;

    void set_text(const String& text) { m_text = text; }

    Outcome exec();
    Outcome outcome() const { return m_outcome; }

    static void notify_accepted(Badge<GWindowServerConnection>);
    static void notify_cancelled(Badge<GWindowServerConnection>);

protected:
    explicit GDragOperation(CObject* parent = nullptr);

private:
    void done(Outcome);

    OwnPtr<CEventLoop> m_event_loop;
    Outcome m_outcome { Outcome::None };
    String m_text;
};
