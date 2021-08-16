/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sampleapi.generator;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Set;
import javax.lang.model.element.Modifier;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import java.util.HashMap;
import java.util.Map;

class DocCommentGenerator {

    public enum Text {
        BROWNFOX(BROWNFOX_TEXT),
        NOWISTHETIME(NOWISTHETIME_TEXT),
        THISPANGRAM(THISPANGRAM_TEXT),
        LOREMIPSUM(LOREMIPSUM_TEXT),
        LIEUROPANLINGUES(LIEUROPANLINGUES_TEXT),
        CODE(CODE_TEXT);

        String commentText;

        Text(String text) {
            commentText = text;
        }

        @Override
        public String toString() {
            return commentText;
        }
    }

    public enum Tag {
        AUTHOR("@author", "Cody J. Writer"),
        PARAM("@param", ""),
        RETURN("@return", Text.NOWISTHETIME.toString()),
        SINCE("@since", "1.0"),
        THROWS("@throws", "If problem detected"),
        EXCEPTION("@exception", "If problem detected"),
        SERIAL("@serial", ""),
        SERIALDATA("@serialData", "The types and order of data could be here."),
        SERIALFIELD("@serialField", "\n        Serial field in special array"),
        FX_PROPDESC("@propertyDescription", ""),
        FX_DEFVALUE("@defaultValue", ""),
        FX_TREATASPRIVATE("@treatAsPrivate", "");

        String tagName;
        String tagValue;

        Tag(String tagName, String tagValue) {
            this.tagName = tagName;
            this.tagValue = tagValue;
        }

        public String toString() {
            return value("", "");
        }

        public String value(String value) {
            return value(value, "");
        }

        public String value(String value, String extra) {
            return tagName
                   + ((value.length() != 0) ? " " + value : "")
                   + ((tagValue.length() != 0) ? " " + tagValue : "")
                   + ((extra.length() != 0) ? " " + extra : "");
        }
    }

    public enum InlineTag {
        LITERAL("@literal", "Use < and > brackets instead of &lt; and &gt; escapes."),
        CODE("@code", "(i) -> new Abc<Object>((i > 0) ? (i << 1) : 0)"),
        LINK("@link", ""),
        VALUE("@value", ""),
        INDEX("@index", "", true);

        String tagName;
        String tagValue;
        boolean counted;
        Map<String, Integer> counters;

        InlineTag(String tagName, String tagValue) {
            this(tagName, tagValue, false);
        }

        InlineTag(String tagName, String tagValue, boolean counted) {
            this.tagName = tagName;
            this.tagValue = tagValue;
            this.counted = counted;
            if (counted) {
                counters = new HashMap<>();
            }
        }

        public String toString() {
            return value("");
        }

        public String value(String value) {
            String name = ((tagValue.length() != 0) ? " " + tagValue : "")
                   + ((value.length() != 0) ? " " + value : "");
            if (counted && !counters.containsKey(name)) {
                counters.put(name, 0);
            }
            return "{" + tagName
                   + name
                   + (counted ? "_" + counters.put(name, counters.get(name) + 1) : "")
                   + "}";
        }
    }

    public static class LinkTag {

        static String[] links = new String[] {
            "Boolean",
            "Boolean#TRUE",
            "Boolean#Boolean(boolean)",
            "Boolean#Boolean(String s)",
            "Boolean#compare(boolean, boolean)",
            "Boolean#compareTo(Boolean b)",
            "java.util.Vector",
            "java.util.Vector#elementCount",
            "java.util.Vector#Vector(int)",
            "java.util.Vector#Vector(int initialCapacity, int capacityIncrement)",
            "java.util.Vector#indexOf(Object)",
            "java.util.Vector#indexOf(Object o, int index)",
            "java.lang.annotation" };

        static int index = 0;

        public static String nextSee() {
            String next = "@see " + links[index];
            index = (index + 1) % links.length;
            return next;
        }

