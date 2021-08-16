/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

// MAIN.CPP - Entry point for the Architecture Description Language Compiler
#include "adlc.hpp"

//------------------------------Prototypes-------------------------------------
static void  usage(ArchDesc& AD);          // Print usage message and exit
static char *strip_ext(char *fname);       // Strip off name extension
static char *base_plus_suffix(const char* base, const char *suffix);// New concatenated string
static int get_legal_text(FileBuff &fbuf, char **legal_text); // Get pointer to legal text

ArchDesc* globalAD = NULL;      // global reference to Architecture Description object

const char* get_basename(const char* filename) {
  const char *basename = filename;
  const char *cp;
  for (cp = basename; *cp; cp++) {
    if (*cp == '/' || *cp == '\\') {
      basename = cp+1;
    }
  }
  return basename;
}

//------------------------------main-------------------------------------------
int main(int argc, char *argv[])
{
  ArchDesc      AD;             // Architecture Description object
  globalAD = &AD;

  // ResourceMark  mark;
  ADLParser    *ADL_Parse;      // ADL Parser object to parse AD file

  // Check for proper arguments
  if( argc == 1 ) usage(AD);    // No arguments?  Then print usage

  // Read command line arguments and file names
  for( int i = 1; i < argc; i++ ) { // For all arguments
    char *s = argv[i];          // Get option/filename

    if( *s++ == '-' ) {         // It's a flag? (not a filename)
      if( !*s ) {               // Stand-alone `-' means stdin
        //********** INSERT CODE HERE **********
      } else while (*s != '\0') { // While have flags on option
        switch (*s++) {         // Handle flag
        case 'd':               // Debug flag
          AD._dfa_debug += 1;   // Set Debug Flag
          break;
        case 'g':               // Debug ad location flag
          AD._adlocation_debug += 1;       // Set Debug ad location Flag
          break;
        case 'o':               // No Output Flag
          AD._no_output ^= 1;   // Toggle no_output flag
          break;
        case 'q':               // Quiet Mode Flag
          AD._quiet_mode ^= 1;  // Toggle quiet_mode flag
          break;
        case 'w':               // Disable Warnings Flag
          AD._disable_warnings ^= 1; // Toggle disable_warnings flag
          break;
        case 'T':               // Option to make DFA as many subroutine calls.
          AD._dfa_small += 1;   // Set Mode Flag
          break;
        case 'c': {             // Set C++ Output file name
          AD._CPP_file._name = s;
          const char *base = strip_ext(strdup(s));
          AD._CPP_CLONE_file._name    = base_plus_suffix(base,"_clone.cpp");
          AD._CPP_EXPAND_file._name   = base_plus_suffix(base,"_expand.cpp");
          AD._CPP_FORMAT_file._name   = base_plus_suffix(base,"_format.cpp");
          AD._CPP_GEN_file._name      = base_plus_suffix(base,"_gen.cpp");
          AD._CPP_MISC_file._name     = base_plus_suffix(base,"_misc.cpp");
          AD._CPP_PEEPHOLE_file._name = base_plus_suffix(base,"_peephole.cpp");
          AD._CPP_PIPELINE_file._name = base_plus_suffix(base,"_pipeline.cpp");
          s += strlen(s);
          break;
        }
        case 'h':               // Set C++ Output file name
          AD._HPP_file._name = s; s += strlen(s);
          break;
        case 'v':               // Set C++ Output file name
          AD._VM_file._name = s; s += strlen(s);
          break;
        case 'a':               // Set C++ Output file name
          AD._DFA_file._name = s;
          AD._bug_file._name = s;
          s += strlen(s);
          break;
        case '#':               // Special internal debug flag
          AD._adl_debug++;      // Increment internal debug level
          break;
        case 's':               // Output which instructions are cisc-spillable
          AD._cisc_spill_debug = true;
          break;
        case 'D':               // Flag Definition
          {
            char* flag = s;
            s += strlen(s);
            char* def = strchr(flag, '=');
            if (def == NULL)  def = (char*)"1";
            else              *def++ = '\0';
            AD.set_preproc_def(flag, def);
          }
          break;
        case 'U':               // Flag Un-Definition
          {
            char* flag = s;
            s += strlen(s);
            AD.set_preproc_def(flag, NULL);
          }
          break;
        default:                // Unknown option
          usage(AD);            // So print usage and exit
        }                       // End of switch on options...
      }                         // End of while have options...

    } else {                    // Not an option; must be a filename
      AD._ADL_file._name = argv[i]; // Set the input filename

      // // Files for storage, based on input file name
      const char *base = strip_ext(strdup(argv[i]));
      char       *temp = base_plus_suffix("dfa_",base);
      AD._DFA_file._name = base_plus_suffix(temp,".cpp");
      delete[] temp;
      temp = base_plus_suffix("ad_",base);
      AD._CPP_file._name          = base_plus_suffix(temp,".cpp");
      AD._CPP_CLONE_file._name    = base_plus_suffix(temp,"_clone.cpp");
      AD._CPP_EXPAND_file._name   = base_plus_suffix(temp,"_expand.cpp");
      AD._CPP_FORMAT_file._name   = base_plus_suffix(temp,"_format.cpp");
      AD._CPP_GEN_file._name      = base_plus_suffix(temp,"_gen.cpp");
      AD._CPP_MISC_file._name     = base_plus_suffix(temp,"_misc.cpp");
      AD._CPP_PEEPHOLE_file._name = base_plus_suffix(temp,"_peephole.cpp");
      AD._CPP_PIPELINE_file._name = base_plus_suffix(temp,"_pipeline.cpp");
      AD._HPP_file._name = base_plus_suffix(temp,".hpp");
      delete[] temp;
      temp = base_plus_suffix("adGlobals_",base);
      AD._VM_file._name = base_plus_suffix(temp,".hpp");
      delete[] temp;
      temp = base_plus_suffix("bugs_",base);
      AD._bug_file._name = base_plus_suffix(temp,".out");
      delete[] temp;
    }                           // End of files vs options...
  }                             // End of while have command line arguments

  // Open files used to store the matcher and its components
  if (AD.open_files() == 0) return 1; // Open all input/output files

  // Build the File Buffer, Parse the input, & Generate Code
  FileBuff  ADL_Buf(&AD._ADL_file, AD); // Create a file buffer for input file

  // Get pointer to legal text at the beginning of AD file.
  // It will be used in generated ad files.
  char* legal_text;
  int legal_sz = get_legal_text(ADL_Buf, &legal_text);

  ADL_Parse = new ADLParser(ADL_Buf, AD); // Create a parser to parse the buffer
  ADL_Parse->parse();           // Parse buffer & build description lists

  if( AD._dfa_debug >= 1 ) {    // For higher debug settings, print dump
    AD.dump();
  }

  delete ADL_Parse;             // Delete parser

  // Verify that the results of the parse are consistent
  AD.verify();

  // Prepare to generate the result files:
  AD.generateMatchLists();
  AD.identify_unique_operands();
  AD.identify_cisc_spill_instructions();
  AD.identify_short_branches();
  // Make sure every file starts with a copyright:
  AD.addSunCopyright(legal_text, legal_sz, AD._HPP_file._fp);           // .hpp
  AD.addSunCopyright(legal_text, legal_sz, AD._CPP_file._fp);           // .cpp
  AD.addSunCopyright(legal_text, legal_sz, AD._CPP_CLONE_file._fp);     // .cpp
  AD.addSunCopyright(legal_text, legal_sz, AD._CPP_EXPAND_file._fp);    // .cpp
  AD.addSunCopyright(legal_text, legal_sz, AD._CPP_FORMAT_file._fp);    // .cpp
  AD.addSunCopyright(legal_text, legal_sz, AD._CPP_GEN_file._fp);       // .cpp
  AD.addSunCopyright(legal_text, legal_sz, AD._CPP_MISC_file._fp);      // .cpp
  AD.addSunCopyright(legal_text, legal_sz, AD._CPP_PEEPHOLE_file._fp);  // .cpp
  AD.addSunCopyright(legal_text, legal_sz, AD._CPP_PIPELINE_file._fp);  // .cpp
  AD.addSunCopyright(legal_text, legal_sz, AD._VM_file._fp);            // .hpp
  AD.addSunCopyright(legal_text, legal_sz, AD._DFA_file._fp);           // .cpp
  // Add include guards for all .hpp files
  AD.addIncludeGuardStart(AD._HPP_file, "GENERATED_ADFILES_AD_HPP");        // .hpp
  AD.addIncludeGuardStart(AD._VM_file, "GENERATED_ADFILES_ADGLOBALS_HPP");  // .hpp
  // Add includes
  AD.addInclude(AD._CPP_file, "precompiled.hpp");
  AD.addInclude(AD._CPP_file, "adfiles", get_basename(AD._VM_file._name));
  AD.addInclude(AD._CPP_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._CPP_file, "memory/allocation.inline.hpp");
  AD.addInclude(AD._CPP_file, "code/codeCache.hpp");
  AD.addInclude(AD._CPP_file, "code/compiledIC.hpp");
  AD.addInclude(AD._CPP_file, "code/nativeInst.hpp");
  AD.addInclude(AD._CPP_file, "code/vmreg.inline.hpp");
  AD.addInclude(AD._CPP_file, "gc/shared/collectedHeap.inline.hpp");
  AD.addInclude(AD._CPP_file, "oops/compiledICHolder.hpp");
  AD.addInclude(AD._CPP_file, "oops/compressedOops.hpp");
  AD.addInclude(AD._CPP_file, "oops/markWord.hpp");
  AD.addInclude(AD._CPP_file, "oops/method.hpp");
  AD.addInclude(AD._CPP_file, "oops/oop.inline.hpp");
  AD.addInclude(AD._CPP_file, "opto/c2_MacroAssembler.hpp");
  AD.addInclude(AD._CPP_file, "opto/cfgnode.hpp");
  AD.addInclude(AD._CPP_file, "opto/intrinsicnode.hpp");
  AD.addInclude(AD._CPP_file, "opto/locknode.hpp");
  AD.addInclude(AD._CPP_file, "opto/opcodes.hpp");
  AD.addInclude(AD._CPP_file, "opto/regalloc.hpp");
  AD.addInclude(AD._CPP_file, "opto/regmask.hpp");
  AD.addInclude(AD._CPP_file, "opto/runtime.hpp");
  AD.addInclude(AD._CPP_file, "runtime/safepointMechanism.hpp");
  AD.addInclude(AD._CPP_file, "runtime/sharedRuntime.hpp");
  AD.addInclude(AD._CPP_file, "runtime/stubRoutines.hpp");
  AD.addInclude(AD._CPP_file, "utilities/growableArray.hpp");
  AD.addInclude(AD._CPP_file, "utilities/powerOfTwo.hpp");
  AD.addInclude(AD._HPP_file, "memory/allocation.hpp");
  AD.addInclude(AD._HPP_file, "oops/compressedOops.hpp");
  AD.addInclude(AD._HPP_file, "code/nativeInst.hpp");
  AD.addInclude(AD._HPP_file, "opto/output.hpp");
  AD.addInclude(AD._HPP_file, "opto/machnode.hpp");
  AD.addInclude(AD._HPP_file, "opto/node.hpp");
  AD.addInclude(AD._HPP_file, "opto/regalloc.hpp");
  AD.addInclude(AD._HPP_file, "opto/subnode.hpp");
  AD.addInclude(AD._HPP_file, "opto/vectornode.hpp");
  AD.addInclude(AD._CPP_CLONE_file, "precompiled.hpp");
  AD.addInclude(AD._CPP_CLONE_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._CPP_EXPAND_file, "precompiled.hpp");
  AD.addInclude(AD._CPP_EXPAND_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._CPP_EXPAND_file, "oops/compressedOops.hpp");
  AD.addInclude(AD._CPP_FORMAT_file, "precompiled.hpp");
  AD.addInclude(AD._CPP_FORMAT_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._CPP_FORMAT_file, "compiler/oopMap.hpp");
  AD.addInclude(AD._CPP_GEN_file, "precompiled.hpp");
  AD.addInclude(AD._CPP_GEN_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._CPP_GEN_file, "opto/cfgnode.hpp");
  AD.addInclude(AD._CPP_GEN_file, "opto/locknode.hpp");
  AD.addInclude(AD._CPP_GEN_file, "opto/rootnode.hpp");
  AD.addInclude(AD._CPP_MISC_file, "precompiled.hpp");
  AD.addInclude(AD._CPP_MISC_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._CPP_PEEPHOLE_file, "precompiled.hpp");
  AD.addInclude(AD._CPP_PEEPHOLE_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._CPP_PIPELINE_file, "precompiled.hpp");
  AD.addInclude(AD._CPP_PIPELINE_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._DFA_file, "precompiled.hpp");
  AD.addInclude(AD._DFA_file, "adfiles", get_basename(AD._HPP_file._name));
  AD.addInclude(AD._DFA_file, "oops/compressedOops.hpp");
  AD.addInclude(AD._DFA_file, "opto/cfgnode.hpp");  // Use PROB_MAX in predicate.
  AD.addInclude(AD._DFA_file, "opto/intrinsicnode.hpp");
  AD.addInclude(AD._DFA_file, "opto/matcher.hpp");
  AD.addInclude(AD._DFA_file, "opto/narrowptrnode.hpp");
  AD.addInclude(AD._DFA_file, "opto/opcodes.hpp");
  AD.addInclude(AD._DFA_file, "opto/convertnode.hpp");
  AD.addInclude(AD._DFA_file, "utilities/powerOfTwo.hpp");

  // Make sure each .cpp file starts with include lines:
  // files declaring and defining generators for Mach* Objects (hpp,cpp)
  // Generate the result files:
  // enumerations, class definitions, object generators, and the DFA
  // file containing enumeration of machine operands & instructions (hpp)
  AD.addPreHeaderBlocks(AD._HPP_file._fp);        // .hpp
  AD.buildMachOperEnum(AD._HPP_file._fp);         // .hpp
  AD.buildMachOpcodesEnum(AD._HPP_file._fp);      // .hpp
  AD.buildMachRegisterNumbers(AD._VM_file._fp);   // VM file
  AD.buildMachRegisterEncodes(AD._HPP_file._fp);  // .hpp file
  AD.declareRegSizes(AD._HPP_file._fp);           // .hpp
  AD.build_pipeline_enums(AD._HPP_file._fp);      // .hpp
  // output definition of class "State"
  AD.defineStateClass(AD._HPP_file._fp);          // .hpp
  // file declaring the Mach* classes derived from MachOper and MachNode
  AD.declareClasses(AD._HPP_file._fp);
  // declare and define maps: in the .hpp and .cpp files respectively
  AD.addSourceBlocks(AD._CPP_file._fp);           // .cpp
  AD.addHeaderBlocks(AD._HPP_file._fp);           // .hpp
  AD.buildReduceMaps(AD._HPP_file._fp, AD._CPP_file._fp);
  AD.buildMustCloneMap(AD._HPP_file._fp, AD._CPP_file._fp);
  // build CISC_spilling oracle and MachNode::cisc_spill() methods
  AD.build_cisc_spill_instructions(AD._HPP_file._fp, AD._CPP_file._fp);
  // define methods for machine dependent State, MachOper, and MachNode classes
  AD.defineClasses(AD._CPP_file._fp);
  AD.buildMachOperGenerator(AD._CPP_GEN_file._fp);// .cpp
  AD.buildMachNodeGenerator(AD._CPP_GEN_file._fp);// .cpp
  // define methods for machine dependent instruction matching
  AD.buildInstructMatchCheck(AD._CPP_file._fp);  // .cpp
  // define methods for machine dependent frame management
  AD.buildFrameMethods(AD._CPP_file._fp);         // .cpp
  AD.generate_needs_deep_clone_jvms(AD._CPP_file._fp);

  // do this last:
  AD.addPreprocessorChecks(AD._CPP_file._fp);     // .cpp
  AD.addPreprocessorChecks(AD._CPP_CLONE_file._fp);     // .cpp
  AD.addPreprocessorChecks(AD._CPP_EXPAND_file._fp);    // .cpp
  AD.addPreprocessorChecks(AD._CPP_FORMAT_file._fp);    // .cpp
  AD.addPreprocessorChecks(AD._CPP_GEN_file._fp);       // .cpp
  AD.addPreprocessorChecks(AD._CPP_MISC_file._fp);      // .cpp
  AD.addPreprocessorChecks(AD._CPP_PEEPHOLE_file._fp);  // .cpp
  AD.addPreprocessorChecks(AD._CPP_PIPELINE_file._fp);  // .cpp

  // define the finite automata that selects lowest cost production
  AD.buildDFA(AD._DFA_file._fp);
  // Add include guards for all .hpp files
  AD.addIncludeGuardEnd(AD._HPP_file, "GENERATED_ADFILES_AD_HPP");        // .hpp
  AD.addIncludeGuardEnd(AD._VM_file, "GENERATED_ADFILES_ADGLOBALS_HPP");  // .hpp

  AD.close_files(0);               // Close all input/output files

  // Final printout and statistics
  // cout << program;

  if( AD._dfa_debug & 2 ) {    // For higher debug settings, print timing info
    //    Timer t_stop;
    //    Timer t_total = t_stop - t_start; // Total running time
    //    cerr << "\n---Architecture Description Totals---\n";
    //    cerr << ", Total lines: " << TotalLines;
    //    float l = TotalLines;
    //    cerr << "\nTotal Compilation Time: " << t_total << "\n";
    //    float ft = (float)t_total;
    //    if( ft > 0.0 ) fprintf(stderr,"Lines/sec: %#5.2f\n", l/ft);
  }
  return (AD._syntax_errs + AD._semantic_errs + AD._internal_errs); // Bye Bye!!
}

