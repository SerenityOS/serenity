/*
 * Copyright (c) 2015, 2021, Red Hat, Inc. All rights reserved.
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
 *
 */

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHROOTPROCESSOR_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHROOTPROCESSOR_HPP

#include "code/codeCache.hpp"
#include "gc/shared/oopStorageSetParState.hpp"
#include "gc/shenandoah/shenandoahCodeRoots.hpp"
#include "gc/shenandoah/shenandoahHeap.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahSharedVariables.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "memory/iterator.hpp"

template <bool CONCURRENT>
class ShenandoahVMWeakRoots {
private:
  OopStorageSetWeakParState<CONCURRENT, false /* is_const */> _weak_roots;
  ShenandoahPhaseTimings::Phase                               _phase;

public:
  ShenandoahVMWeakRoots(ShenandoahPhaseTimings::Phase phase);

  template <typename T>
  void oops_do(T* cl, uint worker_id);

  template <typename IsAlive, typename KeepAlive>
  void weak_oops_do(IsAlive* is_alive, KeepAlive* keep_alive, uint worker_id);

  void report_num_dead();
};

template <bool CONCURRENT>
class ShenandoahVMRoots {
private:
  OopStorageSetStrongParState<CONCURRENT, false /* is_const */> _strong_roots;
  ShenandoahPhaseTimings::Phase                                 _phase;

public:
  ShenandoahVMRoots(ShenandoahPhaseTimings::Phase phase);

  template <typename T>
  void oops_do(T* cl, uint worker_id);
};

class ShenandoahJavaThreadsIterator {
private:
  static const uint _chunks_per_worker = 16; // educated guess

  ThreadsListHandle             _threads;
  uint const                    _length;
  uint const                    _stride;
  volatile uint                 _claimed;
  ShenandoahPhaseTimings::Phase _phase;

  uint claim();
public:
  ShenandoahJavaThreadsIterator(ShenandoahPhaseTimings::Phase phase, uint n_workers);
  void threads_do(ThreadClosure* cl, uint worker_id);

  uint length() const { return _length; }
  Thread* thread_at(uint index) const { return _threads.thread_at(index); }
};

class ShenandoahThreadRoots {
private:
  ShenandoahPhaseTimings::Phase _phase;
  const bool _is_par;
public:
  ShenandoahThreadRoots(ShenandoahPhaseTimings::Phase phase, bool is_par);
  ~ShenandoahThreadRoots();

  void oops_do(OopClosure* oops_cl, CodeBlobClosure* code_cl, uint worker_id);
  void threads_do(ThreadClosure* tc, uint worker_id);
};

class ShenandoahCodeCacheRoots {
private:
  ShenandoahPhaseTimings::Phase _phase;
  ShenandoahCodeRootsIterator   _coderoots_iterator;
public:
  ShenandoahCodeCacheRoots(ShenandoahPhaseTimings::Phase phase);
  ~ShenandoahCodeCacheRoots();

  void code_blobs_do(CodeBlobClosure* blob_cl, uint worker_id);
};

template <bool CONCURRENT, bool SINGLE_THREADED>
class ShenandoahClassLoaderDataRoots {
private:
  ShenandoahSharedSemaphore     _semaphore;
  ShenandoahPhaseTimings::Phase _phase;

  static uint worker_count(uint n_workers) {
    if (SINGLE_THREADED) return 1u;

    // Limit concurrency a bit, otherwise it wastes resources when workers are tripping
    // over each other. This also leaves free workers to process other parts of the root
    // set, while admitted workers are busy with doing the CLDG walk.
    return MAX2(1u, MIN2(ShenandoahSharedSemaphore::max_tokens(), n_workers / 2));
  }

public:
  ShenandoahClassLoaderDataRoots(ShenandoahPhaseTimings::Phase phase, uint n_workers);
  ~ShenandoahClassLoaderDataRoots();

  void always_strong_cld_do(CLDClosure* clds, uint worker_id);
  void cld_do(CLDClosure* clds, uint worker_id);

private:
  typedef void (*CldDo)(CLDClosure*);
  void cld_do_impl(CldDo f, CLDClosure* clds, uint worker_id);
};

class ShenandoahRootProcessor : public StackObj {
private:
  ShenandoahHeap* const               _heap;
  const ShenandoahPhaseTimings::Phase _phase;
  const ShenandoahGCWorkerPhase       _worker_phase;
public:
  ShenandoahRootProcessor(ShenandoahPhaseTimings::Phase phase);