        public static String nextLink() {
            String next = "Also check "
                          + (((index % 2) == 0) ? "{@link " : "{@linkplain ")
                          + links[index]
                          + "} for details.\n";
            index = (index + 1) % links.length;
            return next;
        }
    }

    public static class VersionTag {

        static String[] versions = new String[] {
            "1.5, 09/01/04",
            "1.6, 12/11/06",
            "1.7, 07/28/11",
            "1.8, 04/19/14",
            "9,   06/03/16" };

        static int index = 0;

        public static String nextVersion() {
            String next = "@version " + versions[index];
            index = (index + 1) % versions.length;
            return next;
        }
    }

    //
    // getters (build comments for entities)
    //

    public String getPackageComment() {
        return InlineTag.INDEX.value("PackageCommentLabel") + " "
               + Text.LOREMIPSUM
               + "\n<p>" + Text.LIEUROPANLINGUES
               + "\n" + Text.CODE
               + "\n" + LinkTag.nextLink()
               + "\n" + LinkTag.nextSee()
               + "\n" + Tag.SINCE + "\n";
    }

    String[] serialVals = new String[] { "include", "exclude" };
    // static index to roll over "include/exclude"
    static int serialValIdx = 0;

    public String getBaseComment(JCClassDecl baseDecl, boolean toplevel) {
        String buildComment = InlineTag.INDEX.value("BaseCommentLabel") + " ";

        buildComment += Text.LIEUROPANLINGUES + "\n";

        buildComment += "<p>It is possible to see inlined code:\n"
                        + InlineTag.CODE
                        + " is the example.\n\n";

        buildComment += "<p>Literal use example.\n"
                        + InlineTag.LITERAL + "\n\n";

        buildComment += "<p>" + Text.THISPANGRAM + "\n"; // make comment longer

        buildComment += "<p>" + LinkTag.nextLink() + "\n";

        if (toplevel)
            buildComment += "\n" + VersionTag.nextVersion() + "\n";

        // @param for type params
        List<JCTypeParameter> params = baseDecl.getTypeParameters();
        int paramIndex = 0;
        for (JCTypeParameter paramDecl : params) {
            buildComment += Tag.PARAM.value(
                                "<" + paramDecl.getName().toString() + ">",
                                "the type of value set #" + paramIndex++)
                            + "\n";
        }

        buildComment += "\n" + LinkTag.nextSee();

        buildComment += "\n";

        if (toplevel)
            buildComment += Tag.AUTHOR + "\n";

        buildComment += Tag.SINCE + "\n";

        // for serial; currently no need to dig too deep
        if (isSerializable(baseDecl) || isErrorOrException(baseDecl)) {
            buildComment += "\n" + Tag.SERIAL.value(serialVals[serialValIdx]);
            serialValIdx = (serialValIdx + 1) % 2;
        }

        return buildComment;
    }

    public String getConstComment() {
        String buildComment = InlineTag.INDEX.value("ConstCommentLabel") + " ";

        buildComment += Text.NOWISTHETIME + " " + Text.BROWNFOX + "\n";
        buildComment += LinkTag.nextLink() + "\n";
        buildComment += LinkTag.nextSee() + "\n";
        buildComment += Tag.SINCE + "\n";

        return buildComment;
    }

