/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

// output_c.cpp - Class CPP file output routines for architecture definition

#include "adlc.hpp"

// Utilities to characterize effect statements
static bool is_def(int usedef) {
  switch(usedef) {
  case Component::DEF:
  case Component::USE_DEF: return true; break;
  }
  return false;
}

// Define  an array containing the machine register names, strings.
static void defineRegNames(FILE *fp, RegisterForm *registers) {
  if (registers) {
    fprintf(fp,"\n");
    fprintf(fp,"// An array of character pointers to machine register names.\n");
    fprintf(fp,"const char *Matcher::regName[REG_COUNT] = {\n");

    // Output the register name for each register in the allocation classes
    RegDef *reg_def = NULL;
    RegDef *next = NULL;
    registers->reset_RegDefs();
    for (reg_def = registers->iter_RegDefs(); reg_def != NULL; reg_def = next) {
      next = registers->iter_RegDefs();
      const char *comma = (next != NULL) ? "," : " // no trailing comma";
      fprintf(fp,"  \"%s\"%s\n", reg_def->_regname, comma);
    }

    // Finish defining enumeration
    fprintf(fp,"};\n");

    fprintf(fp,"\n");
    fprintf(fp,"// An array of character pointers to machine register names.\n");
    fprintf(fp,"const VMReg OptoReg::opto2vm[REG_COUNT] = {\n");
    reg_def = NULL;
    next = NULL;
    registers->reset_RegDefs();
    for (reg_def = registers->iter_RegDefs(); reg_def != NULL; reg_def = next) {
      next = registers->iter_RegDefs();
      const char *comma = (next != NULL) ? "," : " // no trailing comma";
      fprintf(fp,"\t%s%s\n", reg_def->_concrete, comma);
    }
    // Finish defining array
    fprintf(fp,"\t};\n");
    fprintf(fp,"\n");

    fprintf(fp," OptoReg::Name OptoReg::vm2opto[ConcreteRegisterImpl::number_of_registers];\n");

  }
}

// Define an array containing the machine register encoding values
static void defineRegEncodes(FILE *fp, RegisterForm *registers) {
  if (registers) {
    fprintf(fp,"\n");
    fprintf(fp,"// An array of the machine register encode values\n");
    fprintf(fp,"const unsigned char Matcher::_regEncode[REG_COUNT] = {\n");

    // Output the register encoding for each register in the allocation classes
    RegDef *reg_def = NULL;
    RegDef *next    = NULL;
    registers->reset_RegDefs();
    for (reg_def = registers->iter_RegDefs(); reg_def != NULL; reg_def = next) {
      next = registers->iter_RegDefs();
      const char* register_encode = reg_def->register_encode();
      const char *comma = (next != NULL) ? "," : " // no trailing comma";
      int encval;
      if (!ADLParser::is_int_token(register_encode, encval)) {
        fprintf(fp,"  %s%s  // %s\n", register_encode, comma, reg_def->_regname);
      } else {
        // Output known constants in hex char format (backward compatibility).
        assert(encval < 256, "Exceeded supported width for register encoding");
        fprintf(fp,"  (unsigned char)'\\x%X'%s  // %s\n", encval, comma, reg_def->_regname);
      }
    }
    // Finish defining enumeration
    fprintf(fp,"};\n");

  } // Done defining array
}

// Output an enumeration of register class names
static void defineRegClassEnum(FILE *fp, RegisterForm *registers) {
  if (registers) {
    // Output an enumeration of register class names
    fprintf(fp,"\n");
    fprintf(fp,"// Enumeration of register class names\n");
    fprintf(fp, "enum machRegisterClass {\n");
    registers->_rclasses.reset();
    for (const char *class_name = NULL; (class_name = registers->_rclasses.iter()) != NULL;) {
      const char * class_name_to_upper = toUpper(class_name);
      fprintf(fp,"  %s,\n", class_name_to_upper);
      delete[] class_name_to_upper;
    }
    // Finish defining enumeration
    fprintf(fp, "  _last_Mach_Reg_Class\n");
    fprintf(fp, "};\n");
  }
}

// Declare an enumeration of user-defined register classes
// and a list of register masks, one for each class.
void ArchDesc::declare_register_masks(FILE *fp_hpp) {
  const char  *rc_name;

  if (_register) {
    // Build enumeration of user-defined register classes.
    defineRegClassEnum(fp_hpp, _register);

    // Generate a list of register masks, one for each class.
    fprintf(fp_hpp,"\n");
    fprintf(fp_hpp,"// Register masks, one for each register class.\n");
    _register->_rclasses.reset();
    for (rc_name = NULL; (rc_name = _register->_rclasses.iter()) != NULL;) {
      RegClass *reg_class = _register->getRegClass(rc_name);
      assert(reg_class, "Using an undefined register class");
      reg_class->declare_register_masks(fp_hpp);
    }
  }
}

// Generate an enumeration of user-defined register classes
// and a list of register masks, one for each class.
void ArchDesc::build_register_masks(FILE *fp_cpp) {
  const char  *rc_name;

  if (_register) {
    // Generate a list of register masks, one for each class.
    fprintf(fp_cpp,"\n");
    fprintf(fp_cpp,"// Register masks, one for each register class.\n");
    _register->_rclasses.reset();
    for (rc_name = NULL; (rc_name = _register->_rclasses.iter()) != NULL;) {
      RegClass *reg_class = _register->getRegClass(rc_name);
      assert(reg_class, "Using an undefined register class");
      reg_class->build_register_masks(fp_cpp);
    }
  }
}

// Compute an index for an array in the pipeline_reads_NNN arrays
static int pipeline_reads_initializer(FILE *fp_cpp, NameList &pipeline_reads, PipeClassForm *pipeclass)
{
  int templen = 1;
  int paramcount = 0;
  const char *paramname;

  if (pipeclass->_parameters.count() == 0)
    return -1;

  pipeclass->_parameters.reset();
  paramname = pipeclass->_parameters.iter();
  const PipeClassOperandForm *pipeopnd =
    (const PipeClassOperandForm *)pipeclass->_localUsage[paramname];
  if (pipeopnd && !pipeopnd->isWrite() && strcmp(pipeopnd->_stage, "Universal"))
    pipeclass->_parameters.reset();

  while ( (paramname = pipeclass->_parameters.iter()) != NULL ) {
    const PipeClassOperandForm *tmppipeopnd =
        (const PipeClassOperandForm *)pipeclass->_localUsage[paramname];

    if (tmppipeopnd)
      templen += 10 + (int)strlen(tmppipeopnd->_stage);
    else
      templen += 19;

    paramcount++;
  }

  // See if the count is zero
  if (paramcount == 0) {
    return -1;
  }

  char *operand_stages = new char [templen];
  operand_stages[0] = 0;
  int i = 0;
  templen = 0;

  pipeclass->_parameters.reset();
  paramname = pipeclass->_parameters.iter();
  pipeopnd = (const PipeClassOperandForm *)pipeclass->_localUsage[paramname];
  if (pipeopnd && !pipeopnd->isWrite() && strcmp(pipeopnd->_stage, "Universal"))
    pipeclass->_parameters.reset();

  while ( (paramname = pipeclass->_parameters.iter()) != NULL ) {
    const PipeClassOperandForm *tmppipeopnd =
        (const PipeClassOperandForm *)pipeclass->_localUsage[paramname];
    templen += sprintf(&operand_stages[templen], "  stage_%s%c\n",
      tmppipeopnd ? tmppipeopnd->_stage : "undefined",
      (++i < paramcount ? ',' : ' ') );
  }

  // See if the same string is in the table
  int ndx = pipeline_reads.index(operand_stages);

  // No, add it to the table
  if (ndx < 0) {
    pipeline_reads.addName(operand_stages);
    ndx = pipeline_reads.index(operand_stages);

    fprintf(fp_cpp, "static const enum machPipelineStages pipeline_reads_%03d[%d] = {\n%s};\n\n",
      ndx+1, paramcount, operand_stages);
  }
  else
    delete [] operand_stages;

  return (ndx);
}

// Compute an index for an array in the pipeline_res_stages_NNN arrays
static int pipeline_res_stages_initializer(
  FILE *fp_cpp,
  PipelineForm *pipeline,
  NameList &pipeline_res_stages,
  PipeClassForm *pipeclass)
{
  const PipeClassResourceForm *piperesource;
  int * res_stages = new int [pipeline->_rescount];
  int i;

  for (i = 0; i < pipeline->_rescount; i++)
     res_stages[i] = 0;

  for (pipeclass->_resUsage.reset();
       (piperesource = (const PipeClassResourceForm *)pipeclass->_resUsage.iter()) != NULL; ) {
    int used_mask = pipeline->_resdict[piperesource->_resource]->is_resource()->mask();
    for (i = 0; i < pipeline->_rescount; i++)
      if ((1 << i) & used_mask) {
        int stage = pipeline->_stages.index(piperesource->_stage);
        if (res_stages[i] < stage+1)
          res_stages[i] = stage+1;
      }
  }

  // Compute the length needed for the resource list
  int commentlen = 0;
  int max_stage = 0;
  for (i = 0; i < pipeline->_rescount; i++) {
    if (res_stages[i] == 0) {
      if (max_stage < 9)
        max_stage = 9;
    }
    else {
      int stagelen = (int)strlen(pipeline->_stages.name(res_stages[i]-1));
      if (max_stage < stagelen)
        max_stage = stagelen;
    }

    commentlen += (int)strlen(pipeline->_reslist.name(i));
  }

  int templen = 1 + commentlen + pipeline->_rescount * (max_stage + 14);

  // Allocate space for the resource list
  char * resource_stages = new char [templen];

  templen = 0;
  for (i = 0; i < pipeline->_rescount; i++) {
    const char * const resname =
      res_stages[i] == 0 ? "undefined" : pipeline->_stages.name(res_stages[i]-1);

    templen += sprintf(&resource_stages[templen], "  stage_%s%-*s // %s\n",
      resname, max_stage - (int)strlen(resname) + 1,
      (i < pipeline->_rescount-1) ? "," : "",
      pipeline->_reslist.name(i));
  }

  // See if the same string is in the table
  int ndx = pipeline_res_stages.index(resource_stages);

  // No, add it to the table
  if (ndx < 0) {
    pipeline_res_stages.addName(resource_stages);
    ndx = pipeline_res_stages.index(resource_stages);

    fprintf(fp_cpp, "static const enum machPipelineStages pipeline_res_stages_%03d[%d] = {\n%s};\n\n",
      ndx+1, pipeline->_rescount, resource_stages);
  }
  else
    delete [] resource_stages;

  delete [] res_stages;

  return (ndx);
}

// Compute an index for an array in the pipeline_res_cycles_NNN arrays
static int pipeline_res_cycles_initializer(
  FILE *fp_cpp,
  PipelineForm *pipeline,
  NameList &pipeline_res_cycles,
  PipeClassForm *pipeclass)
{
  const PipeClassResourceForm *piperesource;
  int * res_cycles = new int [pipeline->_rescount];
  int i;

  for (i = 0; i < pipeline->_rescount; i++)
     res_cycles[i] = 0;

  for (pipeclass->_resUsage.reset();
       (piperesource = (const PipeClassResourceForm *)pipeclass->_resUsage.iter()) != NULL; ) {
    int used_mask = pipeline->_resdict[piperesource->_resource]->is_resource()->mask();
    for (i = 0; i < pipeline->_rescount; i++)
      if ((1 << i) & used_mask) {
        int cycles = piperesource->_cycles;
        if (res_cycles[i] < cycles)
          res_cycles[i] = cycles;
      }
  }

  // Pre-compute the string length
  int templen;
  int cyclelen = 0, commentlen = 0;
  int max_cycles = 0;
  char temp[32];

  for (i = 0; i < pipeline->_rescount; i++) {
    if (max_cycles < res_cycles[i])
      max_cycles = res_cycles[i];
    templen = sprintf(temp, "%d", res_cycles[i]);
    if (cyclelen < templen)
      cyclelen = templen;
    commentlen += (int)strlen(pipeline->_reslist.name(i));
  }

  templen = 1 + commentlen + (cyclelen + 8) * pipeline->_rescount;

  // Allocate space for the resource list
  char * resource_cycles = new char [templen];

  templen = 0;

  for (i = 0; i < pipeline->_rescount; i++) {
    templen += sprintf(&resource_cycles[templen], "  %*d%c // %s\n",
      cyclelen, res_cycles[i], (i < pipeline->_rescount-1) ? ',' : ' ', pipeline->_reslist.name(i));
  }

  // See if the same string is in the table
  int ndx = pipeline_res_cycles.index(resource_cycles);

  // No, add it to the table
  if (ndx < 0) {
    pipeline_res_cycles.addName(resource_cycles);
    ndx = pipeline_res_cycles.index(resource_cycles);

    fprintf(fp_cpp, "static const uint pipeline_res_cycles_%03d[%d] = {\n%s};\n\n",
      ndx+1, pipeline->_rescount, resource_cycles);
  }
  else
    delete [] resource_cycles;

  delete [] res_cycles;

  return (ndx);
}

//typedef unsigned long long uint64_t;

// Compute an index for an array in the pipeline_res_mask_NNN arrays
static int pipeline_res_mask_initializer(
  FILE *fp_cpp,
  PipelineForm *pipeline,
  NameList &pipeline_res_mask,
  NameList &pipeline_res_args,
  PipeClassForm *pipeclass)
{
  const PipeClassResourceForm *piperesource;
  const uint rescount      = pipeline->_rescount;
  const uint maxcycleused  = pipeline->_maxcycleused;
  const uint cyclemasksize = (maxcycleused + 31) >> 5;

  int i, j;
  int element_count = 0;
  uint *res_mask = new uint [cyclemasksize];
  uint resources_used             = 0;
  uint resources_used_exclusively = 0;

  for (pipeclass->_resUsage.reset();
       (piperesource = (const PipeClassResourceForm*)pipeclass->_resUsage.iter()) != NULL; ) {
    element_count++;
  }

  // Pre-compute the string length
  int templen;
  int commentlen = 0;
  int max_cycles = 0;

  int cyclelen = ((maxcycleused + 3) >> 2);
  int masklen = (rescount + 3) >> 2;

  int cycledigit = 0;
  for (i = maxcycleused; i > 0; i /= 10)
    cycledigit++;

  int maskdigit = 0;
  for (i = rescount; i > 0; i /= 10)
    maskdigit++;

  static const char* pipeline_use_cycle_mask = "Pipeline_Use_Cycle_Mask";
  static const char* pipeline_use_element    = "Pipeline_Use_Element";

  templen = 1 +
    (int)(strlen(pipeline_use_cycle_mask) + (int)strlen(pipeline_use_element) +
     (cyclemasksize * 12) + masklen + (cycledigit * 2) + 30) * element_count;

  // Allocate space for the resource list
  char * resource_mask = new char [templen];
  char * last_comma = NULL;

  templen = 0;

  for (pipeclass->_resUsage.reset();
       (piperesource = (const PipeClassResourceForm*)pipeclass->_resUsage.iter()) != NULL; ) {
    int used_mask = pipeline->_resdict[piperesource->_resource]->is_resource()->mask();

    if (!used_mask) {
      fprintf(stderr, "*** used_mask is 0 ***\n");
    }

    resources_used |= used_mask;

    uint lb, ub;

    for (lb =  0; (used_mask & (1 << lb)) == 0; lb++);
    for (ub = 31; (used_mask & (1 << ub)) == 0; ub--);

    if (lb == ub) {
      resources_used_exclusively |= used_mask;
    }

    int formatlen =
      sprintf(&resource_mask[templen], "  %s(0x%0*x, %*d, %*d, %s %s(",
        pipeline_use_element,
        masklen, used_mask,
        cycledigit, lb, cycledigit, ub,
        ((used_mask & (used_mask-1)) != 0) ? "true, " : "false,",
        pipeline_use_cycle_mask);

    templen += formatlen;

    memset(res_mask, 0, cyclemasksize * sizeof(uint));

    int cycles = piperesource->_cycles;
    uint stage          = pipeline->_stages.index(piperesource->_stage);
    if ((uint)NameList::Not_in_list == stage) {
      fprintf(stderr,
              "pipeline_res_mask_initializer: "
              "semantic error: "
              "pipeline stage undeclared: %s\n",
              piperesource->_stage);
      exit(1);
    }
    uint upper_limit    = stage + cycles - 1;
    uint lower_limit    = stage - 1;
    uint upper_idx      = upper_limit >> 5;
    uint lower_idx      = lower_limit >> 5;
    uint upper_position = upper_limit & 0x1f;
    uint lower_position = lower_limit & 0x1f;

    uint mask = (((uint)1) << upper_position) - 1;

    while (upper_idx > lower_idx) {
      res_mask[upper_idx--] |= mask;
      mask = (uint)-1;
    }

    mask -= (((uint)1) << lower_position) - 1;
    res_mask[upper_idx] |= mask;

    for (j = cyclemasksize-1; j >= 0; j--) {
      formatlen =
        sprintf(&resource_mask[templen], "0x%08x%s", res_mask[j], j > 0 ? ", " : "");
      templen += formatlen;
    }

    resource_mask[templen++] = ')';
    resource_mask[templen++] = ')';
    last_comma = &resource_mask[templen];
    resource_mask[templen++] = ',';
    resource_mask[templen++] = '\n';
  }

  resource_mask[templen] = 0;
  if (last_comma) {
    last_comma[0] = ' ';
  }

  // See if the same string is in the table
  int ndx = pipeline_res_mask.index(resource_mask);

  // No, add it to the table
  if (ndx < 0) {
    pipeline_res_mask.addName(resource_mask);
    ndx = pipeline_res_mask.index(resource_mask);

    if (strlen(resource_mask) > 0)
      fprintf(fp_cpp, "static const Pipeline_Use_Element pipeline_res_mask_%03d[%d] = {\n%s};\n\n",
        ndx+1, element_count, resource_mask);

    char* args = new char [9 + 2*masklen + maskdigit];

    sprintf(args, "0x%0*x, 0x%0*x, %*d",
      masklen, resources_used,
      masklen, resources_used_exclusively,
      maskdigit, element_count);

    pipeline_res_args.addName(args);
  }
  else {
    delete [] resource_mask;
  }

  delete [] res_mask;
//delete [] res_masks;

  return (ndx);
}

void ArchDesc::build_pipe_classes(FILE *fp_cpp) {
  const char *classname;
  const char *resourcename;
  int resourcenamelen = 0;
  NameList pipeline_reads;
  NameList pipeline_res_stages;
  NameList pipeline_res_cycles;
  NameList pipeline_res_masks;
  NameList pipeline_res_args;
  const int default_latency = 1;
  const int non_operand_latency = 0;
  const int node_latency = 0;

  if (!_pipeline) {
    fprintf(fp_cpp, "uint Node::latency(uint i) const {\n");
    fprintf(fp_cpp, "  // assert(false, \"pipeline functionality is not defined\");\n");
    fprintf(fp_cpp, "  return %d;\n", non_operand_latency);
    fprintf(fp_cpp, "}\n");
    return;
  }

  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "//------------------Pipeline Methods-----------------------------------------\n");
  fprintf(fp_cpp, "#ifndef PRODUCT\n");
  fprintf(fp_cpp, "const char * Pipeline::stageName(uint s) {\n");
  fprintf(fp_cpp, "  static const char * const _stage_names[] = {\n");
  fprintf(fp_cpp, "    \"undefined\"");

  for (int s = 0; s < _pipeline->_stagecnt; s++)
    fprintf(fp_cpp, ", \"%s\"", _pipeline->_stages.name(s));

  fprintf(fp_cpp, "\n  };\n\n");
  fprintf(fp_cpp, "  return (s <= %d ? _stage_names[s] : \"???\");\n",
    _pipeline->_stagecnt);
  fprintf(fp_cpp, "}\n");
  fprintf(fp_cpp, "#endif\n\n");

  fprintf(fp_cpp, "uint Pipeline::functional_unit_latency(uint start, const Pipeline *pred) const {\n");
  fprintf(fp_cpp, "  // See if the functional units overlap\n");
#if 0
  fprintf(fp_cpp, "\n#ifndef PRODUCT\n");
  fprintf(fp_cpp, "  if (TraceOptoOutput) {\n");
  fprintf(fp_cpp, "    tty->print(\"#   functional_unit_latency: start == %%d, this->exclusively == 0x%%03x, pred->exclusively == 0x%%03x\\n\", start, resourcesUsedExclusively(), pred->resourcesUsedExclusively());\n");
  fprintf(fp_cpp, "  }\n");
  fprintf(fp_cpp, "#endif\n\n");
#endif
  fprintf(fp_cpp, "  uint mask = resourcesUsedExclusively() & pred->resourcesUsedExclusively();\n");
  fprintf(fp_cpp, "  if (mask == 0)\n    return (start);\n\n");
#if 0
  fprintf(fp_cpp, "\n#ifndef PRODUCT\n");
  fprintf(fp_cpp, "  if (TraceOptoOutput) {\n");
  fprintf(fp_cpp, "    tty->print(\"#   functional_unit_latency: mask == 0x%%x\\n\", mask);\n");
  fprintf(fp_cpp, "  }\n");
  fprintf(fp_cpp, "#endif\n\n");
