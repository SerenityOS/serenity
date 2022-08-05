/*
 * Copyright (c) 2022, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibGUI/Event.h>

namespace GUI {

class WindowManager : public Core::Object {
    C_OBJECT(WindowManager)

public:
    static WindowManager* from_wm_id(int);
    virtual ~WindowManager() override;
    int wm_id() { return m_wm_id; };

protected:
    WindowManager(Core::Object* parent = nullptr);

private:
    int m_wm_id { 0 };
};

}
