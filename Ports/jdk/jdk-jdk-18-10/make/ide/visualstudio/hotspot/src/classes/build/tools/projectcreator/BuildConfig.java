/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

class BuildConfig {
    @SuppressWarnings("rawtypes")
    Hashtable vars;
    Vector<String> basicNames, basicPaths;
    String[] context;

    static CompilerInterface ci;
    static CompilerInterface getCI() {
        if (ci == null) {
            String comp = (String)getField(null, "CompilerVersion");
            try {
                ci = (CompilerInterface)Class.forName("build.tools.projectcreator.CompilerInterface" + comp).newInstance();
            } catch (Exception cnfe) {
                System.err.println("Cannot find support for compiler " + comp);
                throw new RuntimeException(cnfe.toString());
            }
        }
        return ci;
    }

    @SuppressWarnings("rawtypes")
    protected void initNames(String flavour, String build, String outDll) {
        if (vars == null) vars = new Hashtable();

        String flavourBuild =  flavour + "_" + build;
        String platformName = getFieldString(null, "PlatformName");
        System.out.println();
        System.out.println(flavourBuild);

        put("Name", getCI().makeCfgName(flavourBuild, platformName));
        put("Flavour", flavour);
        put("Build", build);
        put("PlatformName", platformName);

        // ones mentioned above were needed to expand format
        String buildBase = expandFormat(getFieldString(null, "BuildBase"));
        String sourceBase = getFieldString(null, "SourceBase");
        String buildSpace = getFieldString(null, "BuildSpace");
        String outDir = buildBase;
        String jdkTargetRoot = getFieldString(null, "JdkTargetRoot");
        String makeBinary = getFieldString(null, "MakeBinary");
        String makeOutput = expandFormat(getFieldString(null, "MakeOutput"));

        put("Id", flavourBuild);
        put("OutputDir", outDir);
        put("SourceBase", sourceBase);
        put("BuildBase", buildBase);
        put("BuildSpace", buildSpace);
        put("OutputDll", outDir + Util.sep + outDll);
        put("JdkTargetRoot", jdkTargetRoot);
        put("MakeBinary", makeBinary);
        put("MakeOutput", makeOutput);

        context = new String [] {flavourBuild, flavour, build, null};
    }

    protected void init(Vector<String> includes, Vector<String> defines) {
        initDefaultDefines(defines);
        initDefaultCompilerFlags(includes);
        initDefaultLinkerFlags();
        //handleDB();
    }


    protected void initDefaultCompilerFlags(Vector<String> includes) {
        Vector compilerFlags = new Vector();

        compilerFlags.addAll(getCI().getBaseCompilerFlags(getV("Define"),
                                                          includes,
                                                          get("OutputDir")));

        put("CompilerFlags", compilerFlags);
    }

    protected void initDefaultLinkerFlags() {
        Vector linkerFlags = new Vector();

        linkerFlags.addAll(getCI().getBaseLinkerFlags( get("OutputDir"), get("OutputDll"), get("PlatformName")));

        put("LinkerFlags", linkerFlags);
    }

    public boolean matchesIgnoredPath(String path) {
        Vector<String> rv = new Vector<String>();
        collectRelevantVectors(rv, "IgnorePath");
        for (String pathPart : rv) {
            if (path.contains(pathPart))  {
                return true;
            }
        }
        return false;
    }

    public boolean matchesHidePath(String path) {
        Vector<String> rv = new Vector<String>();
        collectRelevantVectors(rv, "HidePath");
        for (String pathPart : rv) {
            if (path.contains(Util.normalize(pathPart)))  {
                return true;
            }
        }
        return false;
    }

   public Vector<String> matchesAdditionalGeneratedPath(String fullPath) {
        Vector<String> rv = new Vector<String>();
        Hashtable<String, String> v = (Hashtable<String, String>)BuildConfig.getField(this.toString(), "AdditionalGeneratedFile");
        if (v != null) {
            for (Enumeration<String> e=v.keys(); e.hasMoreElements(); ) {
                String key = e.nextElement();
                String val = v.get(key);

                if (fullPath.endsWith(expandFormat(key))) {
                    rv.add(expandFormat(val));
                }
            }
        }
        return rv;
    }

    // Returns true if the specified path refers to a relative alternate
    // source file. RelativeAltSrcInclude is usually "src\closed".
    public static boolean matchesRelativeAltSrcInclude(String path) {
        String relativeAltSrcInclude =
            getFieldString(null, "RelativeAltSrcInclude");
        Vector<String> v = getFieldVector(null, "AltRelativeInclude");
        if (v != null) {
            for (String pathPart : v) {
                if (path.contains(relativeAltSrcInclude + Util.sep + pathPart))  {
                    return true;
                }
            }
        }
        return false;
    }