#endif
  fprintf(fp_cpp, "  for (uint i = 0; i < pred->resourceUseCount(); i++) {\n");
  fprintf(fp_cpp, "    const Pipeline_Use_Element *predUse = pred->resourceUseElement(i);\n");
  fprintf(fp_cpp, "    if (predUse->multiple())\n");
  fprintf(fp_cpp, "      continue;\n\n");
  fprintf(fp_cpp, "    for (uint j = 0; j < resourceUseCount(); j++) {\n");
  fprintf(fp_cpp, "      const Pipeline_Use_Element *currUse = resourceUseElement(j);\n");
  fprintf(fp_cpp, "      if (currUse->multiple())\n");
  fprintf(fp_cpp, "        continue;\n\n");
  fprintf(fp_cpp, "      if (predUse->used() & currUse->used()) {\n");
  fprintf(fp_cpp, "        Pipeline_Use_Cycle_Mask x = predUse->mask();\n");
  fprintf(fp_cpp, "        Pipeline_Use_Cycle_Mask y = currUse->mask();\n\n");
  fprintf(fp_cpp, "        for ( y <<= start; x.overlaps(y); start++ )\n");
  fprintf(fp_cpp, "          y <<= 1;\n");
  fprintf(fp_cpp, "      }\n");
  fprintf(fp_cpp, "    }\n");
  fprintf(fp_cpp, "  }\n\n");
  fprintf(fp_cpp, "  // There is the potential for overlap\n");
  fprintf(fp_cpp, "  return (start);\n");
  fprintf(fp_cpp, "}\n\n");
  fprintf(fp_cpp, "// The following two routines assume that the root Pipeline_Use entity\n");
  fprintf(fp_cpp, "// consists of exactly 1 element for each functional unit\n");
  fprintf(fp_cpp, "// start is relative to the current cycle; used for latency-based info\n");
  fprintf(fp_cpp, "uint Pipeline_Use::full_latency(uint delay, const Pipeline_Use &pred) const {\n");
  fprintf(fp_cpp, "  for (uint i = 0; i < pred._count; i++) {\n");
  fprintf(fp_cpp, "    const Pipeline_Use_Element *predUse = pred.element(i);\n");
  fprintf(fp_cpp, "    if (predUse->_multiple) {\n");
  fprintf(fp_cpp, "      uint min_delay = %d;\n",
    _pipeline->_maxcycleused+1);
  fprintf(fp_cpp, "      // Multiple possible functional units, choose first unused one\n");
  fprintf(fp_cpp, "      for (uint j = predUse->_lb; j <= predUse->_ub; j++) {\n");
  fprintf(fp_cpp, "        const Pipeline_Use_Element *currUse = element(j);\n");
  fprintf(fp_cpp, "        uint curr_delay = delay;\n");
  fprintf(fp_cpp, "        if (predUse->_used & currUse->_used) {\n");
  fprintf(fp_cpp, "          Pipeline_Use_Cycle_Mask x = predUse->_mask;\n");
  fprintf(fp_cpp, "          Pipeline_Use_Cycle_Mask y = currUse->_mask;\n\n");
  fprintf(fp_cpp, "          for ( y <<= curr_delay; x.overlaps(y); curr_delay++ )\n");
  fprintf(fp_cpp, "            y <<= 1;\n");
  fprintf(fp_cpp, "        }\n");
  fprintf(fp_cpp, "        if (min_delay > curr_delay)\n          min_delay = curr_delay;\n");
  fprintf(fp_cpp, "      }\n");
  fprintf(fp_cpp, "      if (delay < min_delay)\n      delay = min_delay;\n");
  fprintf(fp_cpp, "    }\n");
  fprintf(fp_cpp, "    else {\n");
  fprintf(fp_cpp, "      for (uint j = predUse->_lb; j <= predUse->_ub; j++) {\n");
  fprintf(fp_cpp, "        const Pipeline_Use_Element *currUse = element(j);\n");
  fprintf(fp_cpp, "        if (predUse->_used & currUse->_used) {\n");
  fprintf(fp_cpp, "          Pipeline_Use_Cycle_Mask x = predUse->_mask;\n");
  fprintf(fp_cpp, "          Pipeline_Use_Cycle_Mask y = currUse->_mask;\n\n");
  fprintf(fp_cpp, "          for ( y <<= delay; x.overlaps(y); delay++ )\n");
  fprintf(fp_cpp, "            y <<= 1;\n");
  fprintf(fp_cpp, "        }\n");
  fprintf(fp_cpp, "      }\n");
  fprintf(fp_cpp, "    }\n");
  fprintf(fp_cpp, "  }\n\n");
  fprintf(fp_cpp, "  return (delay);\n");
  fprintf(fp_cpp, "}\n\n");
  fprintf(fp_cpp, "void Pipeline_Use::add_usage(const Pipeline_Use &pred) {\n");
  fprintf(fp_cpp, "  for (uint i = 0; i < pred._count; i++) {\n");
  fprintf(fp_cpp, "    const Pipeline_Use_Element *predUse = pred.element(i);\n");
  fprintf(fp_cpp, "    if (predUse->_multiple) {\n");
  fprintf(fp_cpp, "      // Multiple possible functional units, choose first unused one\n");
  fprintf(fp_cpp, "      for (uint j = predUse->_lb; j <= predUse->_ub; j++) {\n");
  fprintf(fp_cpp, "        Pipeline_Use_Element *currUse = element(j);\n");
  fprintf(fp_cpp, "        if ( !predUse->_mask.overlaps(currUse->_mask) ) {\n");
  fprintf(fp_cpp, "          currUse->_used |= (1 << j);\n");
  fprintf(fp_cpp, "          _resources_used |= (1 << j);\n");
  fprintf(fp_cpp, "          currUse->_mask.Or(predUse->_mask);\n");
  fprintf(fp_cpp, "          break;\n");
  fprintf(fp_cpp, "        }\n");
  fprintf(fp_cpp, "      }\n");
  fprintf(fp_cpp, "    }\n");
  fprintf(fp_cpp, "    else {\n");
  fprintf(fp_cpp, "      for (uint j = predUse->_lb; j <= predUse->_ub; j++) {\n");
  fprintf(fp_cpp, "        Pipeline_Use_Element *currUse = element(j);\n");
  fprintf(fp_cpp, "        currUse->_used |= (1 << j);\n");
  fprintf(fp_cpp, "        _resources_used |= (1 << j);\n");
  fprintf(fp_cpp, "        currUse->_mask.Or(predUse->_mask);\n");
  fprintf(fp_cpp, "      }\n");
  fprintf(fp_cpp, "    }\n");
  fprintf(fp_cpp, "  }\n");
  fprintf(fp_cpp, "}\n\n");

  fprintf(fp_cpp, "uint Pipeline::operand_latency(uint opnd, const Pipeline *pred) const {\n");
  fprintf(fp_cpp, "  int const default_latency = 1;\n");
  fprintf(fp_cpp, "\n");
#if 0
  fprintf(fp_cpp, "#ifndef PRODUCT\n");
  fprintf(fp_cpp, "  if (TraceOptoOutput) {\n");
  fprintf(fp_cpp, "    tty->print(\"#   operand_latency(%%d), _read_stage_count = %%d\\n\", opnd, _read_stage_count);\n");
  fprintf(fp_cpp, "  }\n");
  fprintf(fp_cpp, "#endif\n\n");
#endif
  fprintf(fp_cpp, "  assert(this, \"NULL pipeline info\");\n");
  fprintf(fp_cpp, "  assert(pred, \"NULL predecessor pipline info\");\n\n");
  fprintf(fp_cpp, "  if (pred->hasFixedLatency())\n    return (pred->fixedLatency());\n\n");
  fprintf(fp_cpp, "  // If this is not an operand, then assume a dependence with 0 latency\n");
  fprintf(fp_cpp, "  if (opnd > _read_stage_count)\n    return (0);\n\n");
  fprintf(fp_cpp, "  uint writeStage = pred->_write_stage;\n");
  fprintf(fp_cpp, "  uint readStage  = _read_stages[opnd-1];\n");
#if 0
  fprintf(fp_cpp, "\n#ifndef PRODUCT\n");
  fprintf(fp_cpp, "  if (TraceOptoOutput) {\n");
  fprintf(fp_cpp, "    tty->print(\"#   operand_latency: writeStage=%%s readStage=%%s, opnd=%%d\\n\", stageName(writeStage), stageName(readStage), opnd);\n");
  fprintf(fp_cpp, "  }\n");
  fprintf(fp_cpp, "#endif\n\n");
#endif
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "  if (writeStage == stage_undefined || readStage == stage_undefined)\n");
  fprintf(fp_cpp, "    return (default_latency);\n");
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "  int delta = writeStage - readStage;\n");
  fprintf(fp_cpp, "  if (delta < 0) delta = 0;\n\n");
#if 0
  fprintf(fp_cpp, "\n#ifndef PRODUCT\n");
  fprintf(fp_cpp, "  if (TraceOptoOutput) {\n");
  fprintf(fp_cpp, "    tty->print(\"# operand_latency: delta=%%d\\n\", delta);\n");
  fprintf(fp_cpp, "  }\n");
  fprintf(fp_cpp, "#endif\n\n");
#endif
  fprintf(fp_cpp, "  return (delta);\n");
  fprintf(fp_cpp, "}\n\n");

  if (!_pipeline)
    /* Do Nothing */;

  else if (_pipeline->_maxcycleused <= 32) {
    fprintf(fp_cpp, "Pipeline_Use_Cycle_Mask operator&(const Pipeline_Use_Cycle_Mask &in1, const Pipeline_Use_Cycle_Mask &in2) {\n");
    fprintf(fp_cpp, "  return Pipeline_Use_Cycle_Mask(in1._mask & in2._mask);\n");
    fprintf(fp_cpp, "}\n\n");
    fprintf(fp_cpp, "Pipeline_Use_Cycle_Mask operator|(const Pipeline_Use_Cycle_Mask &in1, const Pipeline_Use_Cycle_Mask &in2) {\n");
    fprintf(fp_cpp, "  return Pipeline_Use_Cycle_Mask(in1._mask | in2._mask);\n");
    fprintf(fp_cpp, "}\n\n");
  }
  else {
    uint l;
    uint masklen = (_pipeline->_maxcycleused + 31) >> 5;
    fprintf(fp_cpp, "Pipeline_Use_Cycle_Mask operator&(const Pipeline_Use_Cycle_Mask &in1, const Pipeline_Use_Cycle_Mask &in2) {\n");
    fprintf(fp_cpp, "  return Pipeline_Use_Cycle_Mask(");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_cpp, "in1._mask%d & in2._mask%d%s\n", l, l, l < masklen ? ", " : "");
    fprintf(fp_cpp, ");\n");
    fprintf(fp_cpp, "}\n\n");
    fprintf(fp_cpp, "Pipeline_Use_Cycle_Mask operator|(const Pipeline_Use_Cycle_Mask &in1, const Pipeline_Use_Cycle_Mask &in2) {\n");
    fprintf(fp_cpp, "  return Pipeline_Use_Cycle_Mask(");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_cpp, "in1._mask%d | in2._mask%d%s", l, l, l < masklen ? ", " : "");
    fprintf(fp_cpp, ");\n");
    fprintf(fp_cpp, "}\n\n");
    fprintf(fp_cpp, "void Pipeline_Use_Cycle_Mask::Or(const Pipeline_Use_Cycle_Mask &in2) {\n ");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_cpp, " _mask%d |= in2._mask%d;", l, l);
    fprintf(fp_cpp, "\n}\n\n");
  }

  /* Get the length of all the resource names */
  for (_pipeline->_reslist.reset(), resourcenamelen = 0;
       (resourcename = _pipeline->_reslist.iter()) != NULL;
       resourcenamelen += (int)strlen(resourcename));

  // Create the pipeline class description

  fprintf(fp_cpp, "static const Pipeline pipeline_class_Zero_Instructions(0, 0, true, 0, 0, false, false, false, false, NULL, NULL, NULL, Pipeline_Use(0, 0, 0, NULL));\n\n");
  fprintf(fp_cpp, "static const Pipeline pipeline_class_Unknown_Instructions(0, 0, true, 0, 0, false, true, true, false, NULL, NULL, NULL, Pipeline_Use(0, 0, 0, NULL));\n\n");

  fprintf(fp_cpp, "const Pipeline_Use_Element Pipeline_Use::elaborated_elements[%d] = {\n", _pipeline->_rescount);
  for (int i1 = 0; i1 < _pipeline->_rescount; i1++) {
    fprintf(fp_cpp, "  Pipeline_Use_Element(0, %d, %d, false, Pipeline_Use_Cycle_Mask(", i1, i1);
    uint masklen = (_pipeline->_maxcycleused + 31) >> 5;
    for (int i2 = masklen-1; i2 >= 0; i2--)
      fprintf(fp_cpp, "0%s", i2 > 0 ? ", " : "");
    fprintf(fp_cpp, "))%s\n", i1 < (_pipeline->_rescount-1) ? "," : "");
  }
  fprintf(fp_cpp, "};\n\n");

  fprintf(fp_cpp, "const Pipeline_Use Pipeline_Use::elaborated_use(0, 0, %d, (Pipeline_Use_Element *)&elaborated_elements[0]);\n\n",
    _pipeline->_rescount);

  for (_pipeline->_classlist.reset(); (classname = _pipeline->_classlist.iter()) != NULL; ) {
    fprintf(fp_cpp, "\n");
    fprintf(fp_cpp, "// Pipeline Class \"%s\"\n", classname);
    PipeClassForm *pipeclass = _pipeline->_classdict[classname]->is_pipeclass();
    int maxWriteStage = -1;
    int maxMoreInstrs = 0;
    int paramcount = 0;
    int i = 0;
    const char *paramname;
    int resource_count = (_pipeline->_rescount + 3) >> 2;

    // Scan the operands, looking for last output stage and number of inputs
    for (pipeclass->_parameters.reset(); (paramname = pipeclass->_parameters.iter()) != NULL; ) {
      const PipeClassOperandForm *pipeopnd =
          (const PipeClassOperandForm *)pipeclass->_localUsage[paramname];
      if (pipeopnd) {
        if (pipeopnd->_iswrite) {
           int stagenum  = _pipeline->_stages.index(pipeopnd->_stage);
           int moreinsts = pipeopnd->_more_instrs;
          if ((maxWriteStage+maxMoreInstrs) < (stagenum+moreinsts)) {
            maxWriteStage = stagenum;
            maxMoreInstrs = moreinsts;
          }
        }
      }

      if (i++ > 0 || (pipeopnd && !pipeopnd->isWrite()))
        paramcount++;
    }

    // Create the list of stages for the operands that are read
    // Note that we will build a NameList to reduce the number of copies

    int pipeline_reads_index = pipeline_reads_initializer(fp_cpp, pipeline_reads, pipeclass);

    int pipeline_res_stages_index = pipeline_res_stages_initializer(
      fp_cpp, _pipeline, pipeline_res_stages, pipeclass);

    int pipeline_res_cycles_index = pipeline_res_cycles_initializer(
      fp_cpp, _pipeline, pipeline_res_cycles, pipeclass);

    int pipeline_res_mask_index = pipeline_res_mask_initializer(
      fp_cpp, _pipeline, pipeline_res_masks, pipeline_res_args, pipeclass);

#if 0
    // Process the Resources
    const PipeClassResourceForm *piperesource;

    unsigned resources_used = 0;
    unsigned exclusive_resources_used = 0;
    unsigned resource_groups = 0;
    for (pipeclass->_resUsage.reset();
         (piperesource = (const PipeClassResourceForm *)pipeclass->_resUsage.iter()) != NULL; ) {
      int used_mask = _pipeline->_resdict[piperesource->_resource]->is_resource()->mask();
      if (used_mask)
        resource_groups++;
      resources_used |= used_mask;
      if ((used_mask & (used_mask-1)) == 0)
        exclusive_resources_used |= used_mask;
    }

    if (resource_groups > 0) {
      fprintf(fp_cpp, "static const uint pipeline_res_or_masks_%03d[%d] = {",
        pipeclass->_num, resource_groups);
      for (pipeclass->_resUsage.reset(), i = 1;
           (piperesource = (const PipeClassResourceForm *)pipeclass->_resUsage.iter()) != NULL;
           i++ ) {
        int used_mask = _pipeline->_resdict[piperesource->_resource]->is_resource()->mask();
        if (used_mask) {
          fprintf(fp_cpp, " 0x%0*x%c", resource_count, used_mask, i < (int)resource_groups ? ',' : ' ');
        }
      }
      fprintf(fp_cpp, "};\n\n");
    }
#endif

    // Create the pipeline class description
    fprintf(fp_cpp, "static const Pipeline pipeline_class_%03d(",
      pipeclass->_num);
    if (maxWriteStage < 0)
      fprintf(fp_cpp, "(uint)stage_undefined");
    else if (maxMoreInstrs == 0)
      fprintf(fp_cpp, "(uint)stage_%s", _pipeline->_stages.name(maxWriteStage));
    else
      fprintf(fp_cpp, "((uint)stage_%s)+%d", _pipeline->_stages.name(maxWriteStage), maxMoreInstrs);
    fprintf(fp_cpp, ", %d, %s, %d, %d, %s, %s, %s, %s,\n",
      paramcount,
      pipeclass->hasFixedLatency() ? "true" : "false",
      pipeclass->fixedLatency(),
      pipeclass->InstructionCount(),
      pipeclass->hasBranchDelay() ? "true" : "false",
      pipeclass->hasMultipleBundles() ? "true" : "false",
      pipeclass->forceSerialization() ? "true" : "false",
      pipeclass->mayHaveNoCode() ? "true" : "false" );
    if (paramcount > 0) {
      fprintf(fp_cpp, "\n  (enum machPipelineStages * const) pipeline_reads_%03d,\n ",
        pipeline_reads_index+1);
    }
    else
      fprintf(fp_cpp, " NULL,");
    fprintf(fp_cpp, "  (enum machPipelineStages * const) pipeline_res_stages_%03d,\n",
      pipeline_res_stages_index+1);
    fprintf(fp_cpp, "  (uint * const) pipeline_res_cycles_%03d,\n",
      pipeline_res_cycles_index+1);
    fprintf(fp_cpp, "  Pipeline_Use(%s, (Pipeline_Use_Element *)",
      pipeline_res_args.name(pipeline_res_mask_index));
    if (strlen(pipeline_res_masks.name(pipeline_res_mask_index)) > 0)
      fprintf(fp_cpp, "&pipeline_res_mask_%03d[0]",
        pipeline_res_mask_index+1);
    else
      fprintf(fp_cpp, "NULL");
    fprintf(fp_cpp, "));\n");
  }

  // Generate the Node::latency method if _pipeline defined
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "//------------------Inter-Instruction Latency--------------------------------\n");
  fprintf(fp_cpp, "uint Node::latency(uint i) {\n");
  if (_pipeline) {
#if 0
    fprintf(fp_cpp, "#ifndef PRODUCT\n");
    fprintf(fp_cpp, " if (TraceOptoOutput) {\n");
    fprintf(fp_cpp, "    tty->print(\"# %%4d->latency(%%d)\\n\", _idx, i);\n");
    fprintf(fp_cpp, " }\n");
    fprintf(fp_cpp, "#endif\n");
#endif
    fprintf(fp_cpp, "  uint j;\n");
    fprintf(fp_cpp, "  // verify in legal range for inputs\n");
    fprintf(fp_cpp, "  assert(i < len(), \"index not in range\");\n\n");
    fprintf(fp_cpp, "  // verify input is not null\n");
    fprintf(fp_cpp, "  Node *pred = in(i);\n");
    fprintf(fp_cpp, "  if (!pred)\n    return %d;\n\n",
      non_operand_latency);
    fprintf(fp_cpp, "  if (pred->is_Proj())\n    pred = pred->in(0);\n\n");
    fprintf(fp_cpp, "  // if either node does not have pipeline info, use default\n");
    fprintf(fp_cpp, "  const Pipeline *predpipe = pred->pipeline();\n");
    fprintf(fp_cpp, "  assert(predpipe, \"no predecessor pipeline info\");\n\n");
    fprintf(fp_cpp, "  if (predpipe->hasFixedLatency())\n    return predpipe->fixedLatency();\n\n");
    fprintf(fp_cpp, "  const Pipeline *currpipe = pipeline();\n");
    fprintf(fp_cpp, "  assert(currpipe, \"no pipeline info\");\n\n");
    fprintf(fp_cpp, "  if (!is_Mach())\n    return %d;\n\n",
      node_latency);
    fprintf(fp_cpp, "  const MachNode *m = as_Mach();\n");
    fprintf(fp_cpp, "  j = m->oper_input_base();\n");
    fprintf(fp_cpp, "  if (i < j)\n    return currpipe->functional_unit_latency(%d, predpipe);\n\n",
      non_operand_latency);
    fprintf(fp_cpp, "  // determine which operand this is in\n");
    fprintf(fp_cpp, "  uint n = m->num_opnds();\n");
    fprintf(fp_cpp, "  int delta = %d;\n\n",
      non_operand_latency);
    fprintf(fp_cpp, "  uint k;\n");
    fprintf(fp_cpp, "  for (k = 1; k < n; k++) {\n");
    fprintf(fp_cpp, "    j += m->_opnds[k]->num_edges();\n");
    fprintf(fp_cpp, "    if (i < j)\n");
    fprintf(fp_cpp, "      break;\n");
    fprintf(fp_cpp, "  }\n");
    fprintf(fp_cpp, "  if (k < n)\n");
    fprintf(fp_cpp, "    delta = currpipe->operand_latency(k,predpipe);\n\n");
    fprintf(fp_cpp, "  return currpipe->functional_unit_latency(delta, predpipe);\n");
  }
  else {
    fprintf(fp_cpp, "  // assert(false, \"pipeline functionality is not defined\");\n");
    fprintf(fp_cpp, "  return %d;\n",
      non_operand_latency);
  }
  fprintf(fp_cpp, "}\n\n");

  // Output the list of nop nodes
  fprintf(fp_cpp, "// Descriptions for emitting different functional unit nops\n");
  const char *nop;
  int nopcnt = 0;
  for ( _pipeline->_noplist.reset(); (nop = _pipeline->_noplist.iter()) != NULL; nopcnt++ );

  fprintf(fp_cpp, "void Bundle::initialize_nops(MachNode * nop_list[%d]) {\n", nopcnt);
  int i = 0;
  for ( _pipeline->_noplist.reset(); (nop = _pipeline->_noplist.iter()) != NULL; i++ ) {
    fprintf(fp_cpp, "  nop_list[%d] = (MachNode *) new %sNode();\n", i, nop);
  }
  fprintf(fp_cpp, "};\n\n");
  fprintf(fp_cpp, "#ifndef PRODUCT\n");
  fprintf(fp_cpp, "void Bundle::dump(outputStream *st) const {\n");
  fprintf(fp_cpp, "  static const char * bundle_flags[] = {\n");
  fprintf(fp_cpp, "    \"\",\n");
  fprintf(fp_cpp, "    \"use nop delay\",\n");
  fprintf(fp_cpp, "    \"use unconditional delay\",\n");
  fprintf(fp_cpp, "    \"use conditional delay\",\n");
  fprintf(fp_cpp, "    \"used in conditional delay\",\n");
  fprintf(fp_cpp, "    \"used in unconditional delay\",\n");
  fprintf(fp_cpp, "    \"used in all conditional delays\",\n");
  fprintf(fp_cpp, "  };\n\n");

  fprintf(fp_cpp, "  static const char *resource_names[%d] = {", _pipeline->_rescount);
  for (i = 0; i < _pipeline->_rescount; i++)
    fprintf(fp_cpp, " \"%s\"%c", _pipeline->_reslist.name(i), i < _pipeline->_rescount-1 ? ',' : ' ');
  fprintf(fp_cpp, "};\n\n");

  // See if the same string is in the table
  fprintf(fp_cpp, "  bool needs_comma = false;\n\n");
  fprintf(fp_cpp, "  if (_flags) {\n");
  fprintf(fp_cpp, "    st->print(\"%%s\", bundle_flags[_flags]);\n");
  fprintf(fp_cpp, "    needs_comma = true;\n");
  fprintf(fp_cpp, "  };\n");
  fprintf(fp_cpp, "  if (instr_count()) {\n");
  fprintf(fp_cpp, "    st->print(\"%%s%%d instr%%s\", needs_comma ? \", \" : \"\", instr_count(), instr_count() != 1 ? \"s\" : \"\");\n");
  fprintf(fp_cpp, "    needs_comma = true;\n");
  fprintf(fp_cpp, "  };\n");
  fprintf(fp_cpp, "  uint r = resources_used();\n");
  fprintf(fp_cpp, "  if (r) {\n");
  fprintf(fp_cpp, "    st->print(\"%%sresource%%s:\", needs_comma ? \", \" : \"\", (r & (r-1)) != 0 ? \"s\" : \"\");\n");
  fprintf(fp_cpp, "    for (uint i = 0; i < %d; i++)\n", _pipeline->_rescount);
  fprintf(fp_cpp, "      if ((r & (1 << i)) != 0)\n");
  fprintf(fp_cpp, "        st->print(\" %%s\", resource_names[i]);\n");
  fprintf(fp_cpp, "    needs_comma = true;\n");
  fprintf(fp_cpp, "  };\n");
  fprintf(fp_cpp, "  st->print(\"\\n\");\n");
  fprintf(fp_cpp, "}\n");
  fprintf(fp_cpp, "#endif\n");
}

// ---------------------------------------------------------------------------
//------------------------------Utilities to build Instruction Classes--------
// ---------------------------------------------------------------------------

static void defineOut_RegMask(FILE *fp, const char *node, const char *regMask) {
  fprintf(fp,"const RegMask &%sNode::out_RegMask() const { return (%s); }\n",
          node, regMask);
}

static void print_block_index(FILE *fp, int inst_position) {
  assert( inst_position >= 0, "Instruction number less than zero");
  fprintf(fp, "block_index");
  if( inst_position != 0 ) {
    fprintf(fp, " - %d", inst_position);
  }
}