//------------------------------usage------------------------------------------
static void usage(ArchDesc& AD)
{
  printf("Architecture Description Language Compiler\n\n");
  printf("Usage: adlc [-doqwTs] [-#]* [-D<FLAG>[=<DEF>]] [-U<FLAG>] [-c<CPP_FILE_NAME>] [-h<HPP_FILE_NAME>] [-a<DFA_FILE_NAME>] [-v<GLOBALS_FILE_NAME>] <ADL_FILE_NAME>\n");
  printf(" d  produce DFA debugging info\n");
  printf(" o  no output produced, syntax and semantic checking only\n");
  printf(" q  quiet mode, supresses all non-essential messages\n");
  printf(" w  suppress warning messages\n");
  printf(" T  make DFA as many subroutine calls\n");
  printf(" s  output which instructions are cisc-spillable\n");
  printf(" D  define preprocessor symbol\n");
  printf(" U  undefine preprocessor symbol\n");
  printf(" c  specify CPP file name (default: %s)\n", AD._CPP_file._name);
  printf(" h  specify HPP file name (default: %s)\n", AD._HPP_file._name);
  printf(" a  specify DFA output file name\n");
  printf(" v  specify adGlobals output file name\n");
  printf(" #  increment ADL debug level\n");
  printf("\n");
}

