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

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;
import java.util.Vector;

public class FileTreeCreatorVC10 extends FileTreeCreator {

      public FileTreeCreatorVC10(Path startDir, Vector<BuildConfig> allConfigs, WinGammaPlatformVC10 wg) {
         super(startDir, allConfigs, wg);
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
         DirAttributes currentFileAttr = attributes.peek().clone();
         boolean usePch = false;
         boolean disablePch = false;
         boolean useIgnore = false;
         boolean isAltSrc = false;  // only needed as a debugging crumb
         boolean isReplacedByAltSrc = false;
         String fileName = file.getFileName().toString();

         // TODO hideFile

         // usePch applies to all configs for a file.
         if (fileName.equals(BuildConfig.getFieldString(null, "UseToGeneratePch"))) {
            usePch = true;
         }

         String fileLoc = vcProjLocation.relativize(file).toString();

         // isAltSrc and isReplacedByAltSrc applies to all configs for a file
         if (BuildConfig.matchesRelativeAltSrcInclude(
               file.toAbsolutePath().toString())) {
            // current file is an alternate source file so track it
            isAltSrc = true;
            BuildConfig.trackRelativeAltSrcFile(
                file.toAbsolutePath().toString());
         } else if (BuildConfig.matchesRelativeAltSrcFile(
                    file.toAbsolutePath().toString())) {
            // current file is a regular file that matches an alternate
            // source file so yack about replacing the regular file
            isReplacedByAltSrc = true;
            System.out.println("INFO: alternate source file '" +
                               BuildConfig.getMatchingRelativeAltSrcFile(
                                   file.toAbsolutePath().toString()) +
                               "' replaces '" + fileLoc + "'");
         }

         for (BuildConfig cfg : allConfigs) {
            if (cfg.lookupHashFieldInContext("IgnoreFile", fileName) != null) {
               useIgnore = true;
               currentFileAttr.setIgnore(cfg);
            } else if (cfg.matchesIgnoredPath(file.toAbsolutePath().toString())) {
               useIgnore = true;
               currentFileAttr.setIgnore(cfg);
            }

            if (cfg.lookupHashFieldInContext("DisablePch", fileName) != null) {
               disablePch = true;
               currentFileAttr.setDisablePch(cfg);
            }

            Vector<String> rv = new Vector<String>();
            cfg.collectRelevantVectors(rv, "AdditionalFile");
            for(String addFile : rv) {
               if (addFile.equals(fileName)) {
                  // supress any ignore
                  // TODO - may need some adjustments
                  if (file.toAbsolutePath().toString().contains(cfg.get("Flavour"))) {
                     currentFileAttr.removeFromIgnored(cfg);
                  }
               }
            }
         }

         String tagName = wg10.getFileTagFromSuffix(fileName);

         if (!useIgnore && !disablePch && !usePch && !isReplacedByAltSrc) {
            wg.tag(tagName, new String[] { "Include", fileLoc});
         } else {
            wg.startTag(
                  tagName,
                  new String[] { "Include", fileLoc});

            for (BuildConfig cfg : allConfigs) {
               boolean ignore = currentFileAttr.hasIgnore(cfg);
               if (ignore) {
                  wg.tagData("ExcludedFromBuild", "true", "Condition", "'$(Configuration)|$(Platform)'=='" + cfg.get("Name") + "'");
               }
               if (usePch) {
                  wg.tagData("PrecompiledHeader", "Create", "Condition", "'$(Configuration)|$(Platform)'=='" + cfg.get("Name") + "'");
               }
               if (disablePch) {
                  wg.tag("PrecompiledHeader", "Condition", "'$(Configuration)|$(Platform)'=='" + cfg.get("Name") + "'");
               }
               if (isReplacedByAltSrc) {
                  wg.tagData("ExcludedFromBuild", "true", "Condition",
                             "'$(Configuration)|$(Platform)'=='" +
                             cfg.get("Name") + "'");
               }
            }
            wg.endTag();
         }

         String filter = startDir.relativize(file.getParent().toAbsolutePath()).toString();
         wg10.addFilterDependency(fileLoc, filter);

         return CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs)
            throws IOException {
         Boolean hide = false;
         // TODO remove attrs, if path is matched in this dir, then it is too in every subdir.
         // And we will check anyway
         DirAttributes newAttr = attributes.peek().clone();

         // check per config ignorePaths!
         for (BuildConfig cfg : allConfigs) {
            if (cfg.matchesIgnoredPath(path.toAbsolutePath().toString())) {
               newAttr.setIgnore(cfg);
            }

            // Hide is always on all configs. And additional files are never hiddden
            if (cfg.matchesHidePath(path.toAbsolutePath().toString())) {
               hide = true;
               break;
            }
         }

         if (!hide) {
            String name = startDir.relativize(path.toAbsolutePath()).toString();
            if (!"".equals(name)) {
               wg10.addFilter(name);
            }

            attributes.push(newAttr);
            return super.preVisitDirectory(path, attrs);
         } else {
            return FileVisitResult.SKIP_SUBTREE;
         }
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
         //end matching attributes set by ignorepath
         attributes.pop();
         return CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
         return CONTINUE;
      }

      public void writeFileTree() throws IOException {
         Files.walkFileTree(this.startDir, this);
      }
}