// Scan the peepmatch and output a test for each instruction
static void check_peepmatch_instruction_sequence(FILE *fp, PeepMatch *pmatch, PeepConstraint *pconstraint) {
  int         parent        = -1;
  int         inst_position = 0;
  const char* inst_name     = NULL;
  int         input         = 0;
  fprintf(fp, "  // Check instruction sub-tree\n");
  pmatch->reset();
  for( pmatch->next_instruction( parent, inst_position, inst_name, input );
       inst_name != NULL;
       pmatch->next_instruction( parent, inst_position, inst_name, input ) ) {
    // If this is not a placeholder
    if( ! pmatch->is_placeholder() ) {
      // Define temporaries 'inst#', based on parent and parent's input index
      if( parent != -1 ) {                // root was initialized
        fprintf(fp, "  // Identify previous instruction if inside this block\n");
        fprintf(fp, "  if( ");
        print_block_index(fp, inst_position);
        fprintf(fp, " > 0 ) {\n    Node *n = block->get_node(");
        print_block_index(fp, inst_position);
        fprintf(fp, ");\n    inst%d = (n->is_Mach()) ? ", inst_position);
        fprintf(fp, "n->as_Mach() : NULL;\n  }\n");
      }

      // When not the root
      // Test we have the correct instruction by comparing the rule.
      if( parent != -1 ) {
        fprintf(fp, "  matches = matches && (inst%d != NULL) && (inst%d->rule() == %s_rule);\n",
                inst_position, inst_position, inst_name);
      }
    } else {
      // Check that user did not try to constrain a placeholder
      assert( ! pconstraint->constrains_instruction(inst_position),
              "fatal(): Can not constrain a placeholder instruction");
    }
  }
}

// Build mapping for register indices, num_edges to input
static void build_instruction_index_mapping( FILE *fp, FormDict &globals, PeepMatch *pmatch ) {
  int         parent        = -1;
  int         inst_position = 0;
  const char* inst_name     = NULL;
  int         input         = 0;
  fprintf(fp, "      // Build map to register info\n");
  pmatch->reset();
  for( pmatch->next_instruction( parent, inst_position, inst_name, input );
       inst_name != NULL;
       pmatch->next_instruction( parent, inst_position, inst_name, input ) ) {
    // If this is not a placeholder
    if( ! pmatch->is_placeholder() ) {
      // Define temporaries 'inst#', based on self's inst_position
      InstructForm *inst = globals[inst_name]->is_instruction();
      if( inst != NULL ) {
        char inst_prefix[]  = "instXXXX_";
        sprintf(inst_prefix, "inst%d_",   inst_position);
        char receiver[]     = "instXXXX->";
        sprintf(receiver,    "inst%d->", inst_position);
        inst->index_temps( fp, globals, inst_prefix, receiver );
      }
    }
  }
}

// Generate tests for the constraints
static void check_peepconstraints(FILE *fp, FormDict &globals, PeepMatch *pmatch, PeepConstraint *pconstraint) {
  fprintf(fp, "\n");
  fprintf(fp, "      // Check constraints on sub-tree-leaves\n");

  // Build mapping from num_edges to local variables
  build_instruction_index_mapping( fp, globals, pmatch );

  // Build constraint tests
  if( pconstraint != NULL ) {
    fprintf(fp, "      matches = matches &&");
    bool   first_constraint = true;
    while( pconstraint != NULL ) {
      // indentation and connecting '&&'
      const char *indentation = "      ";
      fprintf(fp, "\n%s%s", indentation, (!first_constraint ? "&& " : "  "));

      // Only have '==' relation implemented
      if( strcmp(pconstraint->_relation,"==") != 0 ) {
        assert( false, "Unimplemented()" );
      }

      // LEFT
      int left_index       = pconstraint->_left_inst;
      const char *left_op  = pconstraint->_left_op;
      // Access info on the instructions whose operands are compared
      InstructForm *inst_left = globals[pmatch->instruction_name(left_index)]->is_instruction();
      assert( inst_left, "Parser should guaranty this is an instruction");
      int left_op_base  = inst_left->oper_input_base(globals);
      // Access info on the operands being compared
      int left_op_index  = inst_left->operand_position(left_op, Component::USE);
      if( left_op_index == -1 ) {
        left_op_index = inst_left->operand_position(left_op, Component::DEF);
        if( left_op_index == -1 ) {
          left_op_index = inst_left->operand_position(left_op, Component::USE_DEF);
        }
      }
      assert( left_op_index  != NameList::Not_in_list, "Did not find operand in instruction");
      ComponentList components_left = inst_left->_components;
      const char *left_comp_type = components_left.at(left_op_index)->_type;
      OpClassForm *left_opclass = globals[left_comp_type]->is_opclass();
      Form::InterfaceType left_interface_type = left_opclass->interface_type(globals);


      // RIGHT
      int right_op_index = -1;
      int right_index      = pconstraint->_right_inst;
      const char *right_op = pconstraint->_right_op;
      if( right_index != -1 ) { // Match operand
        // Access info on the instructions whose operands are compared
        InstructForm *inst_right = globals[pmatch->instruction_name(right_index)]->is_instruction();
        assert( inst_right, "Parser should guaranty this is an instruction");
        int right_op_base = inst_right->oper_input_base(globals);
        // Access info on the operands being compared
        right_op_index = inst_right->operand_position(right_op, Component::USE);
        if( right_op_index == -1 ) {
          right_op_index = inst_right->operand_position(right_op, Component::DEF);
          if( right_op_index == -1 ) {
            right_op_index = inst_right->operand_position(right_op, Component::USE_DEF);
          }
        }
        assert( right_op_index != NameList::Not_in_list, "Did not find operand in instruction");
        ComponentList components_right = inst_right->_components;
        const char *right_comp_type = components_right.at(right_op_index)->_type;
        OpClassForm *right_opclass = globals[right_comp_type]->is_opclass();
        Form::InterfaceType right_interface_type = right_opclass->interface_type(globals);
        assert( right_interface_type == left_interface_type, "Both must be same interface");

      } else {                  // Else match register
        // assert( false, "should be a register" );
      }

      //
      // Check for equivalence
      //
      // fprintf(fp, "(inst%d->_opnds[%d]->reg(ra_,inst%d%s)  /* %d.%s */ == /* %d.%s */ inst%d->_opnds[%d]->reg(ra_,inst%d%s)",
      //         left_index, left_op_index, left_index, left_reg_index, left_index, left_op
      //         right_index, right_op, right_index, right_op_index, right_index, right_reg_index);
      // fprintf(fp, ")");
      //
      switch( left_interface_type ) {
      case Form::register_interface: {
        // Check that they are allocated to the same register
        // Need parameter for index position if not result operand
        char left_reg_index[] = ",instXXXX_idxXXXX";
        if( left_op_index != 0 ) {
          assert( (left_index <= 9999) && (left_op_index <= 9999), "exceed string size");
          // Must have index into operands
          sprintf(left_reg_index,",inst%d_idx%d", (int)left_index, left_op_index);
        } else {
          strcpy(left_reg_index, "");
        }
        fprintf(fp, "(inst%d->_opnds[%d]->reg(ra_,inst%d%s)  /* %d.%s */",
                left_index,  left_op_index, left_index, left_reg_index, left_index, left_op );
        fprintf(fp, " == ");

        if( right_index != -1 ) {
          char right_reg_index[18] = ",instXXXX_idxXXXX";
          if( right_op_index != 0 ) {
            assert( (right_index <= 9999) && (right_op_index <= 9999), "exceed string size");
            // Must have index into operands
            sprintf(right_reg_index,",inst%d_idx%d", (int)right_index, right_op_index);
          } else {
            strcpy(right_reg_index, "");
          }
          fprintf(fp, "/* %d.%s */ inst%d->_opnds[%d]->reg(ra_,inst%d%s)",
                  right_index, right_op, right_index, right_op_index, right_index, right_reg_index );
        } else {
          fprintf(fp, "%s_enc", right_op );
        }
        fprintf(fp,")");
        break;
      }
      case Form::constant_interface: {
        // Compare the '->constant()' values
        fprintf(fp, "(inst%d->_opnds[%d]->constant()  /* %d.%s */",
                left_index,  left_op_index,  left_index, left_op );
        fprintf(fp, " == ");
        fprintf(fp, "/* %d.%s */ inst%d->_opnds[%d]->constant())",
                right_index, right_op, right_index, right_op_index );
        break;
      }
      case Form::memory_interface: {
        // Compare 'base', 'index', 'scale', and 'disp'
        // base
        fprintf(fp, "( \n");
        fprintf(fp, "  (inst%d->_opnds[%d]->base(ra_,inst%d,inst%d_idx%d)  /* %d.%s$$base */",
          left_index, left_op_index, left_index, left_index, left_op_index, left_index, left_op );
        fprintf(fp, " == ");
        fprintf(fp, "/* %d.%s$$base */ inst%d->_opnds[%d]->base(ra_,inst%d,inst%d_idx%d)) &&\n",
                right_index, right_op, right_index, right_op_index, right_index, right_index, right_op_index );
        // index
        fprintf(fp, "  (inst%d->_opnds[%d]->index(ra_,inst%d,inst%d_idx%d)  /* %d.%s$$index */",
                left_index, left_op_index, left_index, left_index, left_op_index, left_index, left_op );
        fprintf(fp, " == ");
        fprintf(fp, "/* %d.%s$$index */ inst%d->_opnds[%d]->index(ra_,inst%d,inst%d_idx%d)) &&\n",
                right_index, right_op, right_index, right_op_index, right_index, right_index, right_op_index );
        // scale
        fprintf(fp, "  (inst%d->_opnds[%d]->scale()  /* %d.%s$$scale */",
                left_index,  left_op_index,  left_index, left_op );
        fprintf(fp, " == ");
        fprintf(fp, "/* %d.%s$$scale */ inst%d->_opnds[%d]->scale()) &&\n",
                right_index, right_op, right_index, right_op_index );
        // disp
        fprintf(fp, "  (inst%d->_opnds[%d]->disp(ra_,inst%d,inst%d_idx%d)  /* %d.%s$$disp */",
                left_index, left_op_index, left_index, left_index, left_op_index, left_index, left_op );
        fprintf(fp, " == ");
        fprintf(fp, "/* %d.%s$$disp */ inst%d->_opnds[%d]->disp(ra_,inst%d,inst%d_idx%d))\n",
                right_index, right_op, right_index, right_op_index, right_index, right_index, right_op_index );
        fprintf(fp, ") \n");
        break;
      }
      case Form::conditional_interface: {
        // Compare the condition code being tested
        assert( false, "Unimplemented()" );
        break;
      }
      default: {
        assert( false, "ShouldNotReachHere()" );
        break;
      }
      }

      // Advance to next constraint
      pconstraint = pconstraint->next();
      first_constraint = false;
    }

    fprintf(fp, ";\n");
  }
}

// // EXPERIMENTAL -- TEMPORARY code
// static Form::DataType get_operand_type(FormDict &globals, InstructForm *instr, const char *op_name ) {
//   int op_index = instr->operand_position(op_name, Component::USE);
//   if( op_index == -1 ) {
//     op_index = instr->operand_position(op_name, Component::DEF);
//     if( op_index == -1 ) {
//       op_index = instr->operand_position(op_name, Component::USE_DEF);
//     }
//   }
//   assert( op_index != NameList::Not_in_list, "Did not find operand in instruction");
//
//   ComponentList components_right = instr->_components;
//   char *right_comp_type = components_right.at(op_index)->_type;
//   OpClassForm *right_opclass = globals[right_comp_type]->is_opclass();
//   Form::InterfaceType  right_interface_type = right_opclass->interface_type(globals);
//
//   return;
// }

// Construct the new sub-tree
static void generate_peepreplace( FILE *fp, FormDict &globals, PeepMatch *pmatch, PeepConstraint *pconstraint, PeepReplace *preplace, int max_position ) {
  fprintf(fp, "      // IF instructions and constraints matched\n");
  fprintf(fp, "      if( matches ) {\n");
  fprintf(fp, "        // generate the new sub-tree\n");
  fprintf(fp, "        assert( true, \"Debug stopping point\");\n");
  if( preplace != NULL ) {
    // Get the root of the new sub-tree
    const char *root_inst = NULL;
    preplace->next_instruction(root_inst);
    InstructForm *root_form = globals[root_inst]->is_instruction();
    assert( root_form != NULL, "Replacement instruction was not previously defined");
    fprintf(fp, "        %sNode *root = new %sNode();\n", root_inst, root_inst);

    int         inst_num;
    const char *op_name;
    int         opnds_index = 0;            // define result operand
    // Then install the use-operands for the new sub-tree
    // preplace->reset();             // reset breaks iteration
    for( preplace->next_operand( inst_num, op_name );
         op_name != NULL;
         preplace->next_operand( inst_num, op_name ) ) {
      InstructForm *inst_form;
      inst_form  = globals[pmatch->instruction_name(inst_num)]->is_instruction();
      assert( inst_form, "Parser should guaranty this is an instruction");
      int inst_op_num = inst_form->operand_position(op_name, Component::USE);
      if( inst_op_num == NameList::Not_in_list )
        inst_op_num = inst_form->operand_position(op_name, Component::USE_DEF);
      assert( inst_op_num != NameList::Not_in_list, "Did not find operand as USE");
      // find the name of the OperandForm from the local name
      const Form *form   = inst_form->_localNames[op_name];
      OperandForm  *op_form = form->is_operand();
      if( opnds_index == 0 ) {
        // Initial setup of new instruction
        fprintf(fp, "        // ----- Initial setup -----\n");
        //
        // Add control edge for this node
        fprintf(fp, "        root->add_req(_in[0]);                // control edge\n");
        // Add unmatched edges from root of match tree
        int op_base = root_form->oper_input_base(globals);
        for( int unmatched_edge = 1; unmatched_edge < op_base; ++unmatched_edge ) {
          fprintf(fp, "        root->add_req(inst%d->in(%d));        // unmatched ideal edge\n",
                                          inst_num, unmatched_edge);
        }
        // If new instruction captures bottom type
        if( root_form->captures_bottom_type(globals) ) {
          // Get bottom type from instruction whose result we are replacing
          fprintf(fp, "        root->_bottom_type = inst%d->bottom_type();\n", inst_num);
        }
        // Define result register and result operand
        fprintf(fp, "        ra_->add_reference(root, inst%d);\n", inst_num);
        fprintf(fp, "        ra_->set_oop (root, ra_->is_oop(inst%d));\n", inst_num);
        fprintf(fp, "        ra_->set_pair(root->_idx, ra_->get_reg_second(inst%d), ra_->get_reg_first(inst%d));\n", inst_num, inst_num);
        fprintf(fp, "        root->_opnds[0] = inst%d->_opnds[0]->clone(); // result\n", inst_num);
        fprintf(fp, "        // ----- Done with initial setup -----\n");
      } else {
        if( (op_form == NULL) || (op_form->is_base_constant(globals) == Form::none) ) {
          // Do not have ideal edges for constants after matching
          fprintf(fp, "        for( unsigned x%d = inst%d_idx%d; x%d < inst%d_idx%d; x%d++ )\n",
                  inst_op_num, inst_num, inst_op_num,
                  inst_op_num, inst_num, inst_op_num+1, inst_op_num );
          fprintf(fp, "          root->add_req( inst%d->in(x%d) );\n",
                  inst_num, inst_op_num );
        } else {
          fprintf(fp, "        // no ideal edge for constants after matching\n");
        }
        fprintf(fp, "        root->_opnds[%d] = inst%d->_opnds[%d]->clone();\n",
                opnds_index, inst_num, inst_op_num );
      }
      ++opnds_index;
    }
  }else {
    // Replacing subtree with empty-tree
    assert( false, "ShouldNotReachHere();");
  }

  for (int i = 0; i <= max_position; i++) {
    fprintf(fp, "        inst%d->set_removed();\n", i);
  }
  // Return the new sub-tree
  fprintf(fp, "        deleted = %d;\n", max_position+1 /*zero to one based*/);
  fprintf(fp, "        return root;  // return new root;\n");
  fprintf(fp, "      }\n");
}


// Define the Peephole method for an instruction node
void ArchDesc::definePeephole(FILE *fp, InstructForm *node) {
  // Generate Peephole function header
  fprintf(fp, "MachNode *%sNode::peephole(Block *block, int block_index, PhaseRegAlloc *ra_, int &deleted) {\n", node->_ident);
  fprintf(fp, "  bool  matches = true;\n");

  // Identify the maximum instruction position,
  // generate temporaries that hold current instruction
  //
  //   MachNode  *inst0 = NULL;
  //   ...
  //   MachNode  *instMAX = NULL;
  //
  int max_position = 0;
  Peephole *peep;
  for( peep = node->peepholes(); peep != NULL; peep = peep->next() ) {
    PeepMatch *pmatch = peep->match();
    assert( pmatch != NULL, "fatal(), missing peepmatch rule");
    if( max_position < pmatch->max_position() )  max_position = pmatch->max_position();
  }
  for( int i = 0; i <= max_position; ++i ) {
    if( i == 0 ) {
      fprintf(fp, "  MachNode *inst0 = this;\n");
    } else {
      fprintf(fp, "  MachNode *inst%d = NULL;\n", i);
    }
  }

  // For each peephole rule in architecture description
  //   Construct a test for the desired instruction sub-tree
  //   then check the constraints
  //   If these match, Generate the new subtree
  for( peep = node->peepholes(); peep != NULL; peep = peep->next() ) {
    int         peephole_number = peep->peephole_number();
    PeepMatch      *pmatch      = peep->match();
    PeepConstraint *pconstraint = peep->constraints();
    PeepReplace    *preplace    = peep->replacement();

    // Root of this peephole is the current MachNode
    assert( true, // %%name?%% strcmp( node->_ident, pmatch->name(0) ) == 0,
            "root of PeepMatch does not match instruction");

    // Make each peephole rule individually selectable
    fprintf(fp, "  if( (OptoPeepholeAt == -1) || (OptoPeepholeAt==%d) ) {\n", peephole_number);
    fprintf(fp, "    matches = true;\n");
    // Scan the peepmatch and output a test for each instruction
    check_peepmatch_instruction_sequence( fp, pmatch, pconstraint );

    // Check constraints and build replacement inside scope
    fprintf(fp, "    // If instruction subtree matches\n");
    fprintf(fp, "    if( matches ) {\n");

    // Generate tests for the constraints
    check_peepconstraints( fp, _globalNames, pmatch, pconstraint );

    // Construct the new sub-tree
    generate_peepreplace( fp, _globalNames, pmatch, pconstraint, preplace, max_position );

    // End of scope for this peephole's constraints
    fprintf(fp, "    }\n");
    // Closing brace '}' to make each peephole rule individually selectable
    fprintf(fp, "  } // end of peephole rule #%d\n", peephole_number);
    fprintf(fp, "\n");
  }

  fprintf(fp, "  return NULL;  // No peephole rules matched\n");
  fprintf(fp, "}\n");
  fprintf(fp, "\n");
}

