/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package build.tools.dirdiff;

import java.io.File;
import java.util.TreeSet;

public class DirDiff implements Runnable {
    private final static String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final boolean traversSccsDirs;
    private static final boolean recurseExtraDirs;
    private static final boolean verboseMode;
    private static final boolean checkSizes;
    private static long SizeTolerance = 0;
    private File goldenDir = null;
    private File testDir = null;

    // static initializer:
      static {
          String traversePropertyValue = System.getProperty("sccs");
          traversSccsDirs = (traversePropertyValue != null &&
                              traversePropertyValue.toLowerCase().equals("true"))? true : false;
          if (traversSccsDirs) {
              System.err.println("traversing SCCS directories...");
          }

          String verbosePropertyValue = System.getProperty("verbose");
          verboseMode = (verbosePropertyValue != null &&
                              verbosePropertyValue.toLowerCase().equals("true"))? true : false;
          if (verboseMode) {
              System.err.println("verbose mode truned on...");
          }

          String noRecurseExtraDirsPropertyValue = System.getProperty("recurse");
          recurseExtraDirs = (noRecurseExtraDirsPropertyValue != null &&
                              noRecurseExtraDirsPropertyValue.toLowerCase().equals("true"))? true : false;
          if (recurseExtraDirs) {
              System.err.println("recursing extra directories...");
          }

          String sizeToleranceValue = System.getProperty("sizeTolerance");
          checkSizes = (sizeToleranceValue != null);
          if (checkSizes) {
              try {
                  SizeTolerance = Long.parseLong(sizeToleranceValue);
              }
              catch (NumberFormatException e) {
                  System.err.println("Invlalid sizeTolerance value: " + sizeToleranceValue);
                  System.err.println("Expecting a long value.  Exiting.");
                  System.exit(1);
              }
              System.err.println("checking matching files for size differences of at least " + SizeTolerance);
          }
      }

  public DirDiff(File inGoldenDir, File inTestDir) {
      goldenDir = inGoldenDir;
      testDir = inTestDir;
  }

  private void whatToDoWithMatchingFiles(File goldenChild, File testChild) {
      if (verboseMode) {
          System.out.println("Files Match:\t" + goldenChild.getAbsolutePath() +
                             " and " + testChild.getAbsolutePath());
      }
      if (checkSizes) {
          // compare file sizes...
          long goldenLength = 0;
          long testLength = 0;
          try {
              goldenLength = goldenChild.length();
              testLength = testChild.length();
          }
          catch (Exception e) {
              System.err.println("Error: exception thrown and caught:");
              e.printStackTrace();
          }
          if (java.lang.Math.abs(goldenLength - testLength) > SizeTolerance) {
              if (goldenLength > testLength) {
                  System.out.println("File short [" + (testLength - goldenLength) + "]:\t" + testChild.getAbsolutePath());
              } else {
                  System.out.println("File long [" + (testLength - goldenLength) + "]:\t" + testChild.getAbsolutePath());
              }
          }
      }
  }


  private void whatToDoWithMatchingDirs(File goldenChild, File testChild) {
      if (verboseMode) {
          System.out.println("Dirs Match:\t" + goldenChild.getAbsolutePath() +
                             " and " + testChild.getAbsolutePath());
      }
  }

  private void whatToDoWithMissingFiles(File missingFile) {
      long length = 0;
      try {
          length = missingFile.length();
      }
      catch (Exception e) {
          System.err.println("Error: exception thrown and caught:");
          e.printStackTrace();
      }

      System.out.println("Missing File [" + length + "]:\t" + missingFile.getAbsolutePath());
  }

  private void whatToDoWithExtraFiles(File extraFile) {
      long length = 0;
      try {
          length = extraFile.length();
      }
      catch (Exception e) {
          System.err.println("Error: exception thrown and caught:");
          e.printStackTrace();
      }

      System.out.println("Extra File [" + length + "]:\t" + extraFile.getAbsolutePath());
  }

  private void whatToDoWithMissingDirs(File missingDir) {
      System.out.println("Missing Dir:\t" + missingDir.getAbsolutePath());
  }

  private void whatToDoWithExtraDirs(File extraDir) {
      System.out.println("Extra Dir:\t" + extraDir.getAbsolutePath());
  }

  private void whatToDoWithNonMatchingChildren(File goldenChild, File testChild) {
      System.out.println("Type Mismatch:\t" + goldenChild.getAbsolutePath() + " is a " +
                         (goldenChild.isDirectory()? "directory" : "file") +
                         " and " + testChild.getAbsolutePath() + " is a " +
                         (testChild.isDirectory()? "directory" : "file"));
  }