//------------------------------open_file------------------------------------
int ArchDesc::open_file(bool required, ADLFILE & ADF, const char *action)
{
  if (required &&
      (ADF._fp = fopen(ADF._name, action)) == NULL) {
    printf("ERROR: Cannot open file for %s: %s\n", action, ADF._name);
    close_files(1);
    return 0;
  }
  return 1;
}

//------------------------------open_files-------------------------------------
int ArchDesc::open_files(void)
{
  if (_ADL_file._name == NULL)
  { printf("ERROR: No ADL input file specified\n"); return 0; }

  if (!open_file(true       , _ADL_file, "r"))          { return 0; }
  if (!open_file(!_no_output, _DFA_file, "w"))          { return 0; }
  if (!open_file(!_no_output, _HPP_file, "w"))          { return 0; }
  if (!open_file(!_no_output, _CPP_file, "w"))          { return 0; }
  if (!open_file(!_no_output, _CPP_CLONE_file, "w"))    { return 0; }
  if (!open_file(!_no_output, _CPP_EXPAND_file, "w"))   { return 0; }
  if (!open_file(!_no_output, _CPP_FORMAT_file, "w"))   { return 0; }
  if (!open_file(!_no_output, _CPP_GEN_file, "w"))      { return 0; }
  if (!open_file(!_no_output, _CPP_MISC_file, "w"))     { return 0; }
  if (!open_file(!_no_output, _CPP_PEEPHOLE_file, "w")) { return 0; }
  if (!open_file(!_no_output, _CPP_PIPELINE_file, "w")) { return 0; }
  if (!open_file(!_no_output, _VM_file , "w"))          { return 0; }
  if (!open_file(_dfa_debug != 0, _bug_file, "w"))    { return 0; }

  return 1;
}

