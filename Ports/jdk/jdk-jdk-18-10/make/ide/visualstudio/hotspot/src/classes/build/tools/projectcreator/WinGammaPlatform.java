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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

abstract class HsArgHandler extends ArgHandler {
    static final int STRING = 1;
    static final int VECTOR = 2;
    static final int HASH   = 3;

    boolean nextNotKey(ArgIterator it) {
        if (it.next()) {
            String s = it.get();
            return (s.length() == 0) || (s.charAt(0) != '-');
        } else {
            return false;
        }
    }

    void empty(String key, String message) {
        if (key != null) {
            System.err.println("** Error: empty " + key);
        }
        if (message != null) {
            System.err.println(message);
        }
        WinGammaPlatform.usage();
    }

    static String getCfg(String val) {
        int under = val.indexOf('_');
        int len = val.length();
        if (under != -1 && under < len - 1) {
            return val.substring(under+1, len);
        } else {
            return null;
        }
    }
}

class ArgRuleSpecific extends ArgRule {
    ArgRuleSpecific(String arg, ArgHandler handler) {
        super(arg, handler);
    }

    boolean match(String rulePattern, String arg) {
        return rulePattern.startsWith(arg);
    }
}


class SpecificHsArgHandler extends HsArgHandler {

    String message, argKey, valKey;
    int type;

    public void handle(ArgIterator it) {
        String cfg = getCfg(it.get());
        if (nextNotKey(it)) {
            String val = it.get();
            switch (type) {
            case VECTOR:
                BuildConfig.addFieldVector(cfg, valKey, val);
                break;
            case HASH:
                BuildConfig.putFieldHash(cfg, valKey, val, "1");
                break;
            case STRING:
                BuildConfig.putField(cfg, valKey, val);
                break;
            default:
                empty(valKey, "Unknown type: "+type);
            }
            it.next();

        } else {
            empty(argKey, message);
        }
    }

    SpecificHsArgHandler(String argKey, String valKey, String message, int type) {
        this.argKey = argKey;
        this.valKey = valKey;
        this.message = message;
        this.type = type;
    }
}


class HsArgRule extends ArgRuleSpecific {

    HsArgRule(String argKey, String valKey, String message, int type) {
        super(argKey, new SpecificHsArgHandler(argKey, valKey, message, type));
    }

}

public abstract class WinGammaPlatform {

    public boolean fileNameStringEquality(String s1, String s2) {
        return s1.equalsIgnoreCase(s2);
    }

    static void usage() throws IllegalArgumentException {
        System.err.println("WinGammaPlatform platform-specific options:");
        System.err.println("  -sourceBase <path to directory (workspace) " +
                           "containing source files; no trailing slash>");
        System.err.println("  -projectFileName <full pathname to which project file " +
                           "will be written; all parent directories must " +
                           "already exist>");
        System.err.println("  If any of the above are specified, "+
                           "they must all be.");
        System.err.println("  Note: if '-altRelativeInclude' option below " +
                           "is used, then the '-relativeAltSrcInclude' " +
                           "option must be used to specify the alternate " +
                           "source dir, e.g., 'src\\closed'");
        System.err.println("  Additional, optional arguments, which can be " +
                           "specified multiple times:");
        System.err.println("    -absoluteInclude <string containing absolute " +
                           "path to include directory>");
        System.err.println("    -altRelativeInclude <string containing " +
                           "alternate include directory relative to " +
                           "-sourceBase>");
        System.err.println("    -relativeInclude <string containing include " +
                           "directory relative to -sourceBase>");
        System.err.println("    -define <preprocessor flag to be #defined " +
                           "(note: doesn't yet support " +
                           "#define (flag) (value))>");
        System.err.println("    -startAt <subdir of sourceBase>");
        System.err.println("    -additionalFile <file not in database but " +
                           "which should show up in project file>");
        System.err.println("    -additionalGeneratedFile <absolute path to " +
                           "directory containing file; no trailing slash> " +
                           "<name of file generated later in the build process>");
        throw new IllegalArgumentException();
    }


    public void addPerFileLine(Hashtable table,
                               String fileName,
                               String line) {
        Vector v = (Vector) table.get(fileName);
        if (v != null) {
            v.add(line);
        } else {
            v = new Vector();
            v.add(line);
            table.put(fileName, v);
        }
    }

    protected static class PerFileCondData {
        public String releaseString;
        public String debugString;
    }