  public void run() {
      File[] currentTestDirs = null;
      if (testDir != null) {
          currentTestDirs = testDir.listFiles();
      }

      File[] currentGoldenDirs = null;
      TreeSet<String> goldDirSet = new TreeSet<>();
      if (goldenDir != null) {
          currentGoldenDirs = goldenDir.listFiles();
          for (int i=0; i<currentGoldenDirs.length; i++) {
              goldDirSet.add(currentGoldenDirs[i].getName());
          }
      }

      // now go through the list of members
      if (currentGoldenDirs != null) {
          for (int i=0; i<currentGoldenDirs.length; i++) {
              File newGoldenDir = currentGoldenDirs[i];

              // do not traverse SCCS directories...
              if (!(newGoldenDir.getAbsolutePath().endsWith("SCCS")) || traversSccsDirs ) {
                  // start a compare of this child and the like-named test child...
                  File newTestDir = new File(testDir.getAbsolutePath() + FILE_SEPARATOR +
                                             newGoldenDir.getName());

                  if (newTestDir.exists()) {
                      if (newGoldenDir.isDirectory()) {
                          if (newTestDir.isDirectory()) {
                              whatToDoWithMatchingDirs(newGoldenDir, newTestDir);
                              Thread t = new Thread( new DirDiff(newGoldenDir, newTestDir));
                              t.start();
                          } else {
                              whatToDoWithNonMatchingChildren(newGoldenDir, newTestDir);
                          }
                      } else { // of... newGoldenDir.isDirectory()...
                          if (newTestDir.isDirectory()) {
                              whatToDoWithNonMatchingChildren(newGoldenDir, newTestDir);
                          }
                           whatToDoWithMatchingFiles(newGoldenDir, newTestDir);
                      }
                  } else { // of... newTestDir.exists()...
                      if (newGoldenDir.isDirectory()) {
                          whatToDoWithMissingDirs(newTestDir);
                          Thread t = new Thread( new DirDiff(newGoldenDir, newTestDir));
                          t.start();
                      } else {
                          whatToDoWithMissingFiles(newTestDir);
                      }
                  }
              }
          }
      }

      // look for extra test objs...
      if (currentTestDirs != null) {
          for (int i=0; i<currentTestDirs.length; i++) {
              // do not traverse SCCS directories...
              if (!(currentTestDirs[i].getAbsolutePath().endsWith("SCCS")) || traversSccsDirs ) {
                  if (!goldDirSet.contains(currentTestDirs[i].getName())) {
                      if (currentTestDirs[i].isDirectory()) {
                          whatToDoWithExtraDirs(currentTestDirs[i]);
                          if (recurseExtraDirs) {
                              Thread t = new Thread( new DirDiff( null, currentTestDirs[i]));
                              t.start();
                          }
                      } else {
                          whatToDoWithExtraFiles(currentTestDirs[i]);
                      }
                  }
              }
          }
      }
  }

  public static void main(String[] args) {
      if (args.length != 2) {
          System.err.println("You must provide two directory names on the command line.");
          System.err.println("Usage:\tDirDiff dir1 dir2");
          System.err.println("\tJava Runtime Properties (set to \"true\"):");
          System.err.println("\t\tsccs\trecurse SCCS directories");
          System.err.println("\t\tverbose\tprint verbose diagnostics");
          System.err.println("\t\trecurse\trecursing extra directories showing extra files");
          System.err.println("\t\tsizeTolerance\tset tolerance for size differences - default is infinite");

          System.exit(0);
      } else {
          File golden = new File(args[0]);
          File test = new File(args[1]);

          if (!golden.exists()) {
              System.err.println("Error: path " + golden.getAbsolutePath() +
                                 " does not exist. Skipping.");
              System.exit(0);
          }
          if (!golden.isDirectory()) {
              System.err.println("Error: path " + golden.getAbsolutePath() +
                                 " must be a directory. Skipping.");
              System.exit(0);
          }
          if (!test.exists()) {
              System.err.println("Error: path " + test.getAbsolutePath() +
                                 " does not exist. Skipping.");
              System.exit(0);
          }
          if (!test.isDirectory()) {
              System.err.println("Error: path " + test.getAbsolutePath() +
                                 " must be a directory. Skipping.");
              System.exit(0);
          }

          Thread t = new Thread( new DirDiff(golden, test));
          t.start();
      }
  }
}