// Define the Expand method for an instruction node
void ArchDesc::defineExpand(FILE *fp, InstructForm *node) {
  unsigned      cnt  = 0;          // Count nodes we have expand into
  unsigned      i;

  // Generate Expand function header
  fprintf(fp, "MachNode* %sNode::Expand(State* state, Node_List& proj_list, Node* mem) {\n", node->_ident);
  fprintf(fp, "  Compile* C = Compile::current();\n");
  // Generate expand code
  if( node->expands() ) {
    const char   *opid;
    int           new_pos, exp_pos;
    const char   *new_id   = NULL;
    const Form   *frm      = NULL;
    InstructForm *new_inst = NULL;
    OperandForm  *new_oper = NULL;
    unsigned      numo     = node->num_opnds() +
                                node->_exprule->_newopers.count();

    // If necessary, generate any operands created in expand rule
    if (node->_exprule->_newopers.count()) {
      for(node->_exprule->_newopers.reset();
          (new_id = node->_exprule->_newopers.iter()) != NULL; cnt++) {
        frm = node->_localNames[new_id];
        assert(frm, "Invalid entry in new operands list of expand rule");
        new_oper = frm->is_operand();
        char *tmp = (char *)node->_exprule->_newopconst[new_id];
        if (tmp == NULL) {
          fprintf(fp,"  MachOper *op%d = new %sOper();\n",
                  cnt, new_oper->_ident);
        }
        else {
          fprintf(fp,"  MachOper *op%d = new %sOper(%s);\n",
                  cnt, new_oper->_ident, tmp);
        }
      }
    }
    cnt = 0;
    // Generate the temps to use for DAG building
    for(i = 0; i < numo; i++) {
      if (i < node->num_opnds()) {
        fprintf(fp,"  MachNode *tmp%d = this;\n", i);
      }
      else {
        fprintf(fp,"  MachNode *tmp%d = NULL;\n", i);
      }
    }
    // Build mapping from num_edges to local variables
    fprintf(fp,"  unsigned num0 = 0;\n");
    for( i = 1; i < node->num_opnds(); i++ ) {
      fprintf(fp,"  unsigned num%d = opnd_array(%d)->num_edges();\n",i,i);
    }

    // Build a mapping from operand index to input edges
    fprintf(fp,"  unsigned idx0 = oper_input_base();\n");

    // The order in which the memory input is added to a node is very
    // strange.  Store nodes get a memory input before Expand is
    // called and other nodes get it afterwards or before depending on
    // match order so oper_input_base is wrong during expansion.  This
    // code adjusts it so that expansion will work correctly.
    int has_memory_edge = node->_matrule->needs_ideal_memory_edge(_globalNames);
    if (has_memory_edge) {
      fprintf(fp,"  if (mem == (Node*)1) {\n");
      fprintf(fp,"    idx0--; // Adjust base because memory edge hasn't been inserted yet\n");
      fprintf(fp,"  }\n");
    }

    for( i = 0; i < node->num_opnds(); i++ ) {
      fprintf(fp,"  unsigned idx%d = idx%d + num%d;\n",
              i+1,i,i);
    }

    // Declare variable to hold root of expansion
    fprintf(fp,"  MachNode *result = NULL;\n");

    // Iterate over the instructions 'node' expands into
    ExpandRule  *expand       = node->_exprule;
    NameAndList *expand_instr = NULL;
    for (expand->reset_instructions();
         (expand_instr = expand->iter_instructions()) != NULL; cnt++) {
      new_id = expand_instr->name();

      InstructForm* expand_instruction = (InstructForm*)globalAD->globalNames()[new_id];

      if (!expand_instruction) {
        globalAD->syntax_err(node->_linenum, "In %s: instruction %s used in expand not declared\n",
                             node->_ident, new_id);
        continue;
      }

      // Build the node for the instruction
      fprintf(fp,"\n  %sNode *n%d = new %sNode();\n", new_id, cnt, new_id);
      // Add control edge for this node
      fprintf(fp,"  n%d->add_req(_in[0]);\n", cnt);
      // Build the operand for the value this node defines.
      Form *form = (Form*)_globalNames[new_id];
      assert(form, "'new_id' must be a defined form name");
      // Grab the InstructForm for the new instruction
      new_inst = form->is_instruction();
      assert(new_inst, "'new_id' must be an instruction name");
      if (node->is_ideal_if() && new_inst->is_ideal_if()) {
        fprintf(fp, "  ((MachIfNode*)n%d)->_prob = _prob;\n", cnt);
        fprintf(fp, "  ((MachIfNode*)n%d)->_fcnt = _fcnt;\n", cnt);
      }

      if (node->is_ideal_fastlock() && new_inst->is_ideal_fastlock()) {
        fprintf(fp, "  ((MachFastLockNode*)n%d)->_rtm_counters = _rtm_counters;\n", cnt);
        fprintf(fp, "  ((MachFastLockNode*)n%d)->_stack_rtm_counters = _stack_rtm_counters;\n", cnt);
      }

      // Fill in the bottom_type where requested
      if (node->captures_bottom_type(_globalNames) &&
          new_inst->captures_bottom_type(_globalNames)) {
        fprintf(fp, "  ((MachTypeNode*)n%d)->_bottom_type = bottom_type();\n", cnt);
      }

      const char *resultOper = new_inst->reduce_result();
      fprintf(fp,"  n%d->set_opnd_array(0, state->MachOperGenerator(%s));\n",
              cnt, machOperEnum(resultOper));

      // get the formal operand NameList
      NameList *formal_lst = &new_inst->_parameters;
      formal_lst->reset();

      // Handle any memory operand
      int memory_operand = new_inst->memory_operand(_globalNames);
      if( memory_operand != InstructForm::NO_MEMORY_OPERAND ) {
        int node_mem_op = node->memory_operand(_globalNames);
        assert( node_mem_op != InstructForm::NO_MEMORY_OPERAND,
                "expand rule member needs memory but top-level inst doesn't have any" );
        if (has_memory_edge) {
          // Copy memory edge
          fprintf(fp,"  if (mem != (Node*)1) {\n");
          fprintf(fp,"    n%d->add_req(_in[1]);\t// Add memory edge\n", cnt);
          fprintf(fp,"  }\n");
        }
      }

      // Iterate over the new instruction's operands
      int prev_pos = -1;
      for( expand_instr->reset(); (opid = expand_instr->iter()) != NULL; ) {
        // Use 'parameter' at current position in list of new instruction's formals
        // instead of 'opid' when looking up info internal to new_inst
        const char *parameter = formal_lst->iter();
        if (!parameter) {
          globalAD->syntax_err(node->_linenum, "Operand %s of expand instruction %s has"
                               " no equivalent in new instruction %s.",
                               opid, node->_ident, new_inst->_ident);
          assert(0, "Wrong expand");
        }

        // Check for an operand which is created in the expand rule
        if ((exp_pos = node->_exprule->_newopers.index(opid)) != -1) {
          new_pos = new_inst->operand_position(parameter,Component::USE);
          exp_pos += node->num_opnds();
          // If there is no use of the created operand, just skip it
          if (new_pos != NameList::Not_in_list) {
            //Copy the operand from the original made above
            fprintf(fp,"  n%d->set_opnd_array(%d, op%d->clone()); // %s\n",
                    cnt, new_pos, exp_pos-node->num_opnds(), opid);
            // Check for who defines this operand & add edge if needed
            fprintf(fp,"  if(tmp%d != NULL)\n", exp_pos);
            fprintf(fp,"    n%d->add_req(tmp%d);\n", cnt, exp_pos);
          }
        }
        else {
          // Use operand name to get an index into instruction component list
          // ins = (InstructForm *) _globalNames[new_id];
          exp_pos = node->operand_position_format(opid);
          assert(exp_pos != -1, "Bad expand rule");
          if (prev_pos > exp_pos && expand_instruction->_matrule != NULL) {
            // For the add_req calls below to work correctly they need
            // to added in the same order that a match would add them.
            // This means that they would need to be in the order of
            // the components list instead of the formal parameters.
            // This is a sort of hidden invariant that previously
            // wasn't checked and could lead to incorrectly
            // constructed nodes.
            syntax_err(node->_linenum, "For expand in %s to work, parameter declaration order in %s must follow matchrule\n",
                       node->_ident, new_inst->_ident);
          }
          prev_pos = exp_pos;

          new_pos = new_inst->operand_position(parameter,Component::USE);
          if (new_pos != -1) {
            // Copy the operand from the ExpandNode to the new node
            fprintf(fp,"  n%d->set_opnd_array(%d, opnd_array(%d)->clone()); // %s\n",
                    cnt, new_pos, exp_pos, opid);
            // For each operand add appropriate input edges by looking at tmp's
            fprintf(fp,"  if(tmp%d == this) {\n", exp_pos);
            // Grab corresponding edges from ExpandNode and insert them here
            fprintf(fp,"    for(unsigned i = 0; i < num%d; i++) {\n", exp_pos);
            fprintf(fp,"      n%d->add_req(_in[i + idx%d]);\n", cnt, exp_pos);
            fprintf(fp,"    }\n");
            fprintf(fp,"  }\n");
            // This value is generated by one of the new instructions
            fprintf(fp,"  else n%d->add_req(tmp%d);\n", cnt, exp_pos);
          }
        }

        // Update the DAG tmp's for values defined by this instruction
        int new_def_pos = new_inst->operand_position(parameter,Component::DEF);
        Effect *eform = (Effect *)new_inst->_effects[parameter];
        // If this operand is a definition in either an effects rule
        // or a match rule
        if((eform) && (is_def(eform->_use_def))) {
          // Update the temp associated with this operand
          fprintf(fp,"  tmp%d = n%d;\n", exp_pos, cnt);
        }
        else if( new_def_pos != -1 ) {
          // Instruction defines a value but user did not declare it
          // in the 'effect' clause
          fprintf(fp,"  tmp%d = n%d;\n", exp_pos, cnt);
        }
      } // done iterating over a new instruction's operands

      // Fix number of operands, as we do not generate redundant ones.
      // The matcher generates some redundant operands, which are removed
      // in the expand function (of the node we generate here). We don't
      // generate the redundant operands here, so set the correct _num_opnds.
      if (expand_instruction->num_opnds() != expand_instruction->num_unique_opnds()) {
        fprintf(fp, "  n%d->_num_opnds = %d; // Only unique opnds generated.\n",
                cnt, expand_instruction->num_unique_opnds());
      }

      // Invoke Expand() for the newly created instruction.
      fprintf(fp,"  result = n%d->Expand( state, proj_list, mem );\n", cnt);
      assert( !new_inst->expands(), "Do not have complete support for recursive expansion");
    } // done iterating over new instructions
    fprintf(fp,"\n");
  } // done generating expand rule

  // Generate projections for instruction's additional DEFs and KILLs
  if( ! node->expands() && (node->needs_projections() || node->has_temps())) {
    // Get string representing the MachNode that projections point at
    const char *machNode = "this";
    // Generate the projections
    fprintf(fp,"  // Add projection edges for additional defs or kills\n");

    // Examine each component to see if it is a DEF or KILL
    node->_components.reset();
    // Skip the first component, if already handled as (SET dst (...))
    Component *comp = NULL;
    // For kills, the choice of projection numbers is arbitrary
    int proj_no = 1;
    bool declared_def  = false;
    bool declared_kill = false;

    while ((comp = node->_components.iter()) != NULL) {
      // Lookup register class associated with operand type
      Form *form = (Form*)_globalNames[comp->_type];
      assert(form, "component type must be a defined form");
      OperandForm *op = form->is_operand();

      if (comp->is(Component::TEMP) ||
          comp->is(Component::TEMP_DEF)) {
        fprintf(fp, "  // TEMP %s\n", comp->_name);
        if (!declared_def) {
          // Define the variable "def" to hold new MachProjNodes
          fprintf(fp, "  MachTempNode *def;\n");
          declared_def = true;
        }
        if (op && op->_interface && op->_interface->is_RegInterface()) {
          fprintf(fp,"  def = new MachTempNode(state->MachOperGenerator(%s));\n",
                  machOperEnum(op->_ident));
          fprintf(fp,"  add_req(def);\n");
          // The operand for TEMP is already constructed during
          // this mach node construction, see buildMachNode().
          //
          // int idx  = node->operand_position_format(comp->_name);
          // fprintf(fp,"  set_opnd_array(%d, state->MachOperGenerator(%s));\n",
          //         idx, machOperEnum(op->_ident));
        } else {
          assert(false, "can't have temps which aren't registers");
        }
      } else if (comp->isa(Component::KILL)) {
        fprintf(fp, "  // DEF/KILL %s\n", comp->_name);

        if (!declared_kill) {
          // Define the variable "kill" to hold new MachProjNodes
          fprintf(fp, "  MachProjNode *kill;\n");
          declared_kill = true;
        }

        assert(op, "Support additional KILLS for base operands");
        const char *regmask    = reg_mask(*op);
        const char *ideal_type = op->ideal_type(_globalNames, _register);

        if (!op->is_bound_register()) {
          syntax_err(node->_linenum, "In %s only bound registers can be killed: %s %s\n",
                     node->_ident, comp->_type, comp->_name);
        }

        fprintf(fp,"  kill = ");
        fprintf(fp,"new MachProjNode( %s, %d, (%s), Op_%s );\n",
                machNode, proj_no++, regmask, ideal_type);
        fprintf(fp,"  proj_list.push(kill);\n");
      }
    }
  }

  if( !node->expands() && node->_matrule != NULL ) {
    // Remove duplicated operands and inputs which use the same name.
    // Search through match operands for the same name usage.
    // The matcher generates these non-unique operands. If the node
    // was constructed by an expand rule, there are no unique operands.
    uint cur_num_opnds = node->num_opnds();
    if (cur_num_opnds > 1 && cur_num_opnds != node->num_unique_opnds()) {
      Component *comp = NULL;
      fprintf(fp, "  // Remove duplicated operands and inputs which use the same name.\n");
      fprintf(fp, "  if (num_opnds() == %d) {\n", cur_num_opnds);
      // Build mapping from num_edges to local variables
      fprintf(fp,"    unsigned num0 = 0;\n");
      for (i = 1; i < cur_num_opnds; i++) {
        fprintf(fp,"    unsigned num%d = opnd_array(%d)->num_edges();", i, i);
        fprintf(fp, " \t// %s\n", node->opnd_ident(i));
      }
      // Build a mapping from operand index to input edges
      fprintf(fp,"    unsigned idx0 = oper_input_base();\n");
      for (i = 0; i < cur_num_opnds; i++) {
        fprintf(fp,"    unsigned idx%d = idx%d + num%d;\n", i+1, i, i);
      }

      uint new_num_opnds = 1;
      node->_components.reset();
      // Skip first unique operands.
      for (i = 1; i < cur_num_opnds; i++) {
        comp = node->_components.iter();
        if (i != node->unique_opnds_idx(i)) {
          break;
        }
        new_num_opnds++;
      }
      // Replace not unique operands with next unique operands.
      for ( ; i < cur_num_opnds; i++) {
        comp = node->_components.iter();
        uint j = node->unique_opnds_idx(i);
        // unique_opnds_idx(i) is unique if unique_opnds_idx(j) is not unique.
        if (j != node->unique_opnds_idx(j)) {
          fprintf(fp,"    set_opnd_array(%d, opnd_array(%d)->clone()); // %s\n",
                  new_num_opnds, i, comp->_name);
          // Delete not unique edges here.
          fprintf(fp,"    for (unsigned i = 0; i < num%d; i++) {\n", i);
          fprintf(fp,"      set_req(i + idx%d, _in[i + idx%d]);\n", new_num_opnds, i);
          fprintf(fp,"    }\n");
          fprintf(fp,"    num%d = num%d;\n", new_num_opnds, i);
          fprintf(fp,"    idx%d = idx%d + num%d;\n", new_num_opnds+1, new_num_opnds, new_num_opnds);
          new_num_opnds++;
        }
      }
      // Delete the rest of edges.
      fprintf(fp,"    for (int i = idx%d - 1; i >= (int)idx%d; i--) {\n", cur_num_opnds, new_num_opnds);
      fprintf(fp,"      del_req(i);\n");
      fprintf(fp,"    }\n");
      fprintf(fp,"    _num_opnds = %d;\n", new_num_opnds);
      assert(new_num_opnds == node->num_unique_opnds(), "what?");
      fprintf(fp, "  } else {\n");
      fprintf(fp, "    assert(_num_opnds == %d, \"There should be either %d or %d operands.\");\n",
                  new_num_opnds, new_num_opnds, cur_num_opnds);
      fprintf(fp, "  }\n");
    }
  }

  // If the node is a MachConstantNode, insert the MachConstantBaseNode edge.
  // NOTE: this edge must be the last input (see MachConstantNode::mach_constant_base_node_input).
  // There are nodes that don't use $constantablebase, but still require that it
  // is an input to the node. Example: divF_reg_immN, Repl32B_imm on x86_64.
  if (node->is_mach_constant() || node->needs_constant_base()) {
    if (node->is_ideal_call() != Form::invalid_type &&
        node->is_ideal_call() != Form::JAVA_LEAF) {
      fprintf(fp, "  // MachConstantBaseNode added in matcher.\n");
      _needs_deep_clone_jvms = true;
    } else {
      fprintf(fp, "  add_req(C->mach_constant_base_node());\n");
    }
  }

  fprintf(fp, "\n");
  if (node->expands()) {
    fprintf(fp, "  return result;\n");
  } else {
    fprintf(fp, "  return this;\n");
  }
  fprintf(fp, "}\n");
  fprintf(fp, "\n");
}


//------------------------------Emit Routines----------------------------------
// Special classes and routines for defining node emit routines which output
// target specific instruction object encodings.
// Define the ___Node::emit() routine
//
// (1) void  ___Node::emit(CodeBuffer &cbuf, PhaseRegAlloc *ra_) const {
// (2)   // ...  encoding defined by user
// (3)
// (4) }
//

class DefineEmitState {
private:
  enum reloc_format { RELOC_NONE        = -1,
                      RELOC_IMMEDIATE   =  0,
                      RELOC_DISP        =  1,
                      RELOC_CALL_DISP   =  2 };
  enum literal_status{ LITERAL_NOT_SEEN  = 0,
                       LITERAL_SEEN      = 1,
                       LITERAL_ACCESSED  = 2,
                       LITERAL_OUTPUT    = 3 };
  // Temporaries that describe current operand
  bool          _cleared;
  OpClassForm  *_opclass;
  OperandForm  *_operand;
  int           _operand_idx;
  const char   *_local_name;
  const char   *_operand_name;
  bool          _doing_disp;
  bool          _doing_constant;
  Form::DataType _constant_type;
  DefineEmitState::literal_status _constant_status;
  DefineEmitState::literal_status _reg_status;
  bool          _doing_emit8;
  bool          _doing_emit_d32;
  bool          _doing_emit_d16;
  bool          _doing_emit_hi;
  bool          _doing_emit_lo;
  bool          _may_reloc;
  reloc_format  _reloc_form;
  const char *  _reloc_type;
  bool          _processing_noninput;

  NameList      _strings_to_emit;

  // Stable state, set by constructor
  ArchDesc     &_AD;
  FILE         *_fp;
  EncClass     &_encoding;
  InsEncode    &_ins_encode;
  InstructForm &_inst;

public:
  DefineEmitState(FILE *fp, ArchDesc &AD, EncClass &encoding,
                  InsEncode &ins_encode, InstructForm &inst)
    : _AD(AD), _fp(fp), _encoding(encoding), _ins_encode(ins_encode), _inst(inst) {
      clear();
  }

  void clear() {
    _cleared       = true;
    _opclass       = NULL;
    _operand       = NULL;
    _operand_idx   = 0;
    _local_name    = "";
    _operand_name  = "";
    _doing_disp    = false;
    _doing_constant= false;
    _constant_type = Form::none;
    _constant_status = LITERAL_NOT_SEEN;
    _reg_status      = LITERAL_NOT_SEEN;
    _doing_emit8   = false;
    _doing_emit_d32= false;
    _doing_emit_d16= false;
    _doing_emit_hi = false;
    _doing_emit_lo = false;
    _may_reloc     = false;
    _reloc_form    = RELOC_NONE;
    _reloc_type    = AdlcVMDeps::none_reloc_type();
    _strings_to_emit.clear();
  }

  // Track necessary state when identifying a replacement variable
  // @arg rep_var: The formal parameter of the encoding.
  void update_state(const char *rep_var) {
    // A replacement variable or one of its subfields
    // Obtain replacement variable from list
    if ( (*rep_var) != '$' ) {
      // A replacement variable, '$' prefix
      // check_rep_var( rep_var );
      if ( Opcode::as_opcode_type(rep_var) != Opcode::NOT_AN_OPCODE ) {
        // No state needed.
        assert( _opclass == NULL,
                "'primary', 'secondary' and 'tertiary' don't follow operand.");
      }
      else if ((strcmp(rep_var, "constanttablebase") == 0) ||
               (strcmp(rep_var, "constantoffset")    == 0) ||
               (strcmp(rep_var, "constantaddress")   == 0)) {
        if (!(_inst.is_mach_constant() || _inst.needs_constant_base())) {
          _AD.syntax_err(_encoding._linenum,
                         "Replacement variable %s not allowed in instruct %s (only in MachConstantNode or MachCall).\n",
                         rep_var, _encoding._name);
        }
      }
      else {
        // Lookup its position in (formal) parameter list of encoding
        int   param_no  = _encoding.rep_var_index(rep_var);
        if ( param_no == -1 ) {
          _AD.syntax_err( _encoding._linenum,
                          "Replacement variable %s not found in enc_class %s.\n",
                          rep_var, _encoding._name);
        }

        // Lookup the corresponding ins_encode parameter
        // This is the argument (actual parameter) to the encoding.
        const char *inst_rep_var = _ins_encode.rep_var_name(_inst, param_no);
        if (inst_rep_var == NULL) {
          _AD.syntax_err( _ins_encode._linenum,
                          "Parameter %s not passed to enc_class %s from instruct %s.\n",
                          rep_var, _encoding._name, _inst._ident);
          assert(false, "inst_rep_var == NULL, cannot continue.");
        }

        // Check if instruction's actual parameter is a local name in the instruction
        const Form  *local     = _inst._localNames[inst_rep_var];
        OpClassForm *opc       = (local != NULL) ? local->is_opclass() : NULL;
        // Note: assert removed to allow constant and symbolic parameters
        // assert( opc, "replacement variable was not found in local names");
        // Lookup the index position iff the replacement variable is a localName
        int idx  = (opc != NULL) ? _inst.operand_position_format(inst_rep_var) : -1;

        if ( idx != -1 ) {
          // This is a local in the instruction
          // Update local state info.
          _opclass        = opc;
          _operand_idx    = idx;
          _local_name     = rep_var;
          _operand_name   = inst_rep_var;

          // !!!!!
          // Do not support consecutive operands.
          assert( _operand == NULL, "Unimplemented()");
          _operand = opc->is_operand();
        }
        else if( ADLParser::is_literal_constant(inst_rep_var) ) {
          // Instruction provided a constant expression
          // Check later that encoding specifies $$$constant to resolve as constant
          _constant_status   = LITERAL_SEEN;
        }
        else if( Opcode::as_opcode_type(inst_rep_var) != Opcode::NOT_AN_OPCODE ) {
          // Instruction provided an opcode: "primary", "secondary", "tertiary"
          // Check later that encoding specifies $$$constant to resolve as constant
          _constant_status   = LITERAL_SEEN;
        }
        else if((_AD.get_registers() != NULL ) && (_AD.get_registers()->getRegDef(inst_rep_var) != NULL)) {
          // Instruction provided a literal register name for this parameter
          // Check that encoding specifies $$$reg to resolve.as register.
          _reg_status        = LITERAL_SEEN;
        }
        else {
          // Check for unimplemented functionality before hard failure
          assert(opc != NULL && strcmp(opc->_ident, "label") == 0, "Unimplemented Label");
          assert(false, "ShouldNotReachHere()");
        }
      } // done checking which operand this is.
    } else {
      //
      // A subfield variable, '$$' prefix
      // Check for fields that may require relocation information.
      // Then check that literal register parameters are accessed with 'reg' or 'constant'
      //
      if ( strcmp(rep_var,"$disp") == 0 ) {
        _doing_disp = true;
        assert( _opclass, "Must use operand or operand class before '$disp'");
        if( _operand == NULL ) {
          // Only have an operand class, generate run-time check for relocation
          _may_reloc    = true;
          _reloc_form   = RELOC_DISP;
          _reloc_type   = AdlcVMDeps::oop_reloc_type();
        } else {
          // Do precise check on operand: is it a ConP or not
          //
          // Check interface for value of displacement
          assert( ( _operand->_interface != NULL ),
                  "$disp can only follow memory interface operand");
          MemInterface *mem_interface= _operand->_interface->is_MemInterface();
          assert( mem_interface != NULL,
                  "$disp can only follow memory interface operand");
          const char *disp = mem_interface->_disp;

          if( disp != NULL && (*disp == '$') ) {
            // MemInterface::disp contains a replacement variable,
            // Check if this matches a ConP
            //
            // Lookup replacement variable, in operand's component list
            const char *rep_var_name = disp + 1; // Skip '$'
            const Component *comp = _operand->_components.search(rep_var_name);
            assert( comp != NULL,"Replacement variable not found in components");
            const char      *type = comp->_type;
            // Lookup operand form for replacement variable's type
            const Form *form = _AD.globalNames()[type];
            assert( form != NULL, "Replacement variable's type not found");
            OperandForm *op = form->is_operand();
            assert( op, "Attempting to emit a non-register or non-constant");
            // Check if this is a constant
            if (op->_matrule && op->_matrule->is_base_constant(_AD.globalNames())) {
              // Check which constant this name maps to: _c0, _c1, ..., _cn
              // const int idx = _operand.constant_position(_AD.globalNames(), comp);
              // assert( idx != -1, "Constant component not found in operand");
              Form::DataType dtype = op->is_base_constant(_AD.globalNames());
              if ( dtype == Form::idealP ) {
                _may_reloc    = true;
                // No longer true that idealP is always an oop
                _reloc_form   = RELOC_DISP;
                _reloc_type   = AdlcVMDeps::oop_reloc_type();
              }
            }

            else if( _operand->is_user_name_for_sReg() != Form::none ) {
              // The only non-constant allowed access to disp is an operand sRegX in a stackSlotX
              assert( op->ideal_to_sReg_type(type) != Form::none, "StackSlots access displacements using 'sRegs'");
              _may_reloc   = false;
            } else {
              assert( false, "fatal(); Only stackSlots can access a non-constant using 'disp'");
            }
          }
        } // finished with precise check of operand for relocation.
      } // finished with subfield variable
      else if ( strcmp(rep_var,"$constant") == 0 ) {
        _doing_constant = true;
        if ( _constant_status == LITERAL_NOT_SEEN ) {
          // Check operand for type of constant
          assert( _operand, "Must use operand before '$$constant'");
          Form::DataType dtype = _operand->is_base_constant(_AD.globalNames());
          _constant_type = dtype;
          if ( dtype == Form::idealP ) {
            _may_reloc    = true;
            // No longer true that idealP is always an oop
            // // _must_reloc   = true;
            _reloc_form   = RELOC_IMMEDIATE;
            _reloc_type   = AdlcVMDeps::oop_reloc_type();
          } else {
            // No relocation information needed
          }
        } else {
          // User-provided literals may not require relocation information !!!!!
          assert( _constant_status == LITERAL_SEEN, "Must know we are processing a user-provided literal");
        }
      }
      else if ( strcmp(rep_var,"$label") == 0 ) {
        // Calls containing labels require relocation
        if ( _inst.is_ideal_call() )  {
          _may_reloc    = true;
          // !!!!! !!!!!
          _reloc_type   = AdlcVMDeps::none_reloc_type();
        }
      }

      // literal register parameter must be accessed as a 'reg' field.
      if ( _reg_status != LITERAL_NOT_SEEN ) {
        assert( _reg_status == LITERAL_SEEN, "Must have seen register literal before now");
        if (strcmp(rep_var,"$reg") == 0 || reg_conversion(rep_var) != NULL) {
          _reg_status  = LITERAL_ACCESSED;
        } else {
          _AD.syntax_err(_encoding._linenum,
                         "Invalid access to literal register parameter '%s' in %s.\n",
                         rep_var, _encoding._name);
          assert( false, "invalid access to literal register parameter");
        }
      }
      // literal constant parameters must be accessed as a 'constant' field
      if (_constant_status != LITERAL_NOT_SEEN) {
        assert(_constant_status == LITERAL_SEEN, "Must have seen constant literal before now");
        if (strcmp(rep_var,"$constant") == 0) {
          _constant_status = LITERAL_ACCESSED;
        } else {
          _AD.syntax_err(_encoding._linenum,
                         "Invalid access to literal constant parameter '%s' in %s.\n",
                         rep_var, _encoding._name);
        }
      }
    } // end replacement and/or subfield

  }

