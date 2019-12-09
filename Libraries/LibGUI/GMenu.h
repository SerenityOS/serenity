#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/CObject.h>
#include <LibGUI/GMenuItem.h>

class GAction;
class Point;

class GMenu final : public CObject {
    C_OBJECT(GMenu)
public:
    explicit GMenu(const StringView& name = "");
    virtual ~GMenu() override;

    static GMenu* from_menu_id(int);

    const String& name() const { return m_name; }

    GAction* action_at(int);

    void add_action(NonnullRefPtr<GAction>);
    void add_separator();
    void add_submenu(NonnullRefPtr<GMenu>);

    void popup(const Point& screen_position);
    void dismiss();

    Function<void(unsigned)> on_item_activation;

private:
    friend class GMenuBar;

    int menu_id() const { return m_menu_id; }
    int realize_menu();
    void unrealize_menu();
    void realize_if_needed();

    int m_menu_id { -1 };
    String m_name;
    NonnullOwnPtrVector<GMenuItem> m_items;
};
