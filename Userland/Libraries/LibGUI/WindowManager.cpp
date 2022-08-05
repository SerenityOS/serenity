/*
 * Copyright (c) 2022, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/IDAllocator.h>
#include <AK/NeverDestroyed.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/WindowManager.h>

namespace GUI {

static IDAllocator s_wm_allocator;

static NeverDestroyed<HashMap<int, WindowManager*>> wms;

WindowManager* WindowManager::from_wm_id(int wm_id)
{
    auto it = wms->find(wm_id);
    if (it != wms->end())
        return (*it).value;
    return nullptr;
}

WindowManager::WindowManager(Core::Object* parent)
    : Core::Object(parent)
{
    m_wm_id = s_wm_allocator.allocate();
    wms->set(m_wm_id, this);
}

WindowManager::~WindowManager()
{
    wms->remove(m_wm_id);
}

}