//------------------------------close_file------------------------------------
void ArchDesc::close_file(int delete_out, ADLFILE& ADF)
{
  if (ADF._fp) {
    fclose(ADF._fp);
    if (delete_out) remove(ADF._name);
  }
}

//------------------------------close_files------------------------------------
void ArchDesc::close_files(int delete_out)
{
  if (_ADL_file._fp) fclose(_ADL_file._fp);

  close_file(delete_out, _CPP_file);
  close_file(delete_out, _CPP_CLONE_file);
  close_file(delete_out, _CPP_EXPAND_file);
  close_file(delete_out, _CPP_FORMAT_file);
  close_file(delete_out, _CPP_GEN_file);
  close_file(delete_out, _CPP_MISC_file);
  close_file(delete_out, _CPP_PEEPHOLE_file);
  close_file(delete_out, _CPP_PIPELINE_file);
  close_file(delete_out, _HPP_file);
  close_file(delete_out, _DFA_file);
  close_file(delete_out, _bug_file);

  if (!_quiet_mode) {
    printf("\n");
    if (_no_output || delete_out) {
      if (_ADL_file._name) printf("%s: ", _ADL_file._name);
      printf("No output produced");
    }
    else {
      if (_ADL_file._name) printf("%s --> ", _ADL_file._name);
      printf("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
             _CPP_file._name,
             _CPP_CLONE_file._name,
             _CPP_EXPAND_file._name,
             _CPP_FORMAT_file._name,
             _CPP_GEN_file._name,
             _CPP_MISC_file._name,
             _CPP_PEEPHOLE_file._name,
             _CPP_PIPELINE_file._name,
             _HPP_file._name,
             _DFA_file._name);
    }
    printf("\n");
  }
}

