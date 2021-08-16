/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, SAP and/or its affiliates.
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

#ifndef SHARE_MEMORY_METASPACE_PRINTMETASPACEINFOKLASSCLOSURE_HPP
#define SHARE_MEMORY_METASPACE_PRINTMETASPACEINFOKLASSCLOSURE_HPP

#include "memory/iterator.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;
class InstanceKlass;

namespace metaspace {

// Helper class for MetaspaceUtils::print_report()
class PrintMetaspaceInfoKlassClosure : public KlassClosure {
private:
  outputStream* const _out;
  uintx _cnt;

  bool print_reflection_invocation_target(outputStream* out, InstanceKlass* magic_accessor_impl_class);

public:

  PrintMetaspaceInfoKlassClosure(outputStream* out, bool do_print);
  void do_klass(Klass* k);

}; // end: PrintKlassInfoClosure

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_PRINTMETASPACEINFOKLASSCLOSURE_HPP