  void add_rep_var(const char *rep_var) {
    // Handle subfield and replacement variables.
    if ( ( *rep_var == '$' ) && ( *(rep_var+1) == '$' ) ) {
      // Check for emit prefix, '$$emit32'
      assert( _cleared, "Can not nest $$$emit32");
      if ( strcmp(rep_var,"$$emit32") == 0 ) {
        _doing_emit_d32 = true;
      }
      else if ( strcmp(rep_var,"$$emit16") == 0 ) {
        _doing_emit_d16 = true;
      }
      else if ( strcmp(rep_var,"$$emit_hi") == 0 ) {
        _doing_emit_hi  = true;
      }
      else if ( strcmp(rep_var,"$$emit_lo") == 0 ) {
        _doing_emit_lo  = true;
      }
      else if ( strcmp(rep_var,"$$emit8") == 0 ) {
        _doing_emit8    = true;
      }
      else {
        _AD.syntax_err(_encoding._linenum, "Unsupported $$operation '%s'\n",rep_var);
        assert( false, "fatal();");
      }
    }
    else {
      // Update state for replacement variables
      update_state( rep_var );
      _strings_to_emit.addName(rep_var);
    }
    _cleared  = false;
  }

  void emit_replacement() {
    // A replacement variable or one of its subfields
    // Obtain replacement variable from list
    // const char *ec_rep_var = encoding->_rep_vars.iter();
    const char *rep_var;
    _strings_to_emit.reset();
    while ( (rep_var = _strings_to_emit.iter()) != NULL ) {

      if ( (*rep_var) == '$' ) {
        // A subfield variable, '$$' prefix
        emit_field( rep_var );
      } else {
        if (_strings_to_emit.peek() != NULL &&
            strcmp(_strings_to_emit.peek(), "$Address") == 0) {
          fprintf(_fp, "Address::make_raw(");

          emit_rep_var( rep_var );
          fprintf(_fp,"->base(ra_,this,idx%d), ", _operand_idx);

          _reg_status = LITERAL_ACCESSED;
          emit_rep_var( rep_var );
          fprintf(_fp,"->index(ra_,this,idx%d), ", _operand_idx);

          _reg_status = LITERAL_ACCESSED;
          emit_rep_var( rep_var );
          fprintf(_fp,"->scale(), ");

          _reg_status = LITERAL_ACCESSED;
          emit_rep_var( rep_var );
          Form::DataType stack_type = _operand ? _operand->is_user_name_for_sReg() : Form::none;
          if( _operand  && _operand_idx==0 && stack_type != Form::none ) {
            fprintf(_fp,"->disp(ra_,this,0), ");
          } else {
            fprintf(_fp,"->disp(ra_,this,idx%d), ", _operand_idx);
          }

          _reg_status = LITERAL_ACCESSED;
          emit_rep_var( rep_var );
          fprintf(_fp,"->disp_reloc())");

          // skip trailing $Address
          _strings_to_emit.iter();
        } else {
          // A replacement variable, '$' prefix
          const char* next = _strings_to_emit.peek();
          const char* next2 = _strings_to_emit.peek(2);
          if (next != NULL && next2 != NULL && strcmp(next2, "$Register") == 0 &&
              (strcmp(next, "$base") == 0 || strcmp(next, "$index") == 0)) {
            // handle $rev_var$$base$$Register and $rev_var$$index$$Register by
            // producing as_Register(opnd_array(#)->base(ra_,this,idx1)).
            fprintf(_fp, "as_Register(");
            // emit the operand reference
            emit_rep_var( rep_var );
            rep_var = _strings_to_emit.iter();
            assert(strcmp(rep_var, "$base") == 0 || strcmp(rep_var, "$index") == 0, "bad pattern");
            // handle base or index
            emit_field(rep_var);
            rep_var = _strings_to_emit.iter();
            assert(strcmp(rep_var, "$Register") == 0, "bad pattern");
            // close up the parens
            fprintf(_fp, ")");
          } else {
            emit_rep_var( rep_var );
          }
        }
      } // end replacement and/or subfield
    }
  }

  void emit_reloc_type(const char* type) {
    fprintf(_fp, "%s", type)
      ;
  }


  void emit() {
    //
    //   "emit_d32_reloc(" or "emit_hi_reloc" or "emit_lo_reloc"
    //
    // Emit the function name when generating an emit function
    if ( _doing_emit_d32 || _doing_emit_hi || _doing_emit_lo ) {
      const char *d32_hi_lo = _doing_emit_d32 ? "d32" : (_doing_emit_hi ? "hi" : "lo");
      // In general, relocatable isn't known at compiler compile time.
      // Check results of prior scan
      if ( ! _may_reloc ) {
        // Definitely don't need relocation information
        fprintf( _fp, "emit_%s(cbuf, ", d32_hi_lo );
        emit_replacement(); fprintf(_fp, ")");
      }
      else {
        // Emit RUNTIME CHECK to see if value needs relocation info
        // If emitting a relocatable address, use 'emit_d32_reloc'
        const char *disp_constant = _doing_disp ? "disp" : _doing_constant ? "constant" : "INVALID";
        assert( (_doing_disp || _doing_constant)
                && !(_doing_disp && _doing_constant),
                "Must be emitting either a displacement or a constant");
        fprintf(_fp,"\n");
        fprintf(_fp,"if ( opnd_array(%d)->%s_reloc() != relocInfo::none ) {\n",
                _operand_idx, disp_constant);
        fprintf(_fp,"  ");
        fprintf(_fp,"emit_%s_reloc(cbuf, ", d32_hi_lo );
        emit_replacement();             fprintf(_fp,", ");
        fprintf(_fp,"opnd_array(%d)->%s_reloc(), ",
                _operand_idx, disp_constant);
        fprintf(_fp, "%d", _reloc_form);fprintf(_fp, ");");
        fprintf(_fp,"\n");
        fprintf(_fp,"} else {\n");
        fprintf(_fp,"  emit_%s(cbuf, ", d32_hi_lo);
        emit_replacement(); fprintf(_fp, ");\n"); fprintf(_fp,"}");
      }
    }
    else if ( _doing_emit_d16 ) {
      // Relocation of 16-bit values is not supported
      fprintf(_fp,"emit_d16(cbuf, ");
      emit_replacement(); fprintf(_fp, ")");
      // No relocation done for 16-bit values
    }
    else if ( _doing_emit8 ) {
      // Relocation of 8-bit values is not supported
      fprintf(_fp,"emit_d8(cbuf, ");
      emit_replacement(); fprintf(_fp, ")");
      // No relocation done for 8-bit values
    }
    else {
      // Not an emit# command, just output the replacement string.
      emit_replacement();
    }

    // Get ready for next state collection.
    clear();
  }

private:

  // recognizes names which represent MacroAssembler register types
  // and return the conversion function to build them from OptoReg
  const char* reg_conversion(const char* rep_var) {
    if (strcmp(rep_var,"$Register") == 0)      return "as_Register";
    if (strcmp(rep_var,"$KRegister") == 0)     return "as_KRegister";
    if (strcmp(rep_var,"$FloatRegister") == 0) return "as_FloatRegister";
#if defined(IA32) || defined(AMD64)
    if (strcmp(rep_var,"$XMMRegister") == 0)   return "as_XMMRegister";
#endif
    if (strcmp(rep_var,"$CondRegister") == 0)  return "as_ConditionRegister";
#if defined(PPC64)
    if (strcmp(rep_var,"$VectorRegister") == 0)   return "as_VectorRegister";
    if (strcmp(rep_var,"$VectorSRegister") == 0)  return "as_VectorSRegister";
#endif
    return NULL;
  }

  void emit_field(const char *rep_var) {
    const char* reg_convert = reg_conversion(rep_var);

    // A subfield variable, '$$subfield'
    if ( strcmp(rep_var, "$reg") == 0 || reg_convert != NULL) {
      // $reg form or the $Register MacroAssembler type conversions
      assert( _operand_idx != -1,
              "Must use this subfield after operand");
      if( _reg_status == LITERAL_NOT_SEEN ) {
        if (_processing_noninput) {
          const Form  *local     = _inst._localNames[_operand_name];
          OperandForm *oper      = local->is_operand();
          const RegDef* first = oper->get_RegClass()->find_first_elem();
          if (reg_convert != NULL) {
            fprintf(_fp, "%s(%s_enc)", reg_convert, first->_regname);
          } else {
            fprintf(_fp, "%s_enc", first->_regname);
          }
        } else {
          fprintf(_fp,"->%s(ra_,this", reg_convert != NULL ? reg_convert : "reg");
          // Add parameter for index position, if not result operand
          if( _operand_idx != 0 ) fprintf(_fp,",idx%d", _operand_idx);
          fprintf(_fp,")");
          fprintf(_fp, "/* %s */", _operand_name);
        }
      } else {
        assert( _reg_status == LITERAL_OUTPUT, "should have output register literal in emit_rep_var");
        // Register literal has already been sent to output file, nothing more needed
      }
    }
    else if ( strcmp(rep_var,"$base") == 0 ) {
      assert( _operand_idx != -1,
              "Must use this subfield after operand");
      assert( ! _may_reloc, "UnImplemented()");
      fprintf(_fp,"->base(ra_,this,idx%d)", _operand_idx);
    }
    else if ( strcmp(rep_var,"$index") == 0 ) {
      assert( _operand_idx != -1,
              "Must use this subfield after operand");
      assert( ! _may_reloc, "UnImplemented()");
      fprintf(_fp,"->index(ra_,this,idx%d)", _operand_idx);
    }
    else if ( strcmp(rep_var,"$scale") == 0 ) {
      assert( ! _may_reloc, "UnImplemented()");
      fprintf(_fp,"->scale()");
    }
    else if ( strcmp(rep_var,"$cmpcode") == 0 ) {
      assert( ! _may_reloc, "UnImplemented()");
      fprintf(_fp,"->ccode()");
    }
    else if ( strcmp(rep_var,"$constant") == 0 ) {
      if( _constant_status == LITERAL_NOT_SEEN ) {
        if ( _constant_type == Form::idealD ) {
          fprintf(_fp,"->constantD()");
        } else if ( _constant_type == Form::idealF ) {
          fprintf(_fp,"->constantF()");
        } else if ( _constant_type == Form::idealL ) {
          fprintf(_fp,"->constantL()");
        } else {
          fprintf(_fp,"->constant()");
        }
      } else {
        assert( _constant_status == LITERAL_OUTPUT, "should have output constant literal in emit_rep_var");
        // Constant literal has already been sent to output file, nothing more needed
      }
    }
    else if ( strcmp(rep_var,"$disp") == 0 ) {
      Form::DataType stack_type = _operand ? _operand->is_user_name_for_sReg() : Form::none;
      if( _operand  && _operand_idx==0 && stack_type != Form::none ) {
        fprintf(_fp,"->disp(ra_,this,0)");
      } else {
        fprintf(_fp,"->disp(ra_,this,idx%d)", _operand_idx);
      }
    }
    else if ( strcmp(rep_var,"$label") == 0 ) {
      fprintf(_fp,"->label()");
    }
    else if ( strcmp(rep_var,"$method") == 0 ) {
      fprintf(_fp,"->method()");
    }
    else {
      printf("emit_field: %s\n",rep_var);
      globalAD->syntax_err(_inst._linenum, "Unknown replacement variable %s in format statement of %s.",
                           rep_var, _inst._ident);
      assert( false, "UnImplemented()");
    }
  }


  void emit_rep_var(const char *rep_var) {
    _processing_noninput = false;
    // A replacement variable, originally '$'
    if ( Opcode::as_opcode_type(rep_var) != Opcode::NOT_AN_OPCODE ) {
      if ((_inst._opcode == NULL) || !_inst._opcode->print_opcode(_fp, Opcode::as_opcode_type(rep_var) )) {
        // Missing opcode
        _AD.syntax_err( _inst._linenum,
                        "Missing $%s opcode definition in %s, used by encoding %s\n",
                        rep_var, _inst._ident, _encoding._name);
      }
    }
    else if (strcmp(rep_var, "constanttablebase") == 0) {
      fprintf(_fp, "as_Register(ra_->get_encode(in(mach_constant_base_node_input())))");
    }
    else if (strcmp(rep_var, "constantoffset") == 0) {
      fprintf(_fp, "constant_offset()");
    }
    else if (strcmp(rep_var, "constantaddress") == 0) {
      fprintf(_fp, "InternalAddress(__ code()->consts()->start() + constant_offset())");
    }
    else {
      // Lookup its position in parameter list
      int   param_no  = _encoding.rep_var_index(rep_var);
      if ( param_no == -1 ) {
        _AD.syntax_err( _encoding._linenum,
                        "Replacement variable %s not found in enc_class %s.\n",
                        rep_var, _encoding._name);
      }
      // Lookup the corresponding ins_encode parameter
      const char *inst_rep_var = _ins_encode.rep_var_name(_inst, param_no);

      // Check if instruction's actual parameter is a local name in the instruction
      const Form  *local     = _inst._localNames[inst_rep_var];
      OpClassForm *opc       = (local != NULL) ? local->is_opclass() : NULL;
      // Note: assert removed to allow constant and symbolic parameters
      // assert( opc, "replacement variable was not found in local names");
      // Lookup the index position iff the replacement variable is a localName
      int idx  = (opc != NULL) ? _inst.operand_position_format(inst_rep_var) : -1;
      if( idx != -1 ) {
        if (_inst.is_noninput_operand(idx)) {
          // This operand isn't a normal input so printing it is done
          // specially.
          _processing_noninput = true;
        } else {
          // Output the emit code for this operand
          fprintf(_fp,"opnd_array(%d)",idx);
        }
        assert( _operand == opc->is_operand(),
                "Previous emit $operand does not match current");
      }
      else if( ADLParser::is_literal_constant(inst_rep_var) ) {
        // else check if it is a constant expression
        // Removed following assert to allow primitive C types as arguments to encodings
        // assert( _constant_status == LITERAL_ACCESSED, "Must be processing a literal constant parameter");
        fprintf(_fp,"(%s)", inst_rep_var);
        _constant_status = LITERAL_OUTPUT;
      }
      else if( Opcode::as_opcode_type(inst_rep_var) != Opcode::NOT_AN_OPCODE ) {
        // else check if "primary", "secondary", "tertiary"
        assert( _constant_status == LITERAL_ACCESSED, "Must be processing a literal constant parameter");
        if ((_inst._opcode == NULL) || !_inst._opcode->print_opcode(_fp, Opcode::as_opcode_type(inst_rep_var) )) {
          // Missing opcode
          _AD.syntax_err( _inst._linenum,
                          "Missing $%s opcode definition in %s\n",
                          rep_var, _inst._ident);

        }
        _constant_status = LITERAL_OUTPUT;
      }
      else if((_AD.get_registers() != NULL ) && (_AD.get_registers()->getRegDef(inst_rep_var) != NULL)) {
        // Instruction provided a literal register name for this parameter
        // Check that encoding specifies $$$reg to resolve.as register.
        assert( _reg_status == LITERAL_ACCESSED, "Must be processing a literal register parameter");
        fprintf(_fp,"(%s_enc)", inst_rep_var);
        _reg_status = LITERAL_OUTPUT;
      }
      else {
        // Check for unimplemented functionality before hard failure
        assert(opc != NULL && strcmp(opc->_ident, "label") == 0, "Unimplemented Label");
        assert(false, "ShouldNotReachHere()");
      }
      // all done
    }
  }

};  // end class DefineEmitState


void ArchDesc::defineSize(FILE *fp, InstructForm &inst) {

  //(1)
  // Output instruction's emit prototype
  fprintf(fp,"uint %sNode::size(PhaseRegAlloc *ra_) const {\n",
          inst._ident);

  fprintf(fp, "  assert(VerifyOops || MachNode::size(ra_) <= %s, \"bad fixed size\");\n", inst._size);

  //(2)
  // Print the size
  fprintf(fp, "  return (VerifyOops ? MachNode::size(ra_) : %s);\n", inst._size);

  // (3) and (4)
  fprintf(fp,"}\n\n");
}

// Emit postalloc expand function.
void ArchDesc::define_postalloc_expand(FILE *fp, InstructForm &inst) {
  InsEncode *ins_encode = inst._insencode;

  // Output instruction's postalloc_expand prototype.
  fprintf(fp, "void  %sNode::postalloc_expand(GrowableArray <Node *> *nodes, PhaseRegAlloc *ra_) {\n",
          inst._ident);

  assert((_encode != NULL) && (ins_encode != NULL), "You must define an encode section.");

  // Output each operand's offset into the array of registers.
  inst.index_temps(fp, _globalNames);

  // Output variables "unsigned idx_<par_name>", Node *n_<par_name> and "MachOpnd *op_<par_name>"
  // for each parameter <par_name> specified in the encoding.
  ins_encode->reset();
  const char *ec_name = ins_encode->encode_class_iter();
  assert(ec_name != NULL, "Postalloc expand must specify an encoding.");

  EncClass *encoding = _encode->encClass(ec_name);
  if (encoding == NULL) {
    fprintf(stderr, "User did not define contents of this encode_class: %s\n", ec_name);
    abort();
  }
  if (ins_encode->current_encoding_num_args() != encoding->num_args()) {
    globalAD->syntax_err(ins_encode->_linenum, "In %s: passing %d arguments to %s but expecting %d",
                         inst._ident, ins_encode->current_encoding_num_args(),
                         ec_name, encoding->num_args());
  }

  fprintf(fp, "  // Access to ins and operands for postalloc expand.\n");
  const int buflen = 2000;
  char idxbuf[buflen]; char *ib = idxbuf; idxbuf[0] = '\0';
  char nbuf  [buflen]; char *nb = nbuf;   nbuf[0]   = '\0';
  char opbuf [buflen]; char *ob = opbuf;  opbuf[0]  = '\0';

  encoding->_parameter_type.reset();
  encoding->_parameter_name.reset();
  const char *type = encoding->_parameter_type.iter();
  const char *name = encoding->_parameter_name.iter();
  int param_no = 0;
  for (; (type != NULL) && (name != NULL);
       (type = encoding->_parameter_type.iter()), (name = encoding->_parameter_name.iter())) {
    const char* arg_name = ins_encode->rep_var_name(inst, param_no);
    int idx = inst.operand_position_format(arg_name);
    if (strcmp(arg_name, "constanttablebase") == 0) {
      ib += sprintf(ib, "  unsigned idx_%-5s = mach_constant_base_node_input(); \t// %s, \t%s\n",
                    name, type, arg_name);
      nb += sprintf(nb, "  Node    *n_%-7s = lookup(idx_%s);\n", name, name);
      // There is no operand for the constanttablebase.
    } else if (inst.is_noninput_operand(idx)) {
      globalAD->syntax_err(inst._linenum,
                           "In %s: you can not pass the non-input %s to a postalloc expand encoding.\n",
                           inst._ident, arg_name);
    } else {
      ib += sprintf(ib, "  unsigned idx_%-5s = idx%d; \t// %s, \t%s\n",
                    name, idx, type, arg_name);
      nb += sprintf(nb, "  Node    *n_%-7s = lookup(idx_%s);\n", name, name);
      ob += sprintf(ob, "  %sOper *op_%s = (%sOper *)opnd_array(%d);\n", type, name, type, idx);
    }
    param_no++;
  }
  assert(ib < &idxbuf[buflen-1] && nb < &nbuf[buflen-1] && ob < &opbuf[buflen-1], "buffer overflow");

  fprintf(fp, "%s", idxbuf);
  fprintf(fp, "  Node    *n_region  = lookup(0);\n");
  fprintf(fp, "%s%s", nbuf, opbuf);
  fprintf(fp, "  Compile *C = ra_->C;\n");

  // Output this instruction's encodings.
  fprintf(fp, "  {");
  const char *ec_code    = NULL;
  const char *ec_rep_var = NULL;
  assert(encoding == _encode->encClass(ec_name), "");

  DefineEmitState pending(fp, *this, *encoding, *ins_encode, inst);
  encoding->_code.reset();
  encoding->_rep_vars.reset();
  // Process list of user-defined strings,
  // and occurrences of replacement variables.
  // Replacement Vars are pushed into a list and then output.
  while ((ec_code = encoding->_code.iter()) != NULL) {
    if (! encoding->_code.is_signal(ec_code)) {
      // Emit pending code.
      pending.emit();
      pending.clear();
      // Emit this code section.
      fprintf(fp, "%s", ec_code);
    } else {
      // A replacement variable or one of its subfields.
      // Obtain replacement variable from list.
      ec_rep_var = encoding->_rep_vars.iter();
      pending.add_rep_var(ec_rep_var);
    }
  }
  // Emit pending code.
  pending.emit();
  pending.clear();
  fprintf(fp, "  }\n");

  fprintf(fp, "}\n\n");

  ec_name = ins_encode->encode_class_iter();
  assert(ec_name == NULL, "Postalloc expand may only have one encoding.");
}

// defineEmit -----------------------------------------------------------------
void ArchDesc::defineEmit(FILE* fp, InstructForm& inst) {
  InsEncode* encode = inst._insencode;

  // (1)
  // Output instruction's emit prototype
  fprintf(fp, "void %sNode::emit(CodeBuffer& cbuf, PhaseRegAlloc* ra_) const {\n", inst._ident);

  // If user did not define an encode section,
  // provide stub that does not generate any machine code.
  if( (_encode == NULL) || (encode == NULL) ) {
    fprintf(fp, "  // User did not define an encode section.\n");
    fprintf(fp, "}\n");
    return;
  }

  // Save current instruction's starting address (helps with relocation).
  fprintf(fp, "  cbuf.set_insts_mark();\n");

  // For MachConstantNodes which are ideal jump nodes, fill the jump table.
  if (inst.is_mach_constant() && inst.is_ideal_jump()) {
    fprintf(fp, "  ra_->C->output()->constant_table().fill_jump_table(cbuf, (MachConstantNode*) this, _index2label);\n");
  }

  // Output each operand's offset into the array of registers.
  inst.index_temps(fp, _globalNames);

  // Output this instruction's encodings
  const char *ec_name;
  bool        user_defined = false;
  encode->reset();
  while ((ec_name = encode->encode_class_iter()) != NULL) {
    fprintf(fp, "  {\n");
    // Output user-defined encoding
    user_defined           = true;

    const char *ec_code    = NULL;
    const char *ec_rep_var = NULL;
    EncClass   *encoding   = _encode->encClass(ec_name);
    if (encoding == NULL) {
      fprintf(stderr, "User did not define contents of this encode_class: %s\n", ec_name);
      abort();
    }

    if (encode->current_encoding_num_args() != encoding->num_args()) {
      globalAD->syntax_err(encode->_linenum, "In %s: passing %d arguments to %s but expecting %d",
                           inst._ident, encode->current_encoding_num_args(),
                           ec_name, encoding->num_args());
    }

    DefineEmitState pending(fp, *this, *encoding, *encode, inst);
    encoding->_code.reset();
    encoding->_rep_vars.reset();
    // Process list of user-defined strings,
    // and occurrences of replacement variables.
    // Replacement Vars are pushed into a list and then output
    while ((ec_code = encoding->_code.iter()) != NULL) {
      if (!encoding->_code.is_signal(ec_code)) {
        // Emit pending code
        pending.emit();
        pending.clear();
        // Emit this code section
        fprintf(fp, "%s", ec_code);
      } else {
        // A replacement variable or one of its subfields
        // Obtain replacement variable from list
        ec_rep_var  = encoding->_rep_vars.iter();
        pending.add_rep_var(ec_rep_var);
      }
    }
    // Emit pending code
    pending.emit();
    pending.clear();
    fprintf(fp, "  }\n");
  } // end while instruction's encodings

  // Check if user stated which encoding to user
  if ( user_defined == false ) {
    fprintf(fp, "  // User did not define which encode class to use.\n");
  }

  // (3) and (4)
  fprintf(fp, "}\n\n");
}