    public String getFieldComment(JCClassDecl baseDecl,
                                  JCVariableDecl varDecl,
                                  boolean isFxStyle) {
        String buildComment = InlineTag.INDEX.value("FieldCommentLabel") + " ";

        buildComment += Text.BROWNFOX + "<p>" + Text.NOWISTHETIME + "\n";
        Set<Modifier> mods = varDecl.getModifiers().getFlags();
        String varName = varDecl.getName().toString();

        if (mods.contains(Modifier.STATIC) && mods.contains(Modifier.FINAL)) {
            JCExpression init = varDecl.getInitializer();
            if (init != null
                && !"null".equals(init.toString())
                && !"serialPersistentFields".equals(varName))
                buildComment += "<p>The value of this constant is "
                                + InlineTag.VALUE
                                + ".\n";
        }

        buildComment += "<p>" + LinkTag.nextLink() + "\n";

        if (isSerializable(baseDecl)) {
            if (isErrorOrException(baseDecl)) {
                buildComment += Tag.SERIAL + "\n";
            } else if ("serialPersistentFields".equals(varName)) {
                JCNewArray sfList = (JCNewArray)varDecl.getInitializer();
                for (JCExpression sf : sfList.getInitializers()) {
                    List<JCExpression> args = ((JCNewClass)sf).getArguments();
                    String sfName = ((JCLiteral)args.get(0)).getValue().toString();
                    String sfClass = ((JCIdent)args.get(1)).getName().toString();
                    String sfType = sfClass.substring(0, sfClass.indexOf(".class"));

                    buildComment += Tag.SERIALFIELD.value(sfName + "    " + sfType)
                                    + "\n";
                }
            } else {
                buildComment += Tag.SERIAL.value("Very important value.") + "\n";
            }
        }

        if (isFxStyle) {
            // set default value
            String varType = varDecl.getType().toString();
            buildComment += Tag.FX_DEFVALUE.value(defValue(varType)) + "\n";
        }

        buildComment += LinkTag.nextSee() + "\n";

        return buildComment;
    }

    public String getMethodComment(JCClassDecl baseDecl,
                                   JCMethodDecl methodDecl,
                                   boolean isFxStyle) {
        String buildComment = InlineTag.INDEX.value("MethodCommentLabel") + " ";

        buildComment += Text.BROWNFOX + "\n<p>" + Text.THISPANGRAM + "\n";

        buildComment += "<p>" + LinkTag.nextLink() + "\n";

        buildComment += "<p>Literal use example.\n"
                        + InlineTag.LITERAL + "\n\n";

        // @param for type params
        List<JCTypeParameter> tparams = methodDecl.getTypeParameters();
        int tparamIndex = 0;
        for (JCTypeParameter paramDecl : tparams) {
            String paramDeclString = paramDecl.getName().toString();
            // simplify it (could contain 'extend'/'super' clauses
            int spacePos = paramDeclString.indexOf(' ');
            if (spacePos != -1)
                paramDeclString = paramDeclString.substring(0, spacePos);
            buildComment += Tag.PARAM.value(
                                "<" + paramDeclString + ">",
                                "the type of value set #" + tparamIndex++)
                            + "\n";
        }

        // @param
        List<JCVariableDecl> params =  methodDecl.getParameters();
        int paramIndex = 0;
        for (JCVariableDecl paramDecl : params) {
            buildComment += Tag.PARAM.value(
                                paramDecl.getName().toString(),
                                "an income parameter #" + paramIndex++)
                            + "\n";
        }

        // @return
        JCTree retType = methodDecl.getReturnType(); // null for constructors
        if (retType != null && !"void".equals(retType.toString()))
            buildComment += Tag.RETURN + "\n";

        // @throws/@exception
        Tag t = isDerived(baseDecl) ? Tag.EXCEPTION : Tag.THROWS;
        List<JCExpression> throwTypes = methodDecl.getThrows();
        for (JCExpression throwsExp : throwTypes) {
            buildComment += t.value(throwsExp.toString()) + "\n";
        }

        if (isSerializable(baseDecl)) {
            switch (methodDecl.getName().toString()) {
                case "writeObject":
                case "readObject":
                case "writeExternal":
                case "readExternal":
                case "writeReplace":
                case "readResolve":
                    buildComment += Tag.SERIALDATA + "\n";
                    break;
                default:
            }
        }

        if (isFxStyle) {
            // @propertySetter/Getter + Description
            if (!"void".equals(retType.toString())) {
                buildComment += Tag.FX_DEFVALUE.value(defValue(retType.toString()))
                                + "\n";
            }
            buildComment += Tag.FX_PROPDESC.value(Text.BROWNFOX.toString()) + "\n";

            // @treatAsPrivate
            if (methodDecl.getModifiers().getFlags().contains(Modifier.PUBLIC))
                buildComment += Tag.FX_TREATASPRIVATE + "\n";
        }

        // @see
        buildComment += LinkTag.nextSee() + "\n";

        // @since
        buildComment += Tag.SINCE + "\n";

        return buildComment;
    }

