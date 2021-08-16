/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.projectcreator;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;

public class FileTreeCreator extends SimpleFileVisitor<Path>
{
   Path vcProjLocation;
   Path startDir;
   final int startDirLength;
   Stack<DirAttributes> attributes = new Stack<DirAttributes>();
   Vector<BuildConfig> allConfigs;
   WinGammaPlatform wg;
   WinGammaPlatformVC10 wg10;

   public FileTreeCreator(Path startDir, Vector<BuildConfig> allConfigs, WinGammaPlatform wg) {
      super();
      this.wg = wg;
      if (wg instanceof WinGammaPlatformVC10) {
          wg10 = (WinGammaPlatformVC10)wg;
      }
      this.allConfigs = allConfigs;
      this.startDir = startDir;
      startDirLength = startDir.toAbsolutePath().toString().length();
      vcProjLocation = FileSystems.getDefault().getPath(allConfigs.firstElement().get("BuildSpace"));
      attributes.push(new DirAttributes());
   }

   public class DirAttributes {

      private HashSet<BuildConfig> ignores;
      private HashSet<BuildConfig> disablePch;

      public DirAttributes() {
         ignores = new HashSet<BuildConfig>();
         disablePch = new HashSet<BuildConfig>();
      }

      public DirAttributes(HashSet<BuildConfig> excludes2, HashSet<BuildConfig> disablePch2) {
         ignores = excludes2;
         disablePch = disablePch2;
      }

      @SuppressWarnings("unchecked")
      public DirAttributes clone() {
         return new DirAttributes((HashSet<BuildConfig>)this.ignores.clone(), (HashSet<BuildConfig>)this.disablePch.clone());
      }

      public void setIgnore(BuildConfig conf) {
         ignores.add(conf);
      }

      public boolean hasIgnore(BuildConfig cfg) {
         return ignores.contains(cfg);
      }

      public void removeFromIgnored(BuildConfig cfg) {
         ignores.remove(cfg);
      }

      public void setDisablePch(BuildConfig conf) {
         disablePch.add(conf);
      }

      public boolean hasDisablePch(BuildConfig cfg) {
         return disablePch.contains(cfg);
      }

      public void removeFromDisablePch(BuildConfig cfg) {
         disablePch.remove(cfg);
      }

   }
}