// defineEvalConstant ---------------------------------------------------------
void ArchDesc::defineEvalConstant(FILE* fp, InstructForm& inst) {
  InsEncode* encode = inst._constant;

  // (1)
  // Output instruction's emit prototype
  fprintf(fp, "void %sNode::eval_constant(Compile* C) {\n", inst._ident);

  // For ideal jump nodes, add a jump-table entry.
  if (inst.is_ideal_jump()) {
    fprintf(fp, "  _constant = C->output()->constant_table().add_jump_table(this);\n");
  }

  // If user did not define an encode section,
  // provide stub that does not generate any machine code.
  if ((_encode == NULL) || (encode == NULL)) {
    fprintf(fp, "  // User did not define an encode section.\n");
    fprintf(fp, "}\n");
    return;
  }

  // Output this instruction's encodings
  const char *ec_name;
  bool        user_defined = false;
  encode->reset();
  while ((ec_name = encode->encode_class_iter()) != NULL) {
    fprintf(fp, "  {\n");
    // Output user-defined encoding
    user_defined           = true;

    const char *ec_code    = NULL;
    const char *ec_rep_var = NULL;
    EncClass   *encoding   = _encode->encClass(ec_name);
    if (encoding == NULL) {
      fprintf(stderr, "User did not define contents of this encode_class: %s\n", ec_name);
      abort();
    }

    if (encode->current_encoding_num_args() != encoding->num_args()) {
      globalAD->syntax_err(encode->_linenum, "In %s: passing %d arguments to %s but expecting %d",
                           inst._ident, encode->current_encoding_num_args(),
                           ec_name, encoding->num_args());
    }

    DefineEmitState pending(fp, *this, *encoding, *encode, inst);
    encoding->_code.reset();
    encoding->_rep_vars.reset();
    // Process list of user-defined strings,
    // and occurrences of replacement variables.
    // Replacement Vars are pushed into a list and then output
    while ((ec_code = encoding->_code.iter()) != NULL) {
      if (!encoding->_code.is_signal(ec_code)) {
        // Emit pending code
        pending.emit();
        pending.clear();
        // Emit this code section
        fprintf(fp, "%s", ec_code);
      } else {
        // A replacement variable or one of its subfields
        // Obtain replacement variable from list
        ec_rep_var  = encoding->_rep_vars.iter();
        pending.add_rep_var(ec_rep_var);
      }
    }
    // Emit pending code
    pending.emit();
    pending.clear();
    fprintf(fp, "  }\n");
  } // end while instruction's encodings

  // Check if user stated which encoding to user
  if (user_defined == false) {
    fprintf(fp, "  // User did not define which encode class to use.\n");
  }

  // (3) and (4)
  fprintf(fp, "}\n");
}

// ---------------------------------------------------------------------------
//--------Utilities to build MachOper and MachNode derived Classes------------
// ---------------------------------------------------------------------------

//------------------------------Utilities to build Operand Classes------------
static void defineIn_RegMask(FILE *fp, FormDict &globals, OperandForm &oper) {
  uint num_edges = oper.num_edges(globals);
  if( num_edges != 0 ) {
    // Method header
    fprintf(fp, "const RegMask *%sOper::in_RegMask(int index) const {\n",
            oper._ident);

    // Assert that the index is in range.
    fprintf(fp, "  assert(0 <= index && index < %d, \"index out of range\");\n",
            num_edges);

    // Figure out if all RegMasks are the same.
    const char* first_reg_class = oper.in_reg_class(0, globals);
    bool all_same = true;
    assert(first_reg_class != NULL, "did not find register mask");

    for (uint index = 1; all_same && index < num_edges; index++) {
      const char* some_reg_class = oper.in_reg_class(index, globals);
      assert(some_reg_class != NULL, "did not find register mask");
      if (strcmp(first_reg_class, some_reg_class) != 0) {
        all_same = false;
      }
    }

    if (all_same) {
      // Return the sole RegMask.
      if (strcmp(first_reg_class, "stack_slots") == 0) {
        fprintf(fp,"  return &(Compile::current()->FIRST_STACK_mask());\n");
      } else if (strcmp(first_reg_class, "dynamic") == 0) {
        fprintf(fp,"  return &RegMask::Empty;\n");
      } else {
        const char* first_reg_class_to_upper = toUpper(first_reg_class);
        fprintf(fp,"  return &%s_mask();\n", first_reg_class_to_upper);
        delete[] first_reg_class_to_upper;
      }
    } else {
      // Build a switch statement to return the desired mask.
      fprintf(fp,"  switch (index) {\n");

      for (uint index = 0; index < num_edges; index++) {
        const char *reg_class = oper.in_reg_class(index, globals);
        assert(reg_class != NULL, "did not find register mask");
        if( !strcmp(reg_class, "stack_slots") ) {
          fprintf(fp, "  case %d: return &(Compile::current()->FIRST_STACK_mask());\n", index);
        } else {
          const char* reg_class_to_upper = toUpper(reg_class);
          fprintf(fp, "  case %d: return &%s_mask();\n", index, reg_class_to_upper);
          delete[] reg_class_to_upper;
        }
      }
      fprintf(fp,"  }\n");
      fprintf(fp,"  ShouldNotReachHere();\n");
      fprintf(fp,"  return NULL;\n");
    }

    // Method close
    fprintf(fp, "}\n\n");
  }
}

// generate code to create a clone for a class derived from MachOper
//
// (0)  MachOper  *MachOperXOper::clone() const {
// (1)    return new MachXOper( _ccode, _c0, _c1, ..., _cn);
// (2)  }
//
static void defineClone(FILE *fp, FormDict &globalNames, OperandForm &oper) {
  fprintf(fp,"MachOper *%sOper::clone() const {\n", oper._ident);
  // Check for constants that need to be copied over
  const int  num_consts    = oper.num_consts(globalNames);
  const bool is_ideal_bool = oper.is_ideal_bool();
  if( (num_consts > 0) ) {
    fprintf(fp,"  return new %sOper(", oper._ident);
    // generate parameters for constants
    int i = 0;
    fprintf(fp,"_c%d", i);
    for( i = 1; i < num_consts; ++i) {
      fprintf(fp,", _c%d", i);
    }
    // finish line (1)
    fprintf(fp,");\n");
  }
  else {
    assert( num_consts == 0, "Currently support zero or one constant per operand clone function");
    fprintf(fp,"  return new %sOper();\n", oper._ident);
  }
  // finish method
  fprintf(fp,"}\n");
}

// Helper functions for bug 4796752, abstracted with minimal modification
// from define_oper_interface()
OperandForm *rep_var_to_operand(const char *encoding, OperandForm &oper, FormDict &globals) {
  OperandForm *op = NULL;
  // Check for replacement variable
  if( *encoding == '$' ) {
    // Replacement variable
    const char *rep_var = encoding + 1;
    // Lookup replacement variable, rep_var, in operand's component list
    const Component *comp = oper._components.search(rep_var);
    assert( comp != NULL, "Replacement variable not found in components");
    // Lookup operand form for replacement variable's type
    const char      *type = comp->_type;
    Form            *form = (Form*)globals[type];
    assert( form != NULL, "Replacement variable's type not found");
    op = form->is_operand();
    assert( op, "Attempting to emit a non-register or non-constant");
  }

  return op;
}

int rep_var_to_constant_index(const char *encoding, OperandForm &oper, FormDict &globals) {
  int idx = -1;
  // Check for replacement variable
  if( *encoding == '$' ) {
    // Replacement variable
    const char *rep_var = encoding + 1;
    // Lookup replacement variable, rep_var, in operand's component list
    const Component *comp = oper._components.search(rep_var);
    assert( comp != NULL, "Replacement variable not found in components");
    // Lookup operand form for replacement variable's type
    const char      *type = comp->_type;
    Form            *form = (Form*)globals[type];
    assert( form != NULL, "Replacement variable's type not found");
    OperandForm *op = form->is_operand();
    assert( op, "Attempting to emit a non-register or non-constant");
    // Check that this is a constant and find constant's index:
    if (op->_matrule && op->_matrule->is_base_constant(globals)) {
      idx  = oper.constant_position(globals, comp);
    }
  }

  return idx;
}

bool is_regI(const char *encoding, OperandForm &oper, FormDict &globals ) {
  bool is_regI = false;

  OperandForm *op = rep_var_to_operand(encoding, oper, globals);
  if( op != NULL ) {
    // Check that this is a register
    if ( (op->_matrule && op->_matrule->is_base_register(globals)) ) {
      // Register
      const char* ideal  = op->ideal_type(globals);
      is_regI = (ideal && (op->ideal_to_Reg_type(ideal) == Form::idealI));
    }
  }

  return is_regI;
}

bool is_conP(const char *encoding, OperandForm &oper, FormDict &globals ) {
  bool is_conP = false;

  OperandForm *op = rep_var_to_operand(encoding, oper, globals);
  if( op != NULL ) {
    // Check that this is a constant pointer
    if (op->_matrule && op->_matrule->is_base_constant(globals)) {
      // Constant
      Form::DataType dtype = op->is_base_constant(globals);
      is_conP = (dtype == Form::idealP);
    }
  }

  return is_conP;
}


// Define a MachOper interface methods
void ArchDesc::define_oper_interface(FILE *fp, OperandForm &oper, FormDict &globals,
                                     const char *name, const char *encoding) {
  bool emit_position = false;
  int position = -1;

  fprintf(fp,"  virtual int            %s", name);
  // Generate access method for base, index, scale, disp, ...
  if( (strcmp(name,"base") == 0) || (strcmp(name,"index") == 0) ) {
    fprintf(fp,"(PhaseRegAlloc *ra_, const Node *node, int idx) const { \n");
    emit_position = true;
  } else if ( (strcmp(name,"disp") == 0) ) {
    fprintf(fp,"(PhaseRegAlloc *ra_, const Node *node, int idx) const { \n");
  } else {
    fprintf(fp, "() const {\n");
  }

  // Check for hexadecimal value OR replacement variable
  if( *encoding == '$' ) {
    // Replacement variable
    const char *rep_var = encoding + 1;
    fprintf(fp,"    // Replacement variable: %s\n", encoding+1);
    // Lookup replacement variable, rep_var, in operand's component list
    const Component *comp = oper._components.search(rep_var);
    assert( comp != NULL, "Replacement variable not found in components");
    // Lookup operand form for replacement variable's type
    const char      *type = comp->_type;
    Form            *form = (Form*)globals[type];
    assert( form != NULL, "Replacement variable's type not found");
    OperandForm *op = form->is_operand();
    assert( op, "Attempting to emit a non-register or non-constant");
    // Check that this is a register or a constant and generate code:
    if ( (op->_matrule && op->_matrule->is_base_register(globals)) ) {
      // Register
      int idx_offset = oper.register_position( globals, rep_var);
      position = idx_offset;
      fprintf(fp,"    return (int)ra_->get_encode(node->in(idx");
      if ( idx_offset > 0 ) fprintf(fp,                      "+%d",idx_offset);
      fprintf(fp,"));\n");
    } else if ( op->ideal_to_sReg_type(op->_ident) != Form::none ) {
      // StackSlot for an sReg comes either from input node or from self, when idx==0
      fprintf(fp,"    if( idx != 0 ) {\n");
      fprintf(fp,"      // Access stack offset (register number) for input operand\n");
      fprintf(fp,"      return ra_->reg2offset(ra_->get_reg_first(node->in(idx)));/* sReg */\n");
      fprintf(fp,"    }\n");
      fprintf(fp,"    // Access stack offset (register number) from myself\n");
      fprintf(fp,"    return ra_->reg2offset(ra_->get_reg_first(node));/* sReg */\n");
    } else if (op->_matrule && op->_matrule->is_base_constant(globals)) {
      // Constant
      // Check which constant this name maps to: _c0, _c1, ..., _cn
      const int idx = oper.constant_position(globals, comp);
      assert( idx != -1, "Constant component not found in operand");
      // Output code for this constant, type dependent.
      fprintf(fp,"    return (int)" );
      oper.access_constant(fp, globals, (uint)idx /* , const_type */);
      fprintf(fp,";\n");
    } else {
      assert( false, "Attempting to emit a non-register or non-constant");
    }
  }
  else if( *encoding == '0' && *(encoding+1) == 'x' ) {
    // Hex value
    fprintf(fp,"    return %s;\n", encoding);
  } else {
    globalAD->syntax_err(oper._linenum, "In operand %s: Do not support this encode constant: '%s' for %s.",
                         oper._ident, encoding, name);
    assert( false, "Do not support octal or decimal encode constants");
  }
  fprintf(fp,"  }\n");

  if( emit_position && (position != -1) && (oper.num_edges(globals) > 0) ) {
    fprintf(fp,"  virtual int            %s_position() const { return %d; }\n", name, position);
    MemInterface *mem_interface = oper._interface->is_MemInterface();
    const char *base = mem_interface->_base;
    const char *disp = mem_interface->_disp;
    if( emit_position && (strcmp(name,"base") == 0)
        && base != NULL && is_regI(base, oper, globals)
        && disp != NULL && is_conP(disp, oper, globals) ) {
      // Found a memory access using a constant pointer for a displacement
      // and a base register containing an integer offset.
      // In this case the base and disp are reversed with respect to what
      // is expected by MachNode::get_base_and_disp() and MachNode::adr_type().
      // Provide a non-NULL return for disp_as_type() that will allow adr_type()
      // to correctly compute the access type for alias analysis.
      //
      // See BugId 4796752, operand indOffset32X in x86_32.ad
      int idx = rep_var_to_constant_index(disp, oper, globals);
      fprintf(fp,"  virtual const TypePtr *disp_as_type() const { return _c%d; }\n", idx);
    }
  }
}

//
// Construct the method to copy _idx, inputs and operands to new node.
static void define_fill_new_machnode(bool used, FILE *fp_cpp) {
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "// Copy _idx, inputs and operands to new node\n");
  fprintf(fp_cpp, "void MachNode::fill_new_machnode(MachNode* node) const {\n");
  if( !used ) {
    fprintf(fp_cpp, "  // This architecture does not have cisc or short branch instructions\n");
    fprintf(fp_cpp, "  ShouldNotCallThis();\n");
    fprintf(fp_cpp, "}\n");
  } else {
    // New node must use same node index for access through allocator's tables
    fprintf(fp_cpp, "  // New node must use same node index\n");
    fprintf(fp_cpp, "  node->set_idx( _idx );\n");
    // Copy machine-independent inputs
    fprintf(fp_cpp, "  // Copy machine-independent inputs\n");
    fprintf(fp_cpp, "  for( uint j = 0; j < req(); j++ ) {\n");
    fprintf(fp_cpp, "    node->add_req(in(j));\n");
    fprintf(fp_cpp, "  }\n");
    // Copy machine operands to new MachNode
    fprintf(fp_cpp, "  // Copy my operands, except for cisc position\n");
    fprintf(fp_cpp, "  int nopnds = num_opnds();\n");
    fprintf(fp_cpp, "  assert( node->num_opnds() == (uint)nopnds, \"Must have same number of operands\");\n");
    fprintf(fp_cpp, "  MachOper **to = node->_opnds;\n");
    fprintf(fp_cpp, "  for( int i = 0; i < nopnds; i++ ) {\n");
    fprintf(fp_cpp, "    if( i != cisc_operand() ) \n");
    fprintf(fp_cpp, "      to[i] = _opnds[i]->clone();\n");
    fprintf(fp_cpp, "  }\n");
    fprintf(fp_cpp, "}\n");
  }
  fprintf(fp_cpp, "\n");
}