//------------------------------strip_ext--------------------------------------
static char *strip_ext(char *fname)
{
  char *ep;

  if (fname) {
    ep = fname + strlen(fname) - 1; // start at last character and look for '.'
    while (ep >= fname && *ep != '.') --ep;
    if (*ep == '.')     *ep = '\0'; // truncate string at '.'
  }
  return fname;
}

//------------------------------base_plus_suffix-------------------------------
// New concatenated string
static char *base_plus_suffix(const char* base, const char *suffix)
{
  int len = (int)strlen(base) + (int)strlen(suffix) + 1;

  char* fname = new char[len];
  sprintf(fname,"%s%s",base,suffix);
  return fname;
}

//------------------------------get_legal_text---------------------------------
// Get pointer to legal text at the beginning of AD file.
// This code assumes that a legal text starts at the beginning of .ad files,
// is commented by "//" at each line and ends with empty line.
//
int get_legal_text(FileBuff &fbuf, char **legal_text)
{
  char* legal_start = fbuf.get_line();
  assert(legal_start[0] == '/' && legal_start[1] == '/', "Incorrect header of AD file");
  char* legal_end = fbuf.get_line();
  assert(strncmp(legal_end, "// Copyright", 12) == 0, "Incorrect header of AD file");
  while(legal_end[0] == '/' && legal_end[1] == '/') {
    legal_end = fbuf.get_line();
  }
  *legal_text = legal_start;
  return (int) (legal_end - legal_start);
}

// VS2005 has its own definition, identical to this one.
#if !defined(_WIN32) || defined(_WIN64) || _MSC_VER < 1400
void *operator new( size_t size, int, const char *, int ) throw() {
  return ::operator new( size );
}
#endif
