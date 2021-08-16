/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Vector;

public class WinGammaPlatformVC10 extends WinGammaPlatform {


   LinkedList <String>filters = new LinkedList<String>();
   LinkedList <String[]>filterDeps = new LinkedList<String[]>();

    @Override
    protected String getProjectExt() {
        return ".vcxproj";
    }

    @Override
    public void writeProjectFile(String projectFileName, String projectName,
            Vector<BuildConfig> allConfigs) throws IOException {
        System.out.println();
        System.out.println("    Writing .vcxproj file: " + projectFileName);

        String projDir = Util.normalize(new File(projectFileName).getParent());

        printWriter = new PrintWriter(projectFileName, "UTF-8");
        printWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        startTag("Project",
                "DefaultTargets", "Build",
                "ToolsVersion", "4.0",
                "xmlns", "http://schemas.microsoft.com/developer/msbuild/2003");
        startTag("ItemGroup",
                "Label", "ProjectConfigurations");
        for (BuildConfig cfg : allConfigs) {
            startTag("ProjectConfiguration",
                    "Include", cfg.get("Name"));
            tagData("Configuration", cfg.get("Id"));
            tagData("Platform", cfg.get("PlatformName"));
            endTag();
        }
        endTag();

        startTag("PropertyGroup", "Label", "Globals");
        tagData("ProjectGuid", "{8822CB5C-1C41-41C2-8493-9F6E1994338B}");
        tagData("Keyword", "MakeFileProj");
        tag("SccProjectName");
        tag("SccLocalPath");
        endTag();

        tag("Import", "Project", "$(VCTargetsPath)\\Microsoft.Cpp.Default.props");

        for (BuildConfig cfg : allConfigs) {
            startTag(cfg, "PropertyGroup", "Label", "Configuration");
            tagData("ConfigurationType", "Makefile");
            tagData("UseDebugLibraries", "true");
            endTag();
        }

        tag("Import", "Project", "$(VCTargetsPath)\\Microsoft.Cpp.props");
        startTag("ImportGroup", "Label", "ExtensionSettings");
        endTag();
        for (BuildConfig cfg : allConfigs) {
            startTag(cfg, "ImportGroup", "Label", "PropertySheets");
            tag("Import",
                    "Project", "$(UserRootDir)\\Microsoft.Cpp.$(Platform).user.props",
                    "Condition", "exists('$(UserRootDir)\\Microsoft.Cpp.$(Platform).user.props')",
                    "Label", "LocalAppDataPlatform");
            endTag();
        }

        tag("PropertyGroup", "Label", "UserMacros");

        startTag("PropertyGroup");
        tagData("_ProjectFileVersion", "10.0.30319.1");
        for (BuildConfig cfg : allConfigs) {
            tagData(cfg, "OutDir", cfg.get("OutputDir") + Util.sep);
            tagData(cfg, "IntDir", cfg.get("OutputDir") + Util.sep);
            tagData(cfg, "LinkIncremental", "false");
        }
        for (BuildConfig cfg : allConfigs) {
            tagData(cfg, "CodeAnalysisRuleSet", "AllRules.ruleset");
            tag(cfg, "CodeAnalysisRules");
            tag(cfg, "CodeAnalysisRuleAssemblies");
        }
        for (BuildConfig cfg : allConfigs) {
            tagData(cfg, "NMakeBuildCommandLine", cfg.get("MakeBinary") + " -f ../../Makefile hotspot LOG=info");
            tagData(cfg, "NMakeReBuildCommandLine", cfg.get("MakeBinary") + " -f ../../Makefile clean-hotspot hotspot LOG=info");
            tagData(cfg, "NMakeCleanCommandLine", cfg.get("MakeBinary") + " -f ../../Makefile clean-hotspot LOG=info");
            tagData(cfg, "NMakeOutput", cfg.get("MakeOutput") + Util.sep + "jvm.dll");
            tagData(cfg, "NMakePreprocessorDefinitions", Util.join(";", cfg.getDefines()));
            tagData(cfg, "NMakeIncludeSearchPath", Util.join(";", cfg.getIncludes()));
        }
        endTag();

        for (BuildConfig cfg : allConfigs) {
            startTag(cfg, "ItemDefinitionGroup");
            startTag("ClCompile");
            tagV(cfg.getV("CompilerFlags"));
            endTag();

            startTag("Link");
            tagV(cfg.getV("LinkerFlags"));
            endTag();

            endTag();
        }

        writeFiles(allConfigs, projDir);

        tag("Import", "Project", "$(VCTargetsPath)\\Microsoft.Cpp.targets");
        startTag("ImportGroup", "Label", "ExtensionTargets");
        endTag();

        endTag();
        printWriter.close();
        System.out.println("    Done writing .vcxproj file.");

        writeFilterFile(projectFileName, projectName, allConfigs, projDir);
        writeUserFile(projectFileName, allConfigs);
    }