    // Returns the relative alternate source file for the specified path.
    // Null is returned if the specified path does not have a matching
    // alternate source file.
    public static String getMatchingRelativeAltSrcFile(String path) {
        Vector<String> v = getFieldVector(null, "RelativeAltSrcFileList");
        if (v == null) {
            return null;
        }
        for (String pathPart : v) {
            if (path.endsWith(pathPart)) {
                String relativeAltSrcInclude =
                    getFieldString(null, "RelativeAltSrcInclude");
                return relativeAltSrcInclude + Util.sep + pathPart;
            }
        }
        return null;
    }

    // Returns true if the specified path has a matching alternate
    // source file.
    public static boolean matchesRelativeAltSrcFile(String path) {
        return getMatchingRelativeAltSrcFile(path) != null;
    }

    // Track the specified alternate source file. The source file is
    // tracked without the leading .*<sep><RelativeAltSrcFileList><sep>
    // part to make matching regular source files easier.
    public static void trackRelativeAltSrcFile(String path) {
        String pattern = getFieldString(null, "RelativeAltSrcInclude") +
            Util.sep;
        int altSrcInd = path.indexOf(pattern);
        if (altSrcInd == -1) {
            // not an AltSrc path
            return;
        }

        altSrcInd += pattern.length();
        if (altSrcInd >= path.length()) {
            // not a valid AltSrc path
            return;
        }

        String altSrcFile = path.substring(altSrcInd);
        Vector v = getFieldVector(null, "RelativeAltSrcFileList");
        if (v == null || !v.contains(altSrcFile)) {
            addFieldVector(null, "RelativeAltSrcFileList", altSrcFile);
        }
    }

    void addTo(Hashtable ht, String key, String value) {
        ht.put(expandFormat(key), expandFormat(value));
    }

    void initDefaultDefines(Vector defines) {
        Vector sysDefines = new Vector();
        sysDefines.add("WIN32");
        sysDefines.add("_WINDOWS");
        sysDefines.add("HOTSPOT_BUILD_USER=\\\""+System.getProperty("user.name")+"\\\"");
        sysDefines.add("HOTSPOT_BUILD_TARGET=\\\""+get("Build")+"\\\"");
        sysDefines.add("INCLUDE_JFR=1");
        sysDefines.add("_JNI_IMPLEMENTATION_");
        if (vars.get("PlatformName").equals("Win32")) {
            sysDefines.add("HOTSPOT_LIB_ARCH=\\\"i386\\\"");
        } else {
            sysDefines.add("HOTSPOT_LIB_ARCH=\\\"amd64\\\"");
        }
        sysDefines.add("DEBUG_LEVEL=\\\"" + get("Build")+"\\\"");
        sysDefines.addAll(defines);

        put("Define", sysDefines);
    }

    String get(String key) {
        return (String)vars.get(key);
    }

    Vector getV(String key) {
        return (Vector)vars.get(key);
    }

    Object getO(String key) {
        return vars.get(key);
    }

    Hashtable getH(String key) {
        return (Hashtable)vars.get(key);
    }

    Object getFieldInContext(String field) {
        for (int i=0; i<context.length; i++) {
            Object rv = getField(context[i], field);
            if (rv != null) {
                return rv;
            }
        }
        return null;
    }

    Object lookupHashFieldInContext(String field, String key) {
        for (int i=0; i<context.length; i++) {
            Hashtable ht = (Hashtable)getField(context[i], field);
            if (ht != null) {
                Object rv = ht.get(key);
                if (rv != null) {
                    return rv;
                }
            }
        }
        return null;
    }

    void put(String key, String value) {
        vars.put(key, value);
    }

    void put(String key, Vector vvalue) {
        vars.put(key, vvalue);
    }

    void add(String key, Vector vvalue) {
        getV(key).addAll(vvalue);
    }

    String flavour() {
        return get("Flavour");
    }

    String build() {
        return get("Build");
    }

    Object getSpecificField(String field) {
        return getField(get("Id"), field);
    }

    void putSpecificField(String field, Object value) {
        putField(get("Id"), field, value);
    }

    void collectRelevantVectors(Vector rv, String field) {
        for (String ctx : context) {
            Vector<String> v = getFieldVector(ctx, field);
            if (v != null) {
                for (String val : v) {
                    rv.add(expandFormat(val).replace('/', '\\'));
                }
            }
        }
    }