  ShenandoahHeap* heap() const { return _heap; }
};

class ShenandoahRootScanner : public ShenandoahRootProcessor {
private:
  ShenandoahThreadRoots                                     _thread_roots;

public:
  ShenandoahRootScanner(uint n_workers, ShenandoahPhaseTimings::Phase phase);
  ~ShenandoahRootScanner();

  void roots_do(uint worker_id, OopClosure* cl);

private:
  void roots_do(uint worker_id, OopClosure* oops, CodeBlobClosure* code, ThreadClosure* tc = NULL);
};

// STW root scanner
class ShenandoahSTWRootScanner : public ShenandoahRootProcessor {
private:
  ShenandoahThreadRoots           _thread_roots;
  ShenandoahCodeCacheRoots        _code_roots;
  ShenandoahClassLoaderDataRoots<false /*concurrent*/, false /* single_thread*/>
                                  _cld_roots;
  ShenandoahVMRoots<false /*concurrent*/>
                                  _vm_roots;
  const bool                      _unload_classes;
public:
  ShenandoahSTWRootScanner(ShenandoahPhaseTimings::Phase phase);

  template <typename T>
  void roots_do(T* oops, uint worker_id);
};

class ShenandoahConcurrentRootScanner : public ShenandoahRootProcessor {
private:
  ShenandoahJavaThreadsIterator             _java_threads;
  ShenandoahVMRoots<true /*concurrent*/>    _vm_roots;
  ShenandoahClassLoaderDataRoots<true /*concurrent*/, false /* single-threaded*/>
                                           _cld_roots;
  ShenandoahNMethodTableSnapshot*          _codecache_snapshot;
  ShenandoahPhaseTimings::Phase            _phase;

public:
  ShenandoahConcurrentRootScanner(uint n_workers, ShenandoahPhaseTimings::Phase phase);
  ~ShenandoahConcurrentRootScanner();

  void roots_do(OopClosure* oops, uint worker_id);

private:
  void update_tlab_stats();
};

// This scanner is only for SH::object_iteration() and only supports single-threaded
// root scanning
class ShenandoahHeapIterationRootScanner : public ShenandoahRootProcessor {
private:
  ShenandoahThreadRoots                                    _thread_roots;
  ShenandoahVMRoots<false /*concurrent*/>                  _vm_roots;
  ShenandoahClassLoaderDataRoots<false /*concurrent*/, true /*single threaded*/>
                                                           _cld_roots;
  ShenandoahVMWeakRoots<false /*concurrent*/>              _weak_roots;
  ShenandoahCodeCacheRoots                                 _code_roots;

public:
  ShenandoahHeapIterationRootScanner();

  void roots_do(OopClosure* cl);
};

// Update all roots at a safepoint
class ShenandoahRootUpdater : public ShenandoahRootProcessor {
private:
  ShenandoahVMRoots<false /*concurrent*/>                   _vm_roots;
  ShenandoahClassLoaderDataRoots<false /*concurrent*/, false /*single threaded*/>
                                                            _cld_roots;
  ShenandoahThreadRoots                                     _thread_roots;
  ShenandoahVMWeakRoots<false /*concurrent*/>               _weak_roots;
  ShenandoahCodeCacheRoots                                  _code_roots;

public:
  ShenandoahRootUpdater(uint n_workers, ShenandoahPhaseTimings::Phase phase);

  template<typename IsAlive, typename KeepAlive>
  void roots_do(uint worker_id, IsAlive* is_alive, KeepAlive* keep_alive);
};

// Adjuster all roots at a safepoint during full gc
class ShenandoahRootAdjuster : public ShenandoahRootProcessor {
private:
  ShenandoahVMRoots<false /*concurrent*/>                   _vm_roots;
  ShenandoahClassLoaderDataRoots<false /*concurrent*/, false /*single threaded*/>
                                                            _cld_roots;
  ShenandoahThreadRoots                                     _thread_roots;
  ShenandoahVMWeakRoots<false /*concurrent*/>               _weak_roots;
  ShenandoahCodeCacheRoots                                  _code_roots;

public:
  ShenandoahRootAdjuster(uint n_workers, ShenandoahPhaseTimings::Phase phase);

  void roots_do(uint worker_id, OopClosure* oops);
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHROOTPROCESSOR_HPP