    private void writeUserFile(String projectFileName, Vector<BuildConfig> allConfigs) throws FileNotFoundException, UnsupportedEncodingException {
        String userFileName = projectFileName + ".user";
        if (new File(userFileName).exists()) {
            return;
        }
        System.out.print("    Writing .vcxproj.user file: " + userFileName);
        printWriter = new PrintWriter(userFileName, "UTF-8");

        printWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        startTag("Project",
                "ToolsVersion", "4.0",
                "xmlns", "http://schemas.microsoft.com/developer/msbuild/2003");

        for (BuildConfig cfg : allConfigs) {
            startTag(cfg, "PropertyGroup");
            tagData("LocalDebuggerCommand", cfg.get("JdkTargetRoot") + "\\bin\\java.exe");
            // Since we run "make hotspot-import", we get the correct jvm.dll by java.exe.
            // The '-XX:+PauseAtExit' option
            // causes the VM to wait for key press before exiting; this
            // allows any stdout or stderr messages to be seen before
            // the cmdtool exits.
            tagData("LocalDebuggerCommandArguments",
                    "-XX:+UnlockDiagnosticVMOptions -XX:+PauseAtExit");
            tagData("LocalDebuggerEnvironment", "JAVA_HOME=" + cfg.get("JdkTargetRoot"));
            endTag();
        }

        endTag();
        printWriter.close();
        System.out.println("    Done.");
    }

    public void addFilter(String rPath) {
       filters.add(rPath);
    }

    public void addFilterDependency(String fileLoc, String filter) {
      filterDeps.add(new String[] {fileLoc, filter});
    }

    private void writeFilterFile(String projectFileName, String projectName,
            Vector<BuildConfig> allConfigs, String base) throws FileNotFoundException, UnsupportedEncodingException {
        String filterFileName = projectFileName + ".filters";
        System.out.print("    Writing .vcxproj.filters file: " + filterFileName);
        printWriter = new PrintWriter(filterFileName, "UTF-8");

        printWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        startTag("Project",
                "ToolsVersion", "4.0",
                "xmlns", "http://schemas.microsoft.com/developer/msbuild/2003");

        startTag("ItemGroup");
        for (String filter : filters) {
           startTag("Filter", "Include",filter);
           UUID uuid = UUID.randomUUID();
           tagData("UniqueIdentifier", "{" + uuid.toString() + "}");
           endTag();
        }
        endTag();

        //TODO - do I need to split cpp and hpp files?

        // then all files
        startTag("ItemGroup");
        for (String[] dep : filterDeps) {
           String tagName = getFileTagFromSuffix(dep[0]);

           startTag(tagName, "Include", dep[0]);
           tagData("Filter", dep[1]);
           endTag();
        }
        endTag();

        endTag();
        printWriter.close();
        System.out.println("    Done.");
    }

    public String getFileTagFromSuffix(String fileName) {
       if (fileName.endsWith(".cpp")) {
          return"ClCompile";
       } else if (fileName.endsWith(".c")) {
          return "ClCompile";
       } else if (fileName.endsWith(".hpp")) {
          return"ClInclude";
       } else if (fileName.endsWith(".h")) {
          return "ClInclude";
       } else {
          return"None";
       }
    }