    protected void addConditionalPerFileLine(Hashtable table,
                                           String fileName,
                                           String releaseLine,
                                           String debugLine) {
        PerFileCondData data = new PerFileCondData();
        data.releaseString = releaseLine;
        data.debugString = debugLine;
        Vector v = (Vector) table.get(fileName);
        if (v != null) {
            v.add(data);
        } else {
            v = new Vector();
            v.add(data);
            table.put(fileName, v);
        }
    }

    protected static class PrelinkCommandData {
      String description;
      String commands;
    }

    protected void addPrelinkCommand(Hashtable table,
                                     String build,
                                     String description,
                                     String commands) {
      PrelinkCommandData data = new PrelinkCommandData();
      data.description = description;
      data.commands = commands;
      table.put(build, data);
    }

    public boolean findString(Vector v, String s) {
        for (Iterator iter = v.iterator(); iter.hasNext(); ) {
            if (((String) iter.next()).equals(s)) {
                return true;
            }
        }

        return false;
    }

     String getProjectName(String fullPath, String extension)
        throws IllegalArgumentException, IOException {
        File file = new File(fullPath).getCanonicalFile();
        fullPath = file.getCanonicalPath();
        String parent = file.getParent();

        if (!fullPath.endsWith(extension)) {
            throw new IllegalArgumentException("project file name \"" +
                                               fullPath +
                                               "\" does not end in "+extension);
        }

        if ((parent != null) &&
            (!fullPath.startsWith(parent))) {
            throw new RuntimeException(
                "Internal error: parent of file name \"" + parent +
                "\" does not match file name \"" + fullPath + "\""
            );
        }

        int len = parent.length();
        if (!parent.endsWith(Util.sep)) {
            len += Util.sep.length();
        }

        int end = fullPath.length() - extension.length();

        if (len == end) {
            throw new RuntimeException(
                "Internal error: file name was empty"
            );
        }

        return fullPath.substring(len, end);
    }

    protected abstract String getProjectExt();

    public void createVcproj(String[] args)
        throws IllegalArgumentException, IOException {

        parseArguments(args);

        String projectFileName = BuildConfig.getFieldString(null, "ProjectFileName");
        String ext = getProjectExt();

        String projectName = getProjectName(projectFileName, ext);

        writeProjectFile(projectFileName, projectName, createAllConfigs(BuildConfig.getFieldString(null, "PlatformName")));
    }

    protected void writePrologue(String[] args) {
        System.err.println("WinGammaPlatform platform-specific arguments:");
        for (int i = 0; i < args.length; i++) {
            System.err.print(args[i] + " ");
        }
        System.err.println();
    }