    void collectRelevantHashes(Hashtable rv, String field) {
        for (String ctx : context) {
            Hashtable v = (Hashtable)getField(ctx, field);
            if (v != null) {
                for (Enumeration e=v.keys(); e.hasMoreElements(); ) {
                    String key = (String)e.nextElement();
                    String val =  (String)v.get(key);
                    addTo(rv, key, val);
                }
            }
        }
    }


    Vector getDefines() {
        Vector rv = new Vector();
        collectRelevantVectors(rv, "Define");
        return rv;
    }

    Vector getIncludes() {
        Vector rv = new Vector();
        collectRelevantVectors(rv, "AbsoluteInclude");
        rv.addAll(getSourceIncludes());
        return rv;
    }

    private Vector getSourceIncludes() {
        Vector<String> rv = new Vector<String>();
        String sourceBase = getFieldString(null, "SourceBase");

        // add relative alternate source include values:
        String relativeAltSrcInclude =
            getFieldString(null, "RelativeAltSrcInclude");
        Vector<String> asri = new Vector<String>();
        collectRelevantVectors(asri, "AltRelativeInclude");
        for (String f : asri) {
            rv.add(sourceBase + Util.sep + relativeAltSrcInclude +
                   Util.sep + f);
        }

        Vector<String> ri = new Vector<String>();
        collectRelevantVectors(ri, "RelativeInclude");
        for (String f : ri) {
            rv.add(sourceBase + Util.sep + f);
        }
        return rv;
    }

    static Hashtable cfgData = new Hashtable();
    static Hashtable globalData = new Hashtable();

    static boolean appliesToTieredBuild(String cfg) {
        return (cfg != null &&
                cfg.startsWith("server"));
    }

    // Filters out the IgnoreFile and IgnorePaths since they are
    // handled specially for tiered builds.
    static boolean appliesToTieredBuild(String cfg, String key) {
        return (appliesToTieredBuild(cfg))&& (key != null && !key.startsWith("Ignore"));
    }

    static String getTieredBuildCfg(String cfg) {
        assert appliesToTieredBuild(cfg) : "illegal configuration " + cfg;
        return "server";
    }

    static Object getField(String cfg, String field) {
        if (cfg == null) {
            return globalData.get(field);
        }

        Hashtable ht =  (Hashtable)cfgData.get(cfg);
        return ht == null ? null : ht.get(field);
    }

    static String getFieldString(String cfg, String field) {
        return (String)getField(cfg, field);
    }

    static Vector getFieldVector(String cfg, String field) {
        return (Vector)getField(cfg, field);
    }

    static void putField(String cfg, String field, Object value) {
        putFieldImpl(cfg, field, value);
        if (appliesToTieredBuild(cfg, field)) {
            putFieldImpl(getTieredBuildCfg(cfg), field, value);
        }
    }

    private static void putFieldImpl(String cfg, String field, Object value) {
        if (cfg == null) {
            globalData.put(field, value);
            return;
        }

        Hashtable ht = (Hashtable)cfgData.get(cfg);
        if (ht == null) {
            ht = new Hashtable();
            cfgData.put(cfg, ht);
        }

        ht.put(field, value);
    }

    static Object getFieldHash(String cfg, String field, String name) {
        Hashtable ht = (Hashtable)getField(cfg, field);

        return ht == null ? null : ht.get(name);
    }

    static void putFieldHash(String cfg, String field, String name, Object val) {
        putFieldHashImpl(cfg, field, name, val);
        if (appliesToTieredBuild(cfg, field)) {
            putFieldHashImpl(getTieredBuildCfg(cfg), field, name, val);
        }
    }

    private static void putFieldHashImpl(String cfg, String field, String name, Object val) {
        Hashtable ht = (Hashtable)getField(cfg, field);

        if (ht == null) {
            ht = new Hashtable();
            putFieldImpl(cfg, field, ht);
        }

        ht.put(name, val);
    }

    static void addFieldVector(String cfg, String field, String element) {
        addFieldVectorImpl(cfg, field, element);
        if (appliesToTieredBuild(cfg, field)) {
            addFieldVectorImpl(getTieredBuildCfg(cfg), field, element);
        }
    }

    private static void addFieldVectorImpl(String cfg, String field, String element) {
        Vector v = (Vector)getField(cfg, field);

        if (v == null) {
            v = new Vector();
            putFieldImpl(cfg, field, v);
        }

        v.add(element);
    }

