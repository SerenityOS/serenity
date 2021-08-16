/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "precompiled.hpp"
#include "gc/z/zTask.hpp"
#include "gc/z/zThread.hpp"

ZTask::GangTask::GangTask(ZTask* ztask, const char* name) :
    AbstractGangTask(name),
    _ztask(ztask) {}

void ZTask::GangTask::work(uint worker_id) {
  ZThread::set_worker_id(worker_id);
  _ztask->work();
  ZThread::clear_worker_id();
}

ZTask::ZTask(const char* name) :
    _gang_task(this, name) {}

const char* ZTask::name() const {
  return _gang_task.name();
}

AbstractGangTask* ZTask::gang_task() {
  return &_gang_task;
}