    void writeFiles(Vector<BuildConfig> allConfigs, String projDir) {
       // This code assummes there are no config specific includes.
       startTag("ItemGroup");

       String sourceBase = BuildConfig.getFieldString(null, "SourceBase");

       // Use first config for all global absolute includes.
       BuildConfig baseConfig = allConfigs.firstElement();
       Vector<String> rv = new Vector<String>();

       // Then use first config for all relative includes
       Vector<String> ri = new Vector<String>();
       baseConfig.collectRelevantVectors(ri, "RelativeSrcInclude");
       for (String f : ri) {
          rv.add(sourceBase + Util.sep + f);
       }

       baseConfig.collectRelevantVectors(rv, "AbsoluteSrcInclude");

       handleIncludes(rv, allConfigs);

       endTag();
    }

    // Will visit file tree for each include
    private void handleIncludes(Vector<String> includes, Vector<BuildConfig> allConfigs) {
       for (String path : includes)  {
          FileTreeCreatorVC10 ftc = new FileTreeCreatorVC10(FileSystems.getDefault().getPath(path) , allConfigs, this);
          try {
             ftc.writeFileTree();
          } catch (IOException e) {
             e.printStackTrace();
          }
       }
    }

    String buildCond(BuildConfig cfg) {
        return "'$(Configuration)|$(Platform)'=='"+cfg.get("Name")+"'";
    }

    void tagV(Vector<String> v) {
        Iterator<String> i = v.iterator();
        while(i.hasNext()) {
            String name = i.next();
            String data = i.next();
            tagData(name, data);
        }
    }

    void tagData(BuildConfig cfg, String name, String data) {
        tagData(name, data, "Condition", buildCond(cfg));
    }

    void tag(BuildConfig cfg, String name, String... attrs) {
        String[] ss = new String[attrs.length + 2];
        ss[0] = "Condition";
        ss[1] = buildCond(cfg);
        System.arraycopy(attrs, 0, ss, 2, attrs.length);

        tag(name, ss);
    }

    void startTag(BuildConfig cfg, String name, String... attrs) {
        String[] ss = new String[attrs.length + 2];
        ss[0] = "Condition";
        ss[1] = buildCond(cfg);
        System.arraycopy(attrs, 0, ss, 2, attrs.length);

        startTag(name, ss);
    }

}

class CompilerInterfaceVC10 extends CompilerInterface {

    @Override
    Vector getBaseCompilerFlags(Vector defines, Vector includes, String outDir) {
        Vector rv = new Vector();

        addAttr(rv, "AdditionalIncludeDirectories", Util.join(";", includes));
        addAttr(rv, "PreprocessorDefinitions",
                Util.join(";", defines).replace("\\\"", "\""));
        addAttr(rv, "PrecompiledHeaderFile", "precompiled.hpp");
        addAttr(rv, "PrecompiledHeaderOutputFile", outDir+Util.sep+"vm.pch");
        addAttr(rv, "AssemblerListingLocation", outDir);
        addAttr(rv, "ObjectFileName", outDir+Util.sep);
        addAttr(rv, "ProgramDataBaseFileName", outDir+Util.sep+"jvm.pdb");
        // Set /nologo option
        addAttr(rv, "SuppressStartupBanner", "true");
        // Surpass the default /Tc or /Tp.
        addAttr(rv, "CompileAs", "Default");
        // Set /W3 option.
        addAttr(rv, "WarningLevel", "Level3");
        // Set /WX option,
        addAttr(rv, "TreatWarningAsError", "true");
        // Set /GS option
        addAttr(rv, "BufferSecurityCheck", "false");
        // Set /Zi option.
        addAttr(rv, "DebugInformationFormat", "ProgramDatabase");
        // Set /Yu option.
        addAttr(rv, "PrecompiledHeader", "Use");
        // Set /EHsc- option
        addAttr(rv, "ExceptionHandling", "");

        addAttr(rv, "MultiProcessorCompilation", "true");

        return rv;
    }

