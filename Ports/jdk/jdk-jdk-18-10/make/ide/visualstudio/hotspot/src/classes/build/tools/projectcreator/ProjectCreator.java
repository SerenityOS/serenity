/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

public class ProjectCreator {

   public static void usage() {
      System.out.println("ProjectCreator options:");
      System.err.println("WinGammaPlatform platform-specific options:");
      System.err.println("  -sourceBase <path to directory (workspace) "
            + "containing source files; no trailing slash>");
      System.err.println("  -dspFileName <full pathname to which .dsp file "
            + "will be written; all parent directories must "
            + "already exist>");
      System.err.println("  -envVar <environment variable to be inserted "
            + "into .dsp file, substituting for path given in "
            + "-sourceBase. Example: HotSpotWorkSpace>");
      System.err.println("  -dllLoc <path to directory in which to put "
            + "jvm.dll; no trailing slash>");
      System.err.println("  If any of the above are specified, "
            + "they must all be.");
      System.err.println("  Note: if '-altRelativeInclude' option below is "
            + "used, then the '-relativeAltSrcInclude' option must be used "
            + "to specify the alternate source dir, e.g., 'src\\closed'");
      System.err.println("  Additional, optional arguments, which can be "
            + "specified multiple times:");
      System.err.println("    -absoluteInclude <string containing absolute "
            + "path to include directory>");
      System.err.println("    -altRelativeInclude <string containing "
            + "alternate include directory relative to -envVar>");
      System.err.println("    -relativeInclude <string containing include "
            + "directory relative to -envVar>");
      System.err.println("    -define <preprocessor flag to be #defined "
            + "(note: doesn't yet support " + "#define (flag) (value))>");
      System.err.println("    -perFileLine <file> <line>");
      System.err.println("    -conditionalPerFileLine <file> <line for "
            + "release build> <line for debug build>");
      System.err.println("  (NOTE: To work around a bug in nmake, where "
            + "you can't have a '#' character in a quoted "
            + "string, all of the lines outputted have \"#\"" + "prepended)");
      System.err.println("    -startAt <subdir of sourceBase>");
      System.err.println("    -ignoreFile <file which won't be able to be "
            + "found in the sourceBase because it's generated " + "later>");
      System.err.println("    -additionalFile <file not in database but "
            + "which should show up in .dsp file>");
      System.err
            .println("    -additionalGeneratedFile <environment variable of "
                  + "generated file's location> <relative path to "
                  + "directory containing file; no trailing slash> "
                  + "<name of file generated later in the build process>");
      System.err.println("    -prelink <build> <desc> <cmds>:");
      System.err
            .println(" Generate a set of prelink commands for the given BUILD");
      System.err
            .println(" (\"Debug\" or \"Release\"). The prelink description and commands");
      System.err.println(" are both quoted strings.");
      System.err.println("    Default includes: \".\"");
      System.err
            .println("    Default defines: WIN32, _WINDOWS, \"HOTSPOT_BUILD_USER=$(USERNAME)\"");
   }

   public static void main(String[] args) {
      try {
         if (args.length < 3) {
            usage();
            System.exit(1);
         }

         String platformName = args[0];
         Class platformClass = Class.forName(platformName);
         WinGammaPlatform platform = (WinGammaPlatform) platformClass
               .newInstance();

         String[] platformArgs = new String[args.length - 1];
         System.arraycopy(args, 1, platformArgs, 0, platformArgs.length);

         // Allow the platform to write platform-specific files
         platform.createVcproj(platformArgs);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
}