    String expandFormat(String format) {
        if (format == null) {
            return null;
        }

        if (format.indexOf('%') == -1) {
            return format;
        }

        StringBuffer sb = new StringBuffer();
        int len = format.length();
        for (int i=0; i<len; i++) {
            char ch = format.charAt(i);
            if (ch == '%') {
                char ch1 = format.charAt(i+1);
                switch (ch1) {
                case '%':
                    sb.append(ch1);
                    break;
                case 'b':
                    sb.append(build());
                    break;
                case 'f':
                    sb.append(flavour());
                    break;
                default:
                    sb.append(ch);
                    sb.append(ch1);
                }
                i++;
            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }
}

abstract class GenericDebugConfig extends BuildConfig {
    abstract String getOptFlag();

    protected void init(Vector includes, Vector defines) {
        defines.add("_DEBUG");
        defines.add("ASSERT");

        super.init(includes, defines);

        getV("CompilerFlags").addAll(getCI().getDebugCompilerFlags(getOptFlag(), get("PlatformName")));
        getV("LinkerFlags").addAll(getCI().getDebugLinkerFlags());
   }
}

abstract class GenericDebugNonKernelConfig extends GenericDebugConfig {
    protected void init(Vector includes, Vector defines) {
        super.init(includes, defines);
        if (get("PlatformName").equals("Win32")) {
            getCI().getAdditionalNonKernelLinkerFlags(getV("LinkerFlags"));
        }
   }
}

class C1DebugConfig extends GenericDebugNonKernelConfig {
    String getOptFlag() {
        return getCI().getNoOptFlag();
    }

    C1DebugConfig() {
        initNames("client", "debug", "jvm.dll");
        init(getIncludes(), getDefines());
    }
}

class C1FastDebugConfig extends GenericDebugNonKernelConfig {
    String getOptFlag() {
        return getCI().getOptFlag();
    }

    C1FastDebugConfig() {
        initNames("client", "fastdebug", "jvm.dll");
        init(getIncludes(), getDefines());
    }
}

class TieredDebugConfig extends GenericDebugNonKernelConfig {
    String getOptFlag() {
        return getCI().getNoOptFlag();
    }

    TieredDebugConfig() {
        initNames("server", "debug", "jvm.dll");
        init(getIncludes(), getDefines());
    }
}

class TieredFastDebugConfig extends GenericDebugNonKernelConfig {
    String getOptFlag() {
        return getCI().getOptFlag();
    }

    TieredFastDebugConfig() {
        initNames("server", "fastdebug", "jvm.dll");
        init(getIncludes(), getDefines());
    }
}

abstract class ProductConfig extends BuildConfig {
    protected void init(Vector includes, Vector defines) {
        defines.add("NDEBUG");
        defines.add("PRODUCT");

        super.init(includes, defines);

        getV("CompilerFlags").addAll(getCI().getProductCompilerFlags());
        getV("LinkerFlags").addAll(getCI().getProductLinkerFlags());
    }
}

class C1ProductConfig extends ProductConfig {
    C1ProductConfig() {
        initNames("client", "product", "jvm.dll");
        init(getIncludes(), getDefines());
    }
}

class TieredProductConfig extends ProductConfig {
    TieredProductConfig() {
        initNames("server", "product", "jvm.dll");
        init(getIncludes(), getDefines());
    }
}


abstract class CompilerInterface {
    abstract Vector getBaseCompilerFlags(Vector defines, Vector includes, String outDir);
    abstract Vector getBaseLinkerFlags(String outDir, String outDll, String platformName);
    abstract Vector getDebugCompilerFlags(String opt, String platformName);
    abstract Vector getDebugLinkerFlags();
    abstract void   getAdditionalNonKernelLinkerFlags(Vector rv);
    abstract Vector getProductCompilerFlags();
    abstract Vector getProductLinkerFlags();
    abstract String getOptFlag();
    abstract String getNoOptFlag();
    abstract String makeCfgName(String flavourBuild, String platformName);

    void addAttr(Vector receiver, String attr, String value) {
        receiver.add(attr); receiver.add(value);
    }
    void extAttr(Vector receiver, String attr, String value) {
        int attr_pos=receiver.indexOf(attr) ;
        if ( attr_pos == -1) {
          // If attr IS NOT present in the Vector - add it
          receiver.add(attr); receiver.add(value);
        } else {
          // If attr IS present in the Vector - append value to it
          receiver.set(attr_pos+1,receiver.get(attr_pos+1)+value);
        }
    }
}