    void parseArguments(String[] args) {
        new ArgsParser(args,
                       new ArgRule[]
            {
                new ArgRule("-sourceBase",
                            new HsArgHandler() {
                                public void handle(ArgIterator it) {
                                   String cfg = getCfg(it.get());
                                   if (nextNotKey(it)) {
                                      String sb = (String) it.get();
                                      if (sb.endsWith(Util.sep)) {
                                         sb = sb.substring(0, sb.length() - 1);
                                      }
                                      BuildConfig.putField(cfg, "SourceBase", sb);
                                      it.next();
                                   } else {
                                      empty("-sourceBase", null);
                                   }
                                }
                            }
                            ),

                new HsArgRule("-buildBase",
                              "BuildBase",
                              "   (Did you set the HotSpotBuildSpace environment variable?)",
                              HsArgHandler.STRING
                              ),

               new HsArgRule("-buildSpace",
                              "BuildSpace",
                              null,
                              HsArgHandler.STRING
                              ),

               new HsArgRule("-makeBinary",
                              "MakeBinary",
                              null,
                              HsArgHandler.STRING
                              ),

               new HsArgRule("-makeOutput",
                              "MakeOutput",
                              null,
                              HsArgHandler.STRING
                              ),

              new HsArgRule("-platformName",
                              "PlatformName",
                              null,
                              HsArgHandler.STRING
                              ),

              new HsArgRule("-projectFileName",
                              "ProjectFileName",
                              null,
                              HsArgHandler.STRING
                              ),

                new HsArgRule("-jdkTargetRoot",
                              "JdkTargetRoot",
                              "   (Did you set the HotSpotJDKDist environment variable?)",
                              HsArgHandler.STRING
                              ),

                new HsArgRule("-compiler",
                              "CompilerVersion",
                              "   (Did you set the VcVersion correctly?)",
                              HsArgHandler.STRING
                              ),

                new HsArgRule("-absoluteInclude",
                              "AbsoluteInclude",
                              null,
                              HsArgHandler.VECTOR
                              ),

                new HsArgRule("-altRelativeInclude",
                              "AltRelativeInclude",
                              null,
                              HsArgHandler.VECTOR
                              ),

                new HsArgRule("-relativeInclude",
                              "RelativeInclude",
                              null,
                              HsArgHandler.VECTOR
                              ),

                new HsArgRule("-absoluteSrcInclude",
                              "AbsoluteSrcInclude",
                              null,
                              HsArgHandler.VECTOR
                              ),

                new HsArgRule("-relativeAltSrcInclude",
                              "RelativeAltSrcInclude",
                              null,
                              HsArgHandler.STRING
                              ),

                new HsArgRule("-relativeSrcInclude",
                              "RelativeSrcInclude",
                              null,
                              HsArgHandler.VECTOR
                              ),

                new HsArgRule("-define",
                              "Define",
                              null,
                              HsArgHandler.VECTOR
                              ),

                new HsArgRule("-useToGeneratePch",
                              "UseToGeneratePch",
                              null,
                              HsArgHandler.STRING
                              ),

                new ArgRuleSpecific("-perFileLine",
                            new HsArgHandler() {
                                public void handle(ArgIterator it) {
                                    String cfg = getCfg(it.get());
                                    if (nextNotKey(it)) {
                                        String fileName = it.get();
                                        if (nextNotKey(it)) {
                                            String line = it.get();
                                            BuildConfig.putFieldHash(cfg, "PerFileLine", fileName, line);
                                            it.next();
                                            return;
                                        }
                                    }
                                    empty(null, "** Error: wrong number of args to -perFileLine");
                                }
                            }
                            ),

                new ArgRuleSpecific("-conditionalPerFileLine",
                            new HsArgHandler() {
                                public void handle(ArgIterator it) {
                                    String cfg = getCfg(it.get());
                                    if (nextNotKey(it)) {
                                        String fileName = it.get();
                                        if (nextNotKey(it)) {
                                            String productLine = it.get();
                                            if (nextNotKey(it)) {
                                                String debugLine = it.get();
                                                BuildConfig.putFieldHash(cfg+"_debug", "CondPerFileLine",
                                                                         fileName, debugLine);
                                                BuildConfig.putFieldHash(cfg+"_product", "CondPerFileLine",
                                                                         fileName, productLine);
                                                it.next();
                                                return;
                                            }
                                        }
                                    }

                                    empty(null, "** Error: wrong number of args to -conditionalPerFileLine");
                                }
                            }
                            ),

                new HsArgRule("-disablePch",
                              "DisablePch",
                              null,
                              HsArgHandler.HASH
                              ),

                new ArgRule("-startAt",
                            new HsArgHandler() {
                                public void handle(ArgIterator it) {
                                    if (BuildConfig.getField(null, "StartAt") != null) {
                                        empty(null, "** Error: multiple -startAt");
                                    }
                                    if (nextNotKey(it)) {
                                        BuildConfig.putField(null, "StartAt", it.get());
                                        it.next();
                                    } else {
                                        empty("-startAt", null);
                                    }
                                }
                            }
                            ),

                new HsArgRule("-ignoreFile",
                                      "IgnoreFile",
                                      null,
                                      HsArgHandler.HASH
                                      ),

                new HsArgRule("-ignorePath",
                              "IgnorePath",
                              null,
                              HsArgHandler.VECTOR
                              ),

                new HsArgRule("-hidePath",
                      "HidePath",
                      null,
                      HsArgHandler.VECTOR
                      ),

                new HsArgRule("-additionalFile",
                              "AdditionalFile",
                              null,
                              HsArgHandler.VECTOR
                              ),

                new ArgRuleSpecific("-additionalGeneratedFile",
                            new HsArgHandler() {
                                public void handle(ArgIterator it) {
                                    String cfg = getCfg(it.get());
                                    if (nextNotKey(it)) {
                                        String dir = it.get();
                                        if (nextNotKey(it)) {
                                            String fileName = it.get();
                                            BuildConfig.putFieldHash(cfg, "AdditionalGeneratedFile",
                                                                     Util.normalize(dir + Util.sep + fileName),
                                                                     fileName);
                                            it.next();
                                            return;
                                        }
                                    }
                                    empty(null, "** Error: wrong number of args to -additionalGeneratedFile");
                                }
                            }
                            ),

                new ArgRule("-prelink",
                            new HsArgHandler() {
                                public void handle(ArgIterator it) {
                                    if (nextNotKey(it)) {
                                        if (nextNotKey(it)) {
                                            String description = it.get();
                                            if (nextNotKey(it)) {
                                                String command = it.get();
                                                BuildConfig.putField(null, "PrelinkDescription", description);
                                                BuildConfig.putField(null, "PrelinkCommand", command);
                                                it.next();
                                                return;
                                            }
                                        }
                                    }

                                    empty(null,  "** Error: wrong number of args to -prelink");
                                }
                            }
                            ),

                new ArgRule("-postbuild",
                            new HsArgHandler() {
                                public void handle(ArgIterator it) {
                                    if (nextNotKey(it)) {
                                        if (nextNotKey(it)) {
                                            String description = it.get();
                                            if (nextNotKey(it)) {
                                                String command = it.get();
                                                BuildConfig.putField(null, "PostbuildDescription", description);
                                                BuildConfig.putField(null, "PostbuildCommand", command);
                                                it.next();
                                                return;
                                            }
                                        }
                                    }

                                    empty(null,  "** Error: wrong number of args to -postbuild");
                                }
                            }
                            ),
            },
                                       new ArgHandler() {
                                           public void handle(ArgIterator it) {

                                               throw new RuntimeException("Arg Parser: unrecognized option "+it.get());
                                           }
                                       }
                                       );
        if (BuildConfig.getField(null, "SourceBase") == null      ||
            BuildConfig.getField(null, "BuildBase") == null       ||
            BuildConfig.getField(null, "ProjectFileName") == null ||
            BuildConfig.getField(null, "CompilerVersion") == null) {
            usage();
        }

        BuildConfig.putField(null, "PlatformObject", this);
    }