    @Override
    Vector getDebugCompilerFlags(String opt, String platformName) {
        Vector rv = new Vector();

        // Set /On option
        addAttr(rv, "Optimization", opt);
        // Set /MD option.
        addAttr(rv, "RuntimeLibrary", "MultiThreadedDLL");
        // Set /Oy- option
        addAttr(rv, "OmitFramePointers", "false");
        // Set /homeparams for x64 debug builds
        if(platformName.equals("x64")) {
            addAttr(rv, "AdditionalOptions", "/homeparams");
        }

        return rv;
    }

    @Override
    Vector getProductCompilerFlags() {
        Vector rv = new Vector();

        // Set /O2 option.
        addAttr(rv, "Optimization", "MaxSpeed");
        // Set /Oy- option
        addAttr(rv, "OmitFramePointers", "false");
        // Set /Ob option.  1 is expandOnlyInline
        addAttr(rv, "InlineFunctionExpansion", "OnlyExplicitInline");
        // Set /GF option.
        addAttr(rv, "StringPooling", "true");
        // Set /MD option. 2 is rtMultiThreadedDLL
        addAttr(rv, "RuntimeLibrary", "MultiThreadedDLL");
        // Set /Gy option
        addAttr(rv, "FunctionLevelLinking", "true");

        return rv;
    }

    @Override
    Vector getBaseLinkerFlags(String outDir, String outDll, String platformName) {
        Vector rv = new Vector();

        if(platformName.equals("Win32")) {
            addAttr(rv, "AdditionalOptions",
                    "/export:JNI_GetDefaultJavaVMInitArgs " +
                    "/export:JNI_CreateJavaVM " +
                    "/export:JVM_FindClassFromBootLoader "+
                    "/export:JNI_GetCreatedJavaVMs "+
                    "/export:jio_snprintf /export:jio_printf "+
                    "/export:jio_fprintf /export:jio_vfprintf "+
                    "/export:jio_vsnprintf "+
                    "/export:JVM_InitAgentProperties");
        }
        addAttr(rv, "AdditionalDependencies", "kernel32.lib;user32.lib;gdi32.lib;winspool.lib;comdlg32.lib;advapi32.lib;shell32.lib;ole32.lib;oleaut32.lib;uuid.lib;Wsock32.lib;winmm.lib;psapi.lib;version.lib");
        addAttr(rv, "OutputFile", outDll);
        addAttr(rv, "SuppressStartupBanner", "true");
        addAttr(rv, "ModuleDefinitionFile", outDir+Util.sep+"vm.def");
        addAttr(rv, "ProgramDatabaseFile", outDir+Util.sep+"jvm.pdb");
        addAttr(rv, "SubSystem", "Windows");
        addAttr(rv, "BaseAddress", "0x8000000");
        addAttr(rv, "ImportLibrary", outDir+Util.sep+"jvm.lib");

        if(platformName.equals("Win32")) {
            addAttr(rv, "TargetMachine", "MachineX86");
        } else {
            addAttr(rv, "TargetMachine", "MachineX64");
        }

        // We always want the /DEBUG option to get full symbol information in the pdb files
        addAttr(rv, "GenerateDebugInformation", "true");

        return rv;
    }

    @Override
    Vector getDebugLinkerFlags() {
        Vector rv = new Vector();

        // Empty now that /DEBUG option is used by all configs

        return rv;
    }

    @Override
    Vector getProductLinkerFlags() {
        Vector rv = new Vector();

        // Set /OPT:REF option.
        addAttr(rv, "OptimizeReferences", "true");
        // Set /OPT:ICF option.
        addAttr(rv, "EnableCOMDATFolding", "true");

        return rv;
    }

    @Override
    void getAdditionalNonKernelLinkerFlags(Vector rv) {
        extAttr(rv, "AdditionalOptions", " /export:AsyncGetCallTrace");
    }

    @Override
    String getOptFlag() {
        return "MaxSpeed";
    }

    @Override
    String getNoOptFlag() {
        return "Disabled";
    }

    @Override
    String makeCfgName(String flavourBuild, String platform) {
        return  flavourBuild + "|" + platform;
    }

}