    //
    // util methods
    //

    private boolean isErrorOrException(JCClassDecl baseDecl) {
        JCExpression ext = baseDecl.getExtendsClause();
        if (ext != null) {
            String extClassName = ext.toString();
            if (extClassName.contains("Error") || extClassName.contains("Exception"))
                return true;
        }
        return false;
    }

    private boolean isSerializable(JCClassDecl baseDecl) {
        List<JCExpression> impls = baseDecl.getImplementsClause();
        for (JCExpression impl : impls) {
            if (impl.toString().contains("Serializable"))
                return true;
        }
        return false;
    }

    private boolean isDerived(JCClassDecl baseDecl) {
        return (baseDecl.getExtendsClause() == null) ? false : true;
    }

    private String defValue(String type) {
        switch (type) {
            case "boolean":
                return "true";
            case "byte": case "char": case "int": case "long":
            case "Integer": case "Long":
                return "1";
            case "float": case "double": case "Float": case "Double":
                return "1.0";
            case "String":
                return "string";
            default:
                return "null";
        }
    }

    private static final String BROWNFOX_TEXT =
        "The quick brown fox jumps over the lazy dog.\n";
    private static final String NOWISTHETIME_TEXT =
        "Now is the time for all good men to come to the aid of the party.\n";
    private static final String THISPANGRAM_TEXT =
        "This pangram contains four a's, one b, two c's, one d, thirty e's,\n" +
        "six f's, five g's, seven h's, eleven i's, one j, one k, two l's,\n" +
        "two m's, eighteen n's, fifteen o's, two p's, one q, five r's,\n" +
        "twenty-seven s's, eighteen t's, two u's, seven v's, eight w's,\n" +
        "two x's, three y's, &amp; one z.";
    private static final String LOREMIPSUM_TEXT =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do\n" +
        "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut\n" +
        "enim ad minim veniam, quis nostrud exercitation ullamco laboris\n" +
        "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor\n" +
        "in reprehenderit in voluptate velit esse cillum dolore eu fugiat\n" +
        "nulla pariatur. Excepteur sint occaecat cupidatat non proident,\n" +
        "sunt in culpa qui officia deserunt mollit anim id est laborum.\n";
    private static final String LIEUROPANLINGUES_TEXT =
        "Li Europan lingues es membres del sam familie. Lor separat existentie\n" +
        "es un myth. Por scientie, musica, sport etc, litot Europa usa li sam\n" +
        "vocabular. Li lingues differe solmen in li grammatica, li pronunciation\n" +
        "e li plu commun vocabules. Omnicos directe al desirabilite de un nov\n" +
        "lingua franca: On refusa continuar payar custosi traductores.\n" +
        "\n" +
        "<p>At solmen va esser necessi far uniform grammatica, pronunciation\n" +
        "e plu commun paroles. Ma quande lingues coalesce, li grammatica del\n" +
        "resultant lingue es plu simplic e regulari quam ti del coalescent\n" +
        "lingues. Li nov lingua franca va esser plu simplic e regulari quam\n" +
        "li existent Europan lingues. It va esser tam simplic quam Occidental\n" +
        "in fact, it va esser Occidental. A un Angleso it va semblar un simplificat\n" +
        "Angles, quam un skeptic Cambridge amico dit me que Occidental es.\n";
    private static final String CODE_TEXT =
        "<pre>\n" +
        "public void checkLanguage(Language lang) throws Exception {\n" +
        "    if (lang.getName().equals(\"Java\")) {\n" +
        "        System.out.println(\"Warning! Hot!\");\n" +
        "    else {\n" +
        "        throw new LooserException();\n" +
        "    }\n" +
        "}\n" +
        "</pre>\n";
}