//------------------------------defineClasses----------------------------------
// Define members of MachNode and MachOper classes based on
// operand and instruction lists
void ArchDesc::defineClasses(FILE *fp) {

  // Define the contents of an array containing the machine register names
  defineRegNames(fp, _register);
  // Define an array containing the machine register encoding values
  defineRegEncodes(fp, _register);
  // Generate an enumeration of user-defined register classes
  // and a list of register masks, one for each class.
  // Only define the RegMask value objects in the expand file.
  // Declare each as an extern const RegMask ...; in ad_<arch>.hpp
  declare_register_masks(_HPP_file._fp);
  // build_register_masks(fp);
  build_register_masks(_CPP_EXPAND_file._fp);
  // Define the pipe_classes
  build_pipe_classes(_CPP_PIPELINE_file._fp);

  // Generate Machine Classes for each operand defined in AD file
  fprintf(fp,"\n");
  fprintf(fp,"\n");
  fprintf(fp,"//------------------Define classes derived from MachOper---------------------\n");
  // Iterate through all operands
  _operands.reset();
  OperandForm *oper;
  for( ; (oper = (OperandForm*)_operands.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( oper->ideal_only() ) continue;
    // !!!!!
    // The declaration of labelOper is in machine-independent file: machnode
    if ( strcmp(oper->_ident,"label") == 0 ) {
      defineIn_RegMask(_CPP_MISC_file._fp, _globalNames, *oper);

      fprintf(fp,"MachOper  *%sOper::clone() const {\n", oper->_ident);
      fprintf(fp,"  return  new %sOper(_label, _block_num);\n", oper->_ident);
      fprintf(fp,"}\n");

      fprintf(fp,"uint %sOper::opcode() const { return %s; }\n",
              oper->_ident, machOperEnum(oper->_ident));
      // // Currently all XXXOper::Hash() methods are identical (990820)
      // define_hash(fp, oper->_ident);
      // // Currently all XXXOper::Cmp() methods are identical (990820)
      // define_cmp(fp, oper->_ident);
      fprintf(fp,"\n");

      continue;
    }

    // The declaration of methodOper is in machine-independent file: machnode
    if ( strcmp(oper->_ident,"method") == 0 ) {
      defineIn_RegMask(_CPP_MISC_file._fp, _globalNames, *oper);

      fprintf(fp,"MachOper  *%sOper::clone() const {\n", oper->_ident);
      fprintf(fp,"  return  new %sOper(_method);\n", oper->_ident);
      fprintf(fp,"}\n");

      fprintf(fp,"uint %sOper::opcode() const { return %s; }\n",
              oper->_ident, machOperEnum(oper->_ident));
      // // Currently all XXXOper::Hash() methods are identical (990820)
      // define_hash(fp, oper->_ident);
      // // Currently all XXXOper::Cmp() methods are identical (990820)
      // define_cmp(fp, oper->_ident);
      fprintf(fp,"\n");

      continue;
    }

    defineIn_RegMask(fp, _globalNames, *oper);
    defineClone(_CPP_CLONE_file._fp, _globalNames, *oper);
    // // Currently all XXXOper::Hash() methods are identical (990820)
    // define_hash(fp, oper->_ident);
    // // Currently all XXXOper::Cmp() methods are identical (990820)
    // define_cmp(fp, oper->_ident);

    // side-call to generate output that used to be in the header file:
    extern void gen_oper_format(FILE *fp, FormDict &globals, OperandForm &oper, bool for_c_file);
    gen_oper_format(_CPP_FORMAT_file._fp, _globalNames, *oper, true);

  }


  // Generate Machine Classes for each instruction defined in AD file
  fprintf(fp,"//------------------Define members for classes derived from MachNode----------\n");
  // Output the definitions for out_RegMask() // & kill_RegMask()
  _instructions.reset();
  InstructForm *instr;
  MachNodeForm *machnode;
  for( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( instr->ideal_only() ) continue;

    defineOut_RegMask(_CPP_MISC_file._fp, instr->_ident, reg_mask(*instr));
  }

  bool used = false;
  // Output the definitions for expand rules & peephole rules
  _instructions.reset();
  for( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( instr->ideal_only() ) continue;
    // If there are multiple defs/kills, or an explicit expand rule, build rule
    if( instr->expands() || instr->needs_projections() ||
        instr->has_temps() ||
        instr->is_mach_constant() ||
        instr->needs_constant_base() ||
        (instr->_matrule != NULL &&
         instr->num_opnds() != instr->num_unique_opnds()) )
      defineExpand(_CPP_EXPAND_file._fp, instr);
    // If there is an explicit peephole rule, build it
    if ( instr->peepholes() )
      definePeephole(_CPP_PEEPHOLE_file._fp, instr);

    // Output code to convert to the cisc version, if applicable
    used |= instr->define_cisc_version(*this, fp);

    // Output code to convert to the short branch version, if applicable
    used |= instr->define_short_branch_methods(*this, fp);
  }

  // Construct the method called by cisc_version() to copy inputs and operands.
  define_fill_new_machnode(used, fp);

  // Output the definitions for labels
  _instructions.reset();
  while( (instr = (InstructForm*)_instructions.iter()) != NULL ) {
    // Ensure this is a machine-world instruction
    if ( instr->ideal_only() ) continue;

    // Access the fields for operand Label
    int label_position = instr->label_position();
    if( label_position != -1 ) {
      // Set the label
      fprintf(fp,"void %sNode::label_set( Label* label, uint block_num ) {\n", instr->_ident);
      fprintf(fp,"  labelOper* oper  = (labelOper*)(opnd_array(%d));\n",
              label_position );
      fprintf(fp,"  oper->_label     = label;\n");
      fprintf(fp,"  oper->_block_num = block_num;\n");
      fprintf(fp,"}\n");
      // Save the label
      fprintf(fp,"void %sNode::save_label( Label** label, uint* block_num ) {\n", instr->_ident);
      fprintf(fp,"  labelOper* oper  = (labelOper*)(opnd_array(%d));\n",
              label_position );
      fprintf(fp,"  *label = oper->_label;\n");
      fprintf(fp,"  *block_num = oper->_block_num;\n");
      fprintf(fp,"}\n");
    }
  }

  // Output the definitions for methods
  _instructions.reset();
  while( (instr = (InstructForm*)_instructions.iter()) != NULL ) {
    // Ensure this is a machine-world instruction
    if ( instr->ideal_only() ) continue;

    // Access the fields for operand Label
    int method_position = instr->method_position();
    if( method_position != -1 ) {
      // Access the method's address
      fprintf(fp,"void %sNode::method_set( intptr_t method ) {\n", instr->_ident);
      fprintf(fp,"  ((methodOper*)opnd_array(%d))->_method = method;\n",
              method_position );
      fprintf(fp,"}\n");
      fprintf(fp,"\n");
    }
  }

  // Define this instruction's number of relocation entries, base is '0'
  _instructions.reset();
  while( (instr = (InstructForm*)_instructions.iter()) != NULL ) {
    // Output the definition for number of relocation entries
    uint reloc_size = instr->reloc(_globalNames);
    if ( reloc_size != 0 ) {
      fprintf(fp,"int %sNode::reloc() const {\n", instr->_ident);
      fprintf(fp,"  return %d;\n", reloc_size);
      fprintf(fp,"}\n");
      fprintf(fp,"\n");
    }
  }
  fprintf(fp,"\n");

  // Output the definitions for code generation
  //
  // address  ___Node::emit(address ptr, PhaseRegAlloc *ra_) const {
  //   // ...  encoding defined by user
  //   return ptr;
  // }
  //
  _instructions.reset();
  for( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( instr->ideal_only() ) continue;

    if (instr->_insencode) {
      if (instr->postalloc_expands()) {
        // Don't write this to _CPP_EXPAND_file, as the code generated calls C-code
        // from code sections in ad file that is dumped to fp.
        define_postalloc_expand(fp, *instr);
      } else {
        defineEmit(fp, *instr);
      }
    }
    if (instr->is_mach_constant()) defineEvalConstant(fp, *instr);
    if (instr->_size)              defineSize        (fp, *instr);

    // side-call to generate output that used to be in the header file:
    extern void gen_inst_format(FILE *fp, FormDict &globals, InstructForm &oper, bool for_c_file);
    gen_inst_format(_CPP_FORMAT_file._fp, _globalNames, *instr, true);
  }

  // Output the definitions for alias analysis
  _instructions.reset();
  for( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( instr->ideal_only() ) continue;

    // Analyze machine instructions that either USE or DEF memory.
    int memory_operand = instr->memory_operand(_globalNames);

    if ( memory_operand != InstructForm::NO_MEMORY_OPERAND ) {
      if( memory_operand == InstructForm::MANY_MEMORY_OPERANDS ) {
        fprintf(fp,"const TypePtr *%sNode::adr_type() const { return TypePtr::BOTTOM; }\n", instr->_ident);
        fprintf(fp,"const MachOper* %sNode::memory_operand() const { return (MachOper*)-1; }\n", instr->_ident);
      } else {
        fprintf(fp,"const MachOper* %sNode::memory_operand() const { return _opnds[%d]; }\n", instr->_ident, memory_operand);
  }
    }
  }

  // Get the length of the longest identifier
  int max_ident_len = 0;
  _instructions.reset();

  for ( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
    if (instr->_ins_pipe && _pipeline->_classlist.search(instr->_ins_pipe)) {
      int ident_len = (int)strlen(instr->_ident);
      if( max_ident_len < ident_len )
        max_ident_len = ident_len;
    }
  }

  // Emit specifically for Node(s)
  fprintf(_CPP_PIPELINE_file._fp, "const Pipeline * %*s::pipeline_class() { return %s; }\n",
    max_ident_len, "Node", _pipeline ? "(&pipeline_class_Zero_Instructions)" : "NULL");
  fprintf(_CPP_PIPELINE_file._fp, "const Pipeline * %*s::pipeline() const { return %s; }\n",
    max_ident_len, "Node", _pipeline ? "(&pipeline_class_Zero_Instructions)" : "NULL");
  fprintf(_CPP_PIPELINE_file._fp, "\n");

  fprintf(_CPP_PIPELINE_file._fp, "const Pipeline * %*s::pipeline_class() { return %s; }\n",
    max_ident_len, "MachNode", _pipeline ? "(&pipeline_class_Unknown_Instructions)" : "NULL");
  fprintf(_CPP_PIPELINE_file._fp, "const Pipeline * %*s::pipeline() const { return pipeline_class(); }\n",
    max_ident_len, "MachNode");
  fprintf(_CPP_PIPELINE_file._fp, "\n");

  // Output the definitions for machine node specific pipeline data
  _machnodes.reset();

  if (_pipeline != NULL) {
    for ( ; (machnode = (MachNodeForm*)_machnodes.iter()) != NULL; ) {
      fprintf(_CPP_PIPELINE_file._fp, "const Pipeline * %sNode::pipeline() const { return (&pipeline_class_%03d); }\n",
              machnode->_ident, ((class PipeClassForm *)_pipeline->_classdict[machnode->_machnode_pipe])->_num);
    }
  }

  fprintf(_CPP_PIPELINE_file._fp, "\n");

  // Output the definitions for instruction pipeline static data references
  _instructions.reset();

  if (_pipeline != NULL) {
    for ( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
      if (instr->_ins_pipe && _pipeline->_classlist.search(instr->_ins_pipe)) {
        fprintf(_CPP_PIPELINE_file._fp, "\n");
        fprintf(_CPP_PIPELINE_file._fp, "const Pipeline * %*sNode::pipeline_class() { return (&pipeline_class_%03d); }\n",
                max_ident_len, instr->_ident, ((class PipeClassForm *)_pipeline->_classdict[instr->_ins_pipe])->_num);
        fprintf(_CPP_PIPELINE_file._fp, "const Pipeline * %*sNode::pipeline() const { return (&pipeline_class_%03d); }\n",
                max_ident_len, instr->_ident, ((class PipeClassForm *)_pipeline->_classdict[instr->_ins_pipe])->_num);
      }
    }
  }
}


// -------------------------------- maps ------------------------------------

// Information needed to generate the ReduceOp mapping for the DFA
class OutputReduceOp : public OutputMap {
public:
  OutputReduceOp(FILE *hpp, FILE *cpp, FormDict &globals, ArchDesc &AD)
    : OutputMap(hpp, cpp, globals, AD, "reduceOp") {};

  void declaration() { fprintf(_hpp, "extern const int   reduceOp[];\n"); }
  void definition()  { fprintf(_cpp, "const        int   reduceOp[] = {\n"); }
  void closing()     { fprintf(_cpp, "  0 // no trailing comma\n");
                       OutputMap::closing();
  }
  void map(OpClassForm &opc)  {
    const char *reduce = opc._ident;
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
  void map(OperandForm &oper) {
    // Most operands without match rules, e.g.  eFlagsReg, do not have a result operand
    const char *reduce = (oper._matrule ? oper.reduce_result() : NULL);
    // operand stackSlot does not have a match rule, but produces a stackSlot
    if( oper.is_user_name_for_sReg() != Form::none ) reduce = oper.reduce_result();
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
  void map(InstructForm &inst) {
    const char *reduce = (inst._matrule ? inst.reduce_result() : NULL);
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
  void map(char         *reduce) {
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
};

// Information needed to generate the LeftOp mapping for the DFA
class OutputLeftOp : public OutputMap {
public:
  OutputLeftOp(FILE *hpp, FILE *cpp, FormDict &globals, ArchDesc &AD)
    : OutputMap(hpp, cpp, globals, AD, "leftOp") {};

  void declaration() { fprintf(_hpp, "extern const int   leftOp[];\n"); }
  void definition()  { fprintf(_cpp, "const        int   leftOp[] = {\n"); }
  void closing()     { fprintf(_cpp, "  0 // no trailing comma\n");
                       OutputMap::closing();
  }
  void map(OpClassForm &opc)  { fprintf(_cpp, "  0"); }
  void map(OperandForm &oper) {
    const char *reduce = oper.reduce_left(_globals);
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
  void map(char        *name) {
    const char *reduce = _AD.reduceLeft(name);
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
  void map(InstructForm &inst) {
    const char *reduce = inst.reduce_left(_globals);
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
};


// Information needed to generate the RightOp mapping for the DFA
class OutputRightOp : public OutputMap {
public:
  OutputRightOp(FILE *hpp, FILE *cpp, FormDict &globals, ArchDesc &AD)
    : OutputMap(hpp, cpp, globals, AD, "rightOp") {};

  void declaration() { fprintf(_hpp, "extern const int   rightOp[];\n"); }
  void definition()  { fprintf(_cpp, "const        int   rightOp[] = {\n"); }
  void closing()     { fprintf(_cpp, "  0 // no trailing comma\n");
                       OutputMap::closing();
  }
  void map(OpClassForm &opc)  { fprintf(_cpp, "  0"); }
  void map(OperandForm &oper) {
    const char *reduce = oper.reduce_right(_globals);
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
  void map(char        *name) {
    const char *reduce = _AD.reduceRight(name);
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
  void map(InstructForm &inst) {
    const char *reduce = inst.reduce_right(_globals);
    if( reduce )  fprintf(_cpp, "  %s_rule", reduce);
    else          fprintf(_cpp, "  0");
  }
};


// Information needed to generate the Rule names for the DFA
class OutputRuleName : public OutputMap {
public:
  OutputRuleName(FILE *hpp, FILE *cpp, FormDict &globals, ArchDesc &AD)
    : OutputMap(hpp, cpp, globals, AD, "ruleName") {};

  void declaration() { fprintf(_hpp, "extern const char *ruleName[];\n"); }
  void definition()  { fprintf(_cpp, "const char        *ruleName[] = {\n"); }
  void closing()     { fprintf(_cpp, "  \"invalid rule name\" // no trailing comma\n");
                       OutputMap::closing();
  }
  void map(OpClassForm &opc)  { fprintf(_cpp, "  \"%s\"", _AD.machOperEnum(opc._ident) ); }
  void map(OperandForm &oper) { fprintf(_cpp, "  \"%s\"", _AD.machOperEnum(oper._ident) ); }
  void map(char        *name) { fprintf(_cpp, "  \"%s\"", name ? name : "0"); }
  void map(InstructForm &inst){ fprintf(_cpp, "  \"%s\"", inst._ident ? inst._ident : "0"); }
};


// Information needed to generate the swallowed mapping for the DFA
class OutputSwallowed : public OutputMap {
public:
  OutputSwallowed(FILE *hpp, FILE *cpp, FormDict &globals, ArchDesc &AD)
    : OutputMap(hpp, cpp, globals, AD, "swallowed") {};

  void declaration() { fprintf(_hpp, "extern const bool  swallowed[];\n"); }
  void definition()  { fprintf(_cpp, "const        bool  swallowed[] = {\n"); }
  void closing()     { fprintf(_cpp, "  false // no trailing comma\n");
                       OutputMap::closing();
  }
  void map(OperandForm &oper) { // Generate the entry for this opcode
    const char *swallowed = oper.swallowed(_globals) ? "true" : "false";
    fprintf(_cpp, "  %s", swallowed);
  }
  void map(OpClassForm &opc)  { fprintf(_cpp, "  false"); }
  void map(char        *name) { fprintf(_cpp, "  false"); }
  void map(InstructForm &inst){ fprintf(_cpp, "  false"); }
};


// Information needed to generate the decision array for instruction chain rule
class OutputInstChainRule : public OutputMap {
public:
  OutputInstChainRule(FILE *hpp, FILE *cpp, FormDict &globals, ArchDesc &AD)
    : OutputMap(hpp, cpp, globals, AD, "instruction_chain_rule") {};

  void declaration() { fprintf(_hpp, "extern const bool  instruction_chain_rule[];\n"); }
  void definition()  { fprintf(_cpp, "const        bool  instruction_chain_rule[] = {\n"); }
  void closing()     { fprintf(_cpp, "  false // no trailing comma\n");
                       OutputMap::closing();
  }
  void map(OpClassForm &opc)   { fprintf(_cpp, "  false"); }
  void map(OperandForm &oper)  { fprintf(_cpp, "  false"); }
  void map(char        *name)  { fprintf(_cpp, "  false"); }
  void map(InstructForm &inst) { // Check for simple chain rule
    const char *chain = inst.is_simple_chain_rule(_globals) ? "true" : "false";
    fprintf(_cpp, "  %s", chain);
  }
};


//---------------------------build_map------------------------------------
// Build  mapping from enumeration for densely packed operands
// TO result and child types.
void ArchDesc::build_map(OutputMap &map) {
  FILE         *fp_hpp = map.decl_file();
  FILE         *fp_cpp = map.def_file();
  int           idx    = 0;
  OperandForm  *op;
  OpClassForm  *opc;
  InstructForm *inst;

  // Construct this mapping
  map.declaration();
  fprintf(fp_cpp,"\n");
  map.definition();

  // Output the mapping for operands
  map.record_position(OutputMap::BEGIN_OPERANDS, idx );
  _operands.reset();
  for(; (op = (OperandForm*)_operands.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( op->ideal_only() )  continue;

    // Generate the entry for this opcode
    fprintf(fp_cpp, "  /* %4d */", idx); map.map(*op); fprintf(fp_cpp, ",\n");
    ++idx;
  };
  fprintf(fp_cpp, "  // last operand\n");

  // Place all user-defined operand classes into the mapping
  map.record_position(OutputMap::BEGIN_OPCLASSES, idx );
  _opclass.reset();
  for(; (opc = (OpClassForm*)_opclass.iter()) != NULL; ) {
    fprintf(fp_cpp, "  /* %4d */", idx); map.map(*opc); fprintf(fp_cpp, ",\n");
    ++idx;
  };
  fprintf(fp_cpp, "  // last operand class\n");

  // Place all internally defined operands into the mapping
  map.record_position(OutputMap::BEGIN_INTERNALS, idx );
  _internalOpNames.reset();
  char *name = NULL;
  for(; (name = (char *)_internalOpNames.iter()) != NULL; ) {
    fprintf(fp_cpp, "  /* %4d */", idx); map.map(name); fprintf(fp_cpp, ",\n");
    ++idx;
  };
  fprintf(fp_cpp, "  // last internally defined operand\n");

  // Place all user-defined instructions into the mapping
  if( map.do_instructions() ) {
    map.record_position(OutputMap::BEGIN_INSTRUCTIONS, idx );
    // Output all simple instruction chain rules first
    map.record_position(OutputMap::BEGIN_INST_CHAIN_RULES, idx );
    {
      _instructions.reset();
      for(; (inst = (InstructForm*)_instructions.iter()) != NULL; ) {
        // Ensure this is a machine-world instruction
        if ( inst->ideal_only() )  continue;
        if ( ! inst->is_simple_chain_rule(_globalNames) ) continue;
        if ( inst->rematerialize(_globalNames, get_registers()) ) continue;

        fprintf(fp_cpp, "  /* %4d */", idx); map.map(*inst); fprintf(fp_cpp, ",\n");
        ++idx;
      };
      map.record_position(OutputMap::BEGIN_REMATERIALIZE, idx );
      _instructions.reset();
      for(; (inst = (InstructForm*)_instructions.iter()) != NULL; ) {
        // Ensure this is a machine-world instruction
        if ( inst->ideal_only() )  continue;
        if ( ! inst->is_simple_chain_rule(_globalNames) ) continue;
        if ( ! inst->rematerialize(_globalNames, get_registers()) ) continue;

        fprintf(fp_cpp, "  /* %4d */", idx); map.map(*inst); fprintf(fp_cpp, ",\n");
        ++idx;
      };
      map.record_position(OutputMap::END_INST_CHAIN_RULES, idx );
    }
    // Output all instructions that are NOT simple chain rules
    {
      _instructions.reset();
      for(; (inst = (InstructForm*)_instructions.iter()) != NULL; ) {
        // Ensure this is a machine-world instruction
        if ( inst->ideal_only() )  continue;
        if ( inst->is_simple_chain_rule(_globalNames) ) continue;
        if ( ! inst->rematerialize(_globalNames, get_registers()) ) continue;

        fprintf(fp_cpp, "  /* %4d */", idx); map.map(*inst); fprintf(fp_cpp, ",\n");
        ++idx;
      };
      map.record_position(OutputMap::END_REMATERIALIZE, idx );
      _instructions.reset();
      for(; (inst = (InstructForm*)_instructions.iter()) != NULL; ) {
        // Ensure this is a machine-world instruction
        if ( inst->ideal_only() )  continue;
        if ( inst->is_simple_chain_rule(_globalNames) ) continue;
        if ( inst->rematerialize(_globalNames, get_registers()) ) continue;

        fprintf(fp_cpp, "  /* %4d */", idx); map.map(*inst); fprintf(fp_cpp, ",\n");
        ++idx;
      };
    }
    fprintf(fp_cpp, "  // last instruction\n");
    map.record_position(OutputMap::END_INSTRUCTIONS, idx );
  }
  // Finish defining table
  map.closing();
};


// Helper function for buildReduceMaps
char reg_save_policy(const char *calling_convention) {
  char callconv;

  if      (!strcmp(calling_convention, "NS"))  callconv = 'N';
  else if (!strcmp(calling_convention, "SOE")) callconv = 'E';
  else if (!strcmp(calling_convention, "SOC")) callconv = 'C';
  else if (!strcmp(calling_convention, "AS"))  callconv = 'A';
  else                                         callconv = 'Z';

  return callconv;
}

void ArchDesc::generate_needs_deep_clone_jvms(FILE *fp_cpp) {
  fprintf(fp_cpp, "bool Compile::needs_deep_clone_jvms() { return %s; }\n\n",
          _needs_deep_clone_jvms ? "true" : "false");
}

//---------------------------generate_assertion_checks-------------------
void ArchDesc::generate_adlc_verification(FILE *fp_cpp) {
  fprintf(fp_cpp, "\n");

  fprintf(fp_cpp, "#ifndef PRODUCT\n");
  fprintf(fp_cpp, "void Compile::adlc_verification() {\n");
  globalDefs().print_asserts(fp_cpp);
  fprintf(fp_cpp, "}\n");
  fprintf(fp_cpp, "#endif\n");
  fprintf(fp_cpp, "\n");
}

//---------------------------addSourceBlocks-----------------------------
void ArchDesc::addSourceBlocks(FILE *fp_cpp) {
  if (_source.count() > 0)
    _source.output(fp_cpp);

  generate_adlc_verification(fp_cpp);
}
//---------------------------addHeaderBlocks-----------------------------
void ArchDesc::addHeaderBlocks(FILE *fp_hpp) {
  if (_header.count() > 0)
    _header.output(fp_hpp);
}
//-------------------------addPreHeaderBlocks----------------------------
void ArchDesc::addPreHeaderBlocks(FILE *fp_hpp) {
  // Output #defines from definition block
  globalDefs().print_defines(fp_hpp);

  if (_pre_header.count() > 0)
    _pre_header.output(fp_hpp);
}

//---------------------------buildReduceMaps-----------------------------
// Build  mapping from enumeration for densely packed operands
// TO result and child types.
void ArchDesc::buildReduceMaps(FILE *fp_hpp, FILE *fp_cpp) {
  RegDef       *rdef;
  RegDef       *next;

  // The emit bodies currently require functions defined in the source block.

  // Build external declarations for mappings
  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "extern const char  register_save_policy[];\n");
  fprintf(fp_hpp, "extern const char  c_reg_save_policy[];\n");
  fprintf(fp_hpp, "extern const int   register_save_type[];\n");
  fprintf(fp_hpp, "\n");

  // Construct Save-Policy array
  fprintf(fp_cpp, "// Map from machine-independent register number to register_save_policy\n");
  fprintf(fp_cpp, "const        char register_save_policy[] = {\n");
  _register->reset_RegDefs();
  for( rdef = _register->iter_RegDefs(); rdef != NULL; rdef = next ) {
    next              = _register->iter_RegDefs();
    char policy       = reg_save_policy(rdef->_callconv);
    const char *comma = (next != NULL) ? "," : " // no trailing comma";
    fprintf(fp_cpp, "  '%c'%s // %s\n", policy, comma, rdef->_regname);
  }
  fprintf(fp_cpp, "};\n\n");

  // Construct Native Save-Policy array
  fprintf(fp_cpp, "// Map from machine-independent register number to c_reg_save_policy\n");
  fprintf(fp_cpp, "const        char c_reg_save_policy[] = {\n");
  _register->reset_RegDefs();
  for( rdef = _register->iter_RegDefs(); rdef != NULL; rdef = next ) {
    next        = _register->iter_RegDefs();
    char policy = reg_save_policy(rdef->_c_conv);
    const char *comma = (next != NULL) ? "," : " // no trailing comma";
    fprintf(fp_cpp, "  '%c'%s // %s\n", policy, comma, rdef->_regname);
  }
  fprintf(fp_cpp, "};\n\n");

  // Construct Register Save Type array
  fprintf(fp_cpp, "// Map from machine-independent register number to register_save_type\n");
  fprintf(fp_cpp, "const        int register_save_type[] = {\n");
  _register->reset_RegDefs();
  for( rdef = _register->iter_RegDefs(); rdef != NULL; rdef = next ) {
    next = _register->iter_RegDefs();
    const char *comma = (next != NULL) ? "," : " // no trailing comma";
    fprintf(fp_cpp, "  %s%s\n", rdef->_idealtype, comma);
  }
  fprintf(fp_cpp, "};\n\n");

  // Construct the table for reduceOp
  OutputReduceOp output_reduce_op(fp_hpp, fp_cpp, _globalNames, *this);
  build_map(output_reduce_op);
  // Construct the table for leftOp
  OutputLeftOp output_left_op(fp_hpp, fp_cpp, _globalNames, *this);
  build_map(output_left_op);
  // Construct the table for rightOp
  OutputRightOp output_right_op(fp_hpp, fp_cpp, _globalNames, *this);
  build_map(output_right_op);
  // Construct the table of rule names
  OutputRuleName output_rule_name(fp_hpp, fp_cpp, _globalNames, *this);
  build_map(output_rule_name);
  // Construct the boolean table for subsumed operands
  OutputSwallowed output_swallowed(fp_hpp, fp_cpp, _globalNames, *this);
  build_map(output_swallowed);
  // // // Preserve in case we decide to use this table instead of another
  //// Construct the boolean table for instruction chain rules
  //OutputInstChainRule output_inst_chain(fp_hpp, fp_cpp, _globalNames, *this);
  //build_map(output_inst_chain);

}


//---------------------------buildMachOperGenerator---------------------------

// Recurse through match tree, building path through corresponding state tree,
// Until we reach the constant we are looking for.
static void path_to_constant(FILE *fp, FormDict &globals,
                             MatchNode *mnode, uint idx) {
  if ( ! mnode) return;

  unsigned    position = 0;
  const char *result   = NULL;
  const char *name     = NULL;
  const char *optype   = NULL;

  // Base Case: access constant in ideal node linked to current state node
  // Each type of constant has its own access function
  if ( (mnode->_lChild == NULL) && (mnode->_rChild == NULL)
       && mnode->base_operand(position, globals, result, name, optype) ) {
    if (         strcmp(optype,"ConI") == 0 ) {
      fprintf(fp, "_leaf->get_int()");
    } else if ( (strcmp(optype,"ConP") == 0) ) {
      fprintf(fp, "_leaf->bottom_type()->is_ptr()");
    } else if ( (strcmp(optype,"ConN") == 0) ) {
      fprintf(fp, "_leaf->bottom_type()->is_narrowoop()");
    } else if ( (strcmp(optype,"ConNKlass") == 0) ) {
      fprintf(fp, "_leaf->bottom_type()->is_narrowklass()");
    } else if ( (strcmp(optype,"ConF") == 0) ) {
      fprintf(fp, "_leaf->getf()");
    } else if ( (strcmp(optype,"ConD") == 0) ) {
      fprintf(fp, "_leaf->getd()");
    } else if ( (strcmp(optype,"ConL") == 0) ) {
      fprintf(fp, "_leaf->get_long()");
    } else if ( (strcmp(optype,"Con")==0) ) {
      // !!!!! - Update if adding a machine-independent constant type
      fprintf(fp, "_leaf->get_int()");
      assert( false, "Unsupported constant type, pointer or indefinite");
    } else if ( (strcmp(optype,"Bool") == 0) ) {
      fprintf(fp, "_leaf->as_Bool()->_test._test");
    } else {
      assert( false, "Unsupported constant type");
    }
    return;
  }

  // If constant is in left child, build path and recurse
  uint lConsts = (mnode->_lChild) ? (mnode->_lChild->num_consts(globals) ) : 0;
  uint rConsts = (mnode->_rChild) ? (mnode->_rChild->num_consts(globals) ) : 0;
  if ( (mnode->_lChild) && (lConsts > idx) ) {
    fprintf(fp, "_kids[0]->");
    path_to_constant(fp, globals, mnode->_lChild, idx);
    return;
  }
  // If constant is in right child, build path and recurse
  if ( (mnode->_rChild) && (rConsts > (idx - lConsts) ) ) {
    idx = idx - lConsts;
    fprintf(fp, "_kids[1]->");
    path_to_constant(fp, globals, mnode->_rChild, idx);
    return;
  }
  assert( false, "ShouldNotReachHere()");
}

// Generate code that is executed when generating a specific Machine Operand
static void genMachOperCase(FILE *fp, FormDict &globalNames, ArchDesc &AD,
                            OperandForm &op) {
  const char *opName         = op._ident;
  const char *opEnumName     = AD.machOperEnum(opName);
  uint        num_consts     = op.num_consts(globalNames);

  // Generate the case statement for this opcode
  fprintf(fp, "  case %s:", opEnumName);
  fprintf(fp, "\n    return new %sOper(", opName);
  // Access parameters for constructor from the stat object
  //
  // Build access to condition code value
  if ( (num_consts > 0) ) {
    uint i = 0;
    path_to_constant(fp, globalNames, op._matrule, i);
    for ( i = 1; i < num_consts; ++i ) {
      fprintf(fp, ", ");
      path_to_constant(fp, globalNames, op._matrule, i);
    }
  }
  fprintf(fp, " );\n");
}


// Build switch to invoke "new" MachNode or MachOper
void ArchDesc::buildMachOperGenerator(FILE *fp_cpp) {
  int idx = 0;

  // Build switch to invoke 'new' for a specific MachOper
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp,
          "//------------------------- MachOper Generator ---------------\n");
  fprintf(fp_cpp,
          "// A switch statement on the dense-packed user-defined type system\n"
          "// that invokes 'new' on the corresponding class constructor.\n");
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "MachOper *State::MachOperGenerator");
  fprintf(fp_cpp, "(int opcode)");
  fprintf(fp_cpp, "{\n");
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "  switch(opcode) {\n");

  // Place all user-defined operands into the mapping
  _operands.reset();
  int  opIndex = 0;
  OperandForm *op;
  for( ; (op =  (OperandForm*)_operands.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( op->ideal_only() )  continue;

    genMachOperCase(fp_cpp, _globalNames, *this, *op);
  };

  // Do not iterate over operand classes for the  operand generator!!!

  // Place all internal operands into the mapping
  _internalOpNames.reset();
  const char *iopn;
  for( ; (iopn =  _internalOpNames.iter()) != NULL; ) {
    const char *opEnumName = machOperEnum(iopn);
    // Generate the case statement for this opcode
    fprintf(fp_cpp, "  case %s:", opEnumName);
    fprintf(fp_cpp, "    return NULL;\n");
  };

  // Generate the default case for switch(opcode)
  fprintf(fp_cpp, "  \n");
  fprintf(fp_cpp, "  default:\n");
  fprintf(fp_cpp, "    fprintf(stderr, \"Default MachOper Generator invoked for: \\n\");\n");
  fprintf(fp_cpp, "    fprintf(stderr, \"   opcode = %cd\\n\", opcode);\n", '%');
  fprintf(fp_cpp, "    break;\n");
  fprintf(fp_cpp, "  }\n");

  // Generate the closing for method Matcher::MachOperGenerator
  fprintf(fp_cpp, "  return NULL;\n");
  fprintf(fp_cpp, "};\n");
}


//---------------------------buildMachNode-------------------------------------
// Build a new MachNode, for MachNodeGenerator or cisc-spilling
void ArchDesc::buildMachNode(FILE *fp_cpp, InstructForm *inst, const char *indent) {
  const char *opType  = NULL;
  const char *opClass = inst->_ident;

  // Create the MachNode object
  fprintf(fp_cpp, "%s %sNode *node = new %sNode();\n",indent, opClass,opClass);

  if ( (inst->num_post_match_opnds() != 0) ) {
    // Instruction that contains operands which are not in match rule.
    //
    // Check if the first post-match component may be an interesting def
    bool           dont_care = false;
    ComponentList &comp_list = inst->_components;
    Component     *comp      = NULL;
    comp_list.reset();
    if ( comp_list.match_iter() != NULL )    dont_care = true;

    // Insert operands that are not in match-rule.
    // Only insert a DEF if the do_care flag is set
    comp_list.reset();
    while ( (comp = comp_list.post_match_iter()) ) {
      // Check if we don't care about DEFs or KILLs that are not USEs
      if ( dont_care && (! comp->isa(Component::USE)) ) {
        continue;
      }
      dont_care = true;
      // For each operand not in the match rule, call MachOperGenerator
      // with the enum for the opcode that needs to be built.
      ComponentList clist = inst->_components;
      int         index  = clist.operand_position(comp->_name, comp->_usedef, inst);
      const char *opcode = machOperEnum(comp->_type);
      fprintf(fp_cpp, "%s node->set_opnd_array(%d, ", indent, index);
      fprintf(fp_cpp, "MachOperGenerator(%s));\n", opcode);
      }
  }
  else if ( inst->is_chain_of_constant(_globalNames, opType) ) {
    // An instruction that chains from a constant!
    // In this case, we need to subsume the constant into the node
    // at operand position, oper_input_base().
    //
    // Fill in the constant
    fprintf(fp_cpp, "%s node->_opnd_array[%d] = ", indent,
            inst->oper_input_base(_globalNames));
    // #####
    // Check for multiple constants and then fill them in.
    // Just like MachOperGenerator
    const char *opName = inst->_matrule->_rChild->_opType;
    fprintf(fp_cpp, "new %sOper(", opName);
    // Grab operand form
    OperandForm *op = (_globalNames[opName])->is_operand();
    // Look up the number of constants
    uint num_consts = op->num_consts(_globalNames);
    if ( (num_consts > 0) ) {
      uint i = 0;
      path_to_constant(fp_cpp, _globalNames, op->_matrule, i);
      for ( i = 1; i < num_consts; ++i ) {
        fprintf(fp_cpp, ", ");
        path_to_constant(fp_cpp, _globalNames, op->_matrule, i);
      }
    }
    fprintf(fp_cpp, " );\n");
    // #####
  }

  // Fill in the bottom_type where requested
  if (inst->captures_bottom_type(_globalNames)) {
    if (strncmp("MachCall", inst->mach_base_class(_globalNames), strlen("MachCall"))) {
      fprintf(fp_cpp, "%s node->_bottom_type = _leaf->bottom_type();\n", indent);
    }
  }
  if( inst->is_ideal_if() ) {
    fprintf(fp_cpp, "%s node->_prob = _leaf->as_If()->_prob;\n", indent);
    fprintf(fp_cpp, "%s node->_fcnt = _leaf->as_If()->_fcnt;\n", indent);
  }
  if (inst->is_ideal_halt()) {
    fprintf(fp_cpp, "%s node->_halt_reason = _leaf->as_Halt()->_halt_reason;\n", indent);
    fprintf(fp_cpp, "%s node->_reachable   = _leaf->as_Halt()->_reachable;\n", indent);
  }
  if (inst->is_ideal_jump()) {
    fprintf(fp_cpp, "%s node->_probs = _leaf->as_Jump()->_probs;\n", indent);
  }
  if( inst->is_ideal_fastlock() ) {
    fprintf(fp_cpp, "%s node->_rtm_counters = _leaf->as_FastLock()->rtm_counters();\n", indent);
    fprintf(fp_cpp, "%s node->_stack_rtm_counters = _leaf->as_FastLock()->stack_rtm_counters();\n", indent);
  }

}

//---------------------------declare_cisc_version------------------------------
// Build CISC version of this instruction
void InstructForm::declare_cisc_version(ArchDesc &AD, FILE *fp_hpp) {
  if( AD.can_cisc_spill() ) {
    InstructForm *inst_cisc = cisc_spill_alternate();
    if (inst_cisc != NULL) {
      fprintf(fp_hpp, "  virtual int            cisc_operand() const { return %d; }\n", cisc_spill_operand());
      fprintf(fp_hpp, "  virtual MachNode      *cisc_version(int offset);\n");
      fprintf(fp_hpp, "  virtual void           use_cisc_RegMask();\n");
      fprintf(fp_hpp, "  virtual const RegMask *cisc_RegMask() const { return _cisc_RegMask; }\n");
    }
  }
}

//---------------------------define_cisc_version-------------------------------
// Build CISC version of this instruction
bool InstructForm::define_cisc_version(ArchDesc &AD, FILE *fp_cpp) {
  InstructForm *inst_cisc = this->cisc_spill_alternate();
  if( AD.can_cisc_spill() && (inst_cisc != NULL) ) {
    const char   *name      = inst_cisc->_ident;
    assert( inst_cisc->num_opnds() == this->num_opnds(), "Must have same number of operands");
    OperandForm *cisc_oper = AD.cisc_spill_operand();
    assert( cisc_oper != NULL, "insanity check");
    const char *cisc_oper_name  = cisc_oper->_ident;
    assert( cisc_oper_name != NULL, "insanity check");
    //
    // Set the correct reg_mask_or_stack for the cisc operand
    fprintf(fp_cpp, "\n");
    fprintf(fp_cpp, "void %sNode::use_cisc_RegMask() {\n", this->_ident);
    // Lookup the correct reg_mask_or_stack
    const char *reg_mask_name = cisc_reg_mask_name();
    fprintf(fp_cpp, "  _cisc_RegMask = &STACK_OR_%s;\n", reg_mask_name);
    fprintf(fp_cpp, "}\n");
    //
    // Construct CISC version of this instruction
    fprintf(fp_cpp, "\n");
    fprintf(fp_cpp, "// Build CISC version of this instruction\n");
    fprintf(fp_cpp, "MachNode *%sNode::cisc_version(int offset) {\n", this->_ident);
    // Create the MachNode object
    fprintf(fp_cpp, "  %sNode *node = new %sNode();\n", name, name);
    // Fill in the bottom_type where requested
    if ( this->captures_bottom_type(AD.globalNames()) ) {
      fprintf(fp_cpp, "  node->_bottom_type = bottom_type();\n");
    }

    uint cur_num_opnds = num_opnds();
    if (cur_num_opnds > 1 && cur_num_opnds != num_unique_opnds()) {
      fprintf(fp_cpp,"  node->_num_opnds = %d;\n", num_unique_opnds());
    }

    fprintf(fp_cpp, "\n");
    fprintf(fp_cpp, "  // Copy _idx, inputs and operands to new node\n");
    fprintf(fp_cpp, "  fill_new_machnode(node);\n");
    // Construct operand to access [stack_pointer + offset]
    fprintf(fp_cpp, "  // Construct operand to access [stack_pointer + offset]\n");
    fprintf(fp_cpp, "  node->set_opnd_array(cisc_operand(), new %sOper(offset));\n", cisc_oper_name);
    fprintf(fp_cpp, "\n");

    // Return result and exit scope
    fprintf(fp_cpp, "  return node;\n");
    fprintf(fp_cpp, "}\n");
    fprintf(fp_cpp, "\n");
    return true;
  }
  return false;
}

//---------------------------declare_short_branch_methods----------------------
// Build prototypes for short branch methods
void InstructForm::declare_short_branch_methods(FILE *fp_hpp) {
  if (has_short_branch_form()) {
    fprintf(fp_hpp, "  virtual MachNode      *short_branch_version();\n");
  }
}

//---------------------------define_short_branch_methods-----------------------
// Build definitions for short branch methods
bool InstructForm::define_short_branch_methods(ArchDesc &AD, FILE *fp_cpp) {
  if (has_short_branch_form()) {
    InstructForm *short_branch = short_branch_form();
    const char   *name         = short_branch->_ident;

    // Construct short_branch_version() method.
    fprintf(fp_cpp, "// Build short branch version of this instruction\n");
    fprintf(fp_cpp, "MachNode *%sNode::short_branch_version() {\n", this->_ident);
    // Create the MachNode object
    fprintf(fp_cpp, "  %sNode *node = new %sNode();\n", name, name);
    if( is_ideal_if() ) {
      fprintf(fp_cpp, "  node->_prob = _prob;\n");
      fprintf(fp_cpp, "  node->_fcnt = _fcnt;\n");
    }
    // Fill in the bottom_type where requested
    if ( this->captures_bottom_type(AD.globalNames()) ) {
      fprintf(fp_cpp, "  node->_bottom_type = bottom_type();\n");
    }

    fprintf(fp_cpp, "\n");
    // Short branch version must use same node index for access
    // through allocator's tables
    fprintf(fp_cpp, "  // Copy _idx, inputs and operands to new node\n");
    fprintf(fp_cpp, "  fill_new_machnode(node);\n");

    // Return result and exit scope
    fprintf(fp_cpp, "  return node;\n");
    fprintf(fp_cpp, "}\n");
    fprintf(fp_cpp,"\n");
    return true;
  }
  return false;
}


//---------------------------buildMachNodeGenerator----------------------------
// Build switch to invoke appropriate "new" MachNode for an opcode
void ArchDesc::buildMachNodeGenerator(FILE *fp_cpp) {

  // Build switch to invoke 'new' for a specific MachNode
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp,
          "//------------------------- MachNode Generator ---------------\n");
  fprintf(fp_cpp,
          "// A switch statement on the dense-packed user-defined type system\n"
          "// that invokes 'new' on the corresponding class constructor.\n");
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "MachNode *State::MachNodeGenerator");
  fprintf(fp_cpp, "(int opcode)");
  fprintf(fp_cpp, "{\n");
  fprintf(fp_cpp, "  switch(opcode) {\n");

  // Provide constructor for all user-defined instructions
  _instructions.reset();
  int  opIndex = operandFormCount();
  InstructForm *inst;
  for( ; (inst = (InstructForm*)_instructions.iter()) != NULL; ) {
    // Ensure that matrule is defined.
    if ( inst->_matrule == NULL ) continue;

    int         opcode  = opIndex++;
    const char *opClass = inst->_ident;
    char       *opType  = NULL;

    // Generate the case statement for this instruction
    fprintf(fp_cpp, "  case %s_rule:", opClass);

    // Start local scope
    fprintf(fp_cpp, " {\n");
    // Generate code to construct the new MachNode
    buildMachNode(fp_cpp, inst, "     ");
    // Return result and exit scope
    fprintf(fp_cpp, "      return node;\n");
    fprintf(fp_cpp, "    }\n");
  }

  // Generate the default case for switch(opcode)
  fprintf(fp_cpp, "  \n");
  fprintf(fp_cpp, "  default:\n");
  fprintf(fp_cpp, "    fprintf(stderr, \"Default MachNode Generator invoked for: \\n\");\n");
  fprintf(fp_cpp, "    fprintf(stderr, \"   opcode = %cd\\n\", opcode);\n", '%');
  fprintf(fp_cpp, "    break;\n");
  fprintf(fp_cpp, "  };\n");

  // Generate the closing for method Matcher::MachNodeGenerator
  fprintf(fp_cpp, "  return NULL;\n");
  fprintf(fp_cpp, "}\n");
}


//---------------------------buildInstructMatchCheck--------------------------
// Output the method to Matcher which checks whether or not a specific
// instruction has a matching rule for the host architecture.
void ArchDesc::buildInstructMatchCheck(FILE *fp_cpp) const {
  fprintf(fp_cpp, "\n\n");
  fprintf(fp_cpp, "const bool Matcher::has_match_rule(int opcode) {\n");
  fprintf(fp_cpp, "  assert(_last_machine_leaf < opcode && opcode < _last_opcode, \"opcode in range\");\n");
  fprintf(fp_cpp, "  return _hasMatchRule[opcode];\n");
  fprintf(fp_cpp, "}\n\n");

  fprintf(fp_cpp, "const bool Matcher::_hasMatchRule[_last_opcode] = {\n");
  int i;
  for (i = 0; i < _last_opcode - 1; i++) {
    fprintf(fp_cpp, "    %-5s,  // %s\n",
            _has_match_rule[i] ? "true" : "false",
            NodeClassNames[i]);
  }
  fprintf(fp_cpp, "    %-5s   // %s\n",
          _has_match_rule[i] ? "true" : "false",
          NodeClassNames[i]);
  fprintf(fp_cpp, "};\n");
}

//---------------------------buildFrameMethods---------------------------------
// Output the methods to Matcher which specify frame behavior
void ArchDesc::buildFrameMethods(FILE *fp_cpp) {
  fprintf(fp_cpp,"\n\n");
  // Sync Stack Slots
  fprintf(fp_cpp,"int Compile::sync_stack_slots() const { return %s; }\n\n",
          _frame->_sync_stack_slots);
  // Java Stack Alignment
  fprintf(fp_cpp,"uint Matcher::stack_alignment_in_bytes() { return %s; }\n\n",
          _frame->_alignment);
  // Java Return Address Location
  fprintf(fp_cpp,"OptoReg::Name Matcher::return_addr() const {");
  if (_frame->_return_addr_loc) {
    fprintf(fp_cpp," return OptoReg::Name(%s_num); }\n\n",
            _frame->_return_addr);
  }
  else {
    fprintf(fp_cpp," return OptoReg::stack2reg(%s); }\n\n",
            _frame->_return_addr);
  }
  // varargs C out slots killed
  fprintf(fp_cpp,"uint Compile::varargs_C_out_slots_killed() const ");
  fprintf(fp_cpp,"{ return %s; }\n\n", _frame->_varargs_C_out_slots_killed);
  // Java Return Value Location
  fprintf(fp_cpp,"OptoRegPair Matcher::return_value(uint ideal_reg) {\n");
  fprintf(fp_cpp,"%s\n", _frame->_return_value);
  fprintf(fp_cpp,"}\n\n");
  // Native Return Value Location
  fprintf(fp_cpp,"OptoRegPair Matcher::c_return_value(uint ideal_reg) {\n");
  fprintf(fp_cpp,"%s\n", _frame->_c_return_value);
  fprintf(fp_cpp,"}\n\n");

  // Inline Cache Register, mask definition, and encoding
  fprintf(fp_cpp,"OptoReg::Name Matcher::inline_cache_reg() {");
  fprintf(fp_cpp," return OptoReg::Name(%s_num); }\n\n",
          _frame->_inline_cache_reg);
  fprintf(fp_cpp,"int Matcher::inline_cache_reg_encode() {");
  fprintf(fp_cpp," return _regEncode[inline_cache_reg()]; }\n\n");

  // Interpreter's Frame Pointer Register
  fprintf(fp_cpp,"OptoReg::Name Matcher::interpreter_frame_pointer_reg() {");
  if (_frame->_interpreter_frame_pointer_reg == NULL)
    fprintf(fp_cpp," return OptoReg::Bad; }\n\n");
  else
    fprintf(fp_cpp," return OptoReg::Name(%s_num); }\n\n",
            _frame->_interpreter_frame_pointer_reg);

  // Frame Pointer definition
  /* CNC - I can not contemplate having a different frame pointer between
     Java and native code; makes my head hurt to think about it.
  fprintf(fp_cpp,"OptoReg::Name Matcher::frame_pointer() const {");
  fprintf(fp_cpp," return OptoReg::Name(%s_num); }\n\n",
          _frame->_frame_pointer);
  */
  // (Native) Frame Pointer definition
  fprintf(fp_cpp,"OptoReg::Name Matcher::c_frame_pointer() const {");
  fprintf(fp_cpp," return OptoReg::Name(%s_num); }\n\n",
          _frame->_frame_pointer);

  // Number of callee-save + always-save registers for calling convention
  fprintf(fp_cpp, "// Number of callee-save + always-save registers\n");
  fprintf(fp_cpp, "int  Matcher::number_of_saved_registers() {\n");
  RegDef *rdef;
  int nof_saved_registers = 0;
  _register->reset_RegDefs();
  while( (rdef = _register->iter_RegDefs()) != NULL ) {
    if( !strcmp(rdef->_callconv, "SOE") ||  !strcmp(rdef->_callconv, "AS") )
      ++nof_saved_registers;
  }
  fprintf(fp_cpp, "  return %d;\n", nof_saved_registers);
  fprintf(fp_cpp, "};\n\n");
}




static int PrintAdlcCisc = 0;
//---------------------------identify_cisc_spilling----------------------------
// Get info for the CISC_oracle and MachNode::cisc_version()
void ArchDesc::identify_cisc_spill_instructions() {

  if (_frame == NULL)
    return;

  // Find the user-defined operand for cisc-spilling
  if( _frame->_cisc_spilling_operand_name != NULL ) {
    const Form *form = _globalNames[_frame->_cisc_spilling_operand_name];
    OperandForm *oper = form ? form->is_operand() : NULL;
    // Verify the user's suggestion
    if( oper != NULL ) {
      // Ensure that match field is defined.
      if ( oper->_matrule != NULL )  {
        MatchRule &mrule = *oper->_matrule;
        if( strcmp(mrule._opType,"AddP") == 0 ) {
          MatchNode *left = mrule._lChild;
          MatchNode *right= mrule._rChild;
          if( left != NULL && right != NULL ) {
            const Form *left_op  = _globalNames[left->_opType]->is_operand();
            const Form *right_op = _globalNames[right->_opType]->is_operand();
            if(  (left_op != NULL && right_op != NULL)
              && (left_op->interface_type(_globalNames) == Form::register_interface)
              && (right_op->interface_type(_globalNames) == Form::constant_interface) ) {
              // Successfully verified operand
              set_cisc_spill_operand( oper );
              if( _cisc_spill_debug ) {
                fprintf(stderr, "\n\nVerified CISC-spill operand %s\n\n", oper->_ident);
             }
            }
          }
        }
      }
    }
  }

  if( cisc_spill_operand() != NULL ) {
    // N^2 comparison of instructions looking for a cisc-spilling version
    _instructions.reset();
    InstructForm *instr;
    for( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
      // Ensure that match field is defined.
      if ( instr->_matrule == NULL )  continue;

      MatchRule &mrule = *instr->_matrule;
      Predicate *pred  =  instr->build_predicate();

      // Grab the machine type of the operand
      const char *rootOp = instr->_ident;
      mrule._machType    = rootOp;

      // Find result type for match
      const char *result = instr->reduce_result();

      if( PrintAdlcCisc ) fprintf(stderr, "  new instruction %s \n", instr->_ident ? instr->_ident : " ");
      bool  found_cisc_alternate = false;
      _instructions.reset2();
      InstructForm *instr2;
      for( ; !found_cisc_alternate && (instr2 = (InstructForm*)_instructions.iter2()) != NULL; ) {
        // Ensure that match field is defined.
        if( PrintAdlcCisc ) fprintf(stderr, "  instr2 == %s \n", instr2->_ident ? instr2->_ident : " ");
        if ( instr2->_matrule != NULL
            && (instr != instr2 )                // Skip self
            && (instr2->reduce_result() != NULL) // want same result
            && (strcmp(result, instr2->reduce_result()) == 0)) {
          MatchRule &mrule2 = *instr2->_matrule;
          Predicate *pred2  =  instr2->build_predicate();
          found_cisc_alternate = instr->cisc_spills_to(*this, instr2);
        }
      }
    }
  }
}

//---------------------------build_cisc_spilling-------------------------------
// Get info for the CISC_oracle and MachNode::cisc_version()
void ArchDesc::build_cisc_spill_instructions(FILE *fp_hpp, FILE *fp_cpp) {
  // Output the table for cisc spilling
  fprintf(fp_cpp, "//  The following instructions can cisc-spill\n");
  _instructions.reset();
  InstructForm *inst = NULL;
  for(; (inst = (InstructForm*)_instructions.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( inst->ideal_only() )  continue;
    const char *inst_name = inst->_ident;
    int   operand   = inst->cisc_spill_operand();
    if( operand != AdlcVMDeps::Not_cisc_spillable ) {
      InstructForm *inst2 = inst->cisc_spill_alternate();
      fprintf(fp_cpp, "//  %s can cisc-spill operand %d to %s\n", inst->_ident, operand, inst2->_ident);
    }
  }
  fprintf(fp_cpp, "\n\n");
}

//---------------------------identify_short_branches----------------------------
// Get info for our short branch replacement oracle.
void ArchDesc::identify_short_branches() {
  // Walk over all instructions, checking to see if they match a short
  // branching alternate.
  _instructions.reset();
  InstructForm *instr;
  while( (instr = (InstructForm*)_instructions.iter()) != NULL ) {
    // The instruction must have a match rule.
    if (instr->_matrule != NULL &&
        instr->is_short_branch()) {

      _instructions.reset2();
      InstructForm *instr2;
      while( (instr2 = (InstructForm*)_instructions.iter2()) != NULL ) {
        instr2->check_branch_variant(*this, instr);
      }
    }
  }
}


//---------------------------identify_unique_operands---------------------------
// Identify unique operands.
void ArchDesc::identify_unique_operands() {
  // Walk over all instructions.
  _instructions.reset();
  InstructForm *instr;
  while( (instr = (InstructForm*)_instructions.iter()) != NULL ) {
    // Ensure this is a machine-world instruction
    if (!instr->ideal_only()) {
      instr->set_unique_opnds();
    }
  }
}