    Vector createAllConfigs(String platform) {
        Vector allConfigs = new Vector();

        allConfigs.add(new C1DebugConfig());
        allConfigs.add(new C1FastDebugConfig());
        allConfigs.add(new C1ProductConfig());

        allConfigs.add(new TieredDebugConfig());
        allConfigs.add(new TieredFastDebugConfig());
        allConfigs.add(new TieredProductConfig());

        return allConfigs;
    }

    PrintWriter printWriter;

    public void writeProjectFile(String projectFileName, String projectName,
                                 Vector<BuildConfig> allConfigs) throws IOException {
        throw new RuntimeException("use compiler version specific version");
    }

    int indent;
    private Stack<String> tagStack = new Stack<String>();

    private void startTagPrim(String name, String[] attrs, boolean close) {
       startTagPrim(name, attrs, close, true);
    }

    private void startTagPrim(String name, String[] attrs, boolean close,
          boolean newline) {
       doIndent();
       printWriter.print("<" + name);
       indent++;

       if (attrs != null && attrs.length > 0) {
          for (int i = 0; i < attrs.length; i += 2) {
             printWriter.print(" " + attrs[i] + "=\"" + attrs[i + 1] + "\"");
             if (i < attrs.length - 2) {
             }
          }
       }

       if (close) {
          indent--;
          printWriter.print(" />");
       } else {
          // TODO push tag name, and change endTag to pop and print.
          tagStack.push(name);
          printWriter.print(">");
       }
       if (newline) {
          printWriter.println();
       }
    }

    void startTag(String name, String... attrs) {
       startTagPrim(name, attrs, false);
    }

    void startTagV(String name, Vector attrs) {
       String s[] = new String[attrs.size()];
       for (int i = 0; i < attrs.size(); i++) {
          s[i] = (String) attrs.elementAt(i);
       }
       startTagPrim(name, s, false);
    }

    void endTag() {
       String name = tagStack.pop();
       indent--;
       doIndent();
       printWriter.println("</" + name + ">");
    }

    private void endTagNoIndent() {
       String name = tagStack.pop();
       indent--;
       printWriter.println("</" + name + ">");
    }

    void tag(String name, String... attrs) {
       startTagPrim(name, attrs, true);
    }

    void tagData(String name, String data) {
       startTagPrim(name, null, false, false);
       printWriter.print(data);
       endTagNoIndent();
    }

    void tagData(String name, String data, String... attrs) {
       startTagPrim(name, attrs, false, false);
       printWriter.print(data);
       endTagNoIndent();
    }

    void tagV(String name, Vector attrs) {
       String s[] = new String[attrs.size()];
       for (int i = 0; i < attrs.size(); i++) {
          s[i] = (String) attrs.elementAt(i);
       }
       startTagPrim(name, s, true);
    }

    void doIndent() {
       for (int i = 0; i < indent; i++) {
          printWriter.print("  ");
       }
    }


}
