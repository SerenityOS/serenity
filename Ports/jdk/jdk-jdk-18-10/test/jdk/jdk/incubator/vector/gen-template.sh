#!/bin/bash
#
# Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

generate_perf_tests=$1

TEMPLATE_FOLDER="templates/"

unit_output="unit_tests.template"
perf_output="perf_tests.template"
perf_scalar_output="perf_scalar_tests.template"

unary="Unary-op"
unary_masked="Unary-Masked-op"
unary_scalar="Unary-Scalar-op"
ternary="Ternary-op"
ternary_masked="Ternary-Masked-op"
ternary_broadcast="Ternary-Broadcast-op"
ternary_broadcast_masked="Ternary-Broadcast-Masked-op"
ternary_double_broadcast="Ternary-Double-Broadcast-op"
ternary_double_broadcast_masked="Ternary-Double-Broadcast-Masked-op"
ternary_scalar="Ternary-Scalar-op"
binary="Binary-op"
binary_masked="Binary-Masked-op"
binary_broadcast="Binary-Broadcast-op"
binary_broadcast_masked="Binary-Broadcast-Masked-op"
binary_broadcast_long="Binary-Broadcast-Long-op"
binary_broadcast_masked_long="Binary-Broadcast-Masked-Long-op"
binary_scalar="Binary-Scalar-op"
blend="Blend-op"
test_template="Test"
compare_template="Compare"
compare_masked_template="Compare-Masked"
compare_broadcast_template="Compare-Broadcast"
reduction_scalar="Reduction-Scalar-op"
reduction_scalar_min="Reduction-Scalar-Min-op"
reduction_scalar_max="Reduction-Scalar-Max-op"
reduction_scalar_masked="Reduction-Scalar-Masked-op"
reduction_scalar_min_masked="Reduction-Scalar-Masked-Min-op"
reduction_scalar_max_masked="Reduction-Scalar-Masked-Max-op"
reduction_op="Reduction-op"
reduction_op_min="Reduction-Min-op"
reduction_op_max="Reduction-Max-op"
reduction_op_masked="Reduction-Masked-op"
reduction_op_min_masked="Reduction-Masked-Min-op"
reduction_op_max_masked="Reduction-Masked-Max-op"
unary_math_template="Unary-op-math"
binary_math_template="Binary-op-math"
binary_math_broadcast_template="Binary-Broadcast-op-math"
bool_reduction_scalar="BoolReduction-Scalar-op"
bool_reduction_template="BoolReduction-op"
with_op_template="With-Op"
shift_template="Shift-op"
shift_masked_template="Shift-Masked-op"
get_template="Get-op"
rearrange_template="Rearrange"
broadcast_template="Broadcast"
zero_template="Zero"
slice_template="Slice-op"
slice1_template="Slice-bop"
slice1_masked_template="Slice-Masked-bop"
unslice_template="Unslice-op"
unslice1_template="Unslice-bop"
unslice1_masked_template="Unslice-Masked-bop"
miscellaneous_template="Miscellaneous"

function replace_variables {
  local filename=$1
  local output=$2
  local kernel=$3
  local test=$4
  local op=$5
  local init=$6
  local guard=$7
  local masked=$8
  local op_name=$9
  local kernel_smoke=${10}

  if [ "x${kernel}" != "x" ]; then
    local kernel_escaped=$(echo -e "$kernel" | tr '\n' '|')
    sed "s/\[\[KERNEL\]\]/${kernel_escaped}/g" $filename > ${filename}.current1
    cat ${filename}.current1 | tr '|' "\n" > ${filename}.current
    rm -f "${filename}.current1"
  else
    cp $filename ${filename}.current
  fi

  # Check if we need to do multiple replacements
  # If you want to emit for an operation using lanewise(VectorOperator.**, ..) and also using dedicated instruction (e.g. add(..)), then
  # pass the 'test' argument as "OPERATOR_NAME+func_Name" (e.g. "ADD+add")
  # if there is a masked version available for the operation add "withMask" to 'test' argument (e.g. "ADD+add+withMask")
  local test_func=""
  local withMask=""
  local tests=($(awk -F+ '{$1=$1} 1' <<< $test))
  if [ "${tests[2]}" == "withMask" ]; then
    test=${tests[0]}
    test_func=${tests[1]}
    withMask=${tests[2]}
  elif [ "${tests[1]}" == "withMask" ]; then
    test=""
    test_func=${tests[0]}
    withMask=${tests[1]}
  elif [ "${tests[1]}" != "" ]; then
    test=${tests[0]}
    test_func=${tests[1]}
  fi

  sed_prog="
    s/\<OPTIONAL\>\(.*\)\<\\OPTIONAL\>/\1/g
    s/\[\[TEST_TYPE\]\]/${masked}/g
    s/\[\[TEST_OP\]\]/${op}/g
    s/\[\[TEST_INIT\]\]/${init}/g
    s/\[\[OP_NAME\]\]/${op_name}/g
  "
  sed_prog_2="$sed_prog
    s/\[\[TEST\]\]/${test_func}/g
    s/[.][^(]*(VectorOperators.$test_func, /.$test_func(/g
    s/[.][^(]*(VectorOperators.$test_func,/.$test_func(/g
    s/[.][^(]*(VectorOperators.$test_func/.$test_func(/g
  "
  sed_prog="
    $sed_prog
    s/\[\[TEST\]\]/${test}/g
  "

  # Guard the test if necessary
  if [ "$guard" != "" ]; then
    echo -e "#if[${guard}]\n" >> $output
  fi
  if [ "$test" != "" ]; then
    sed -e "$sed_prog" < ${filename}.current >> $output
  fi
  # If we also have a dedicated function for the operation then use 2nd sed expression
  if [[ "$filename" == *"Unit"* ]] && [ "$test_func" != "" ]; then
    if [ "$masked" == "" ] || [ "$withMask" != "" ]; then
      if [ ! -z "$kernel_smoke" ]; then
        local kernel_smoke_escaped=$(echo -e "$kernel_smoke" | tr '\n' '|')
        sed "s/\[\[KERNEL\]\]/${kernel_smoke_escaped}/g" $filename > ${filename}.scurrent1
        cat ${filename}.scurrent1 | tr '|' "\n" > ${filename}.scurrent
        rm -f "${filename}.scurrent1"
      else
        cp $filename.current ${filename}.scurrent
      fi
      sed -e "$sed_prog_2" < ${filename}.scurrent >> $output
      rm -f ${filename}.scurrent
    fi
  fi
  if [ "$guard" != "" ]; then
    echo -e "#end[${guard}]\n" >> $output
  fi

  rm -f ${filename}.current
}

function gen_op_tmpl {
  local template=$1
  local test=$2
  local op=$3
  local guard=""
  local init=""
  if [ $# -gt 3 ]; then
    guard=$4
  fi
  if [ $# == 5 ]; then
    init=$5
  fi

  local masked=""
  if [[ $template == *"Masked"* ]]; then
    masked="Masked"
  fi

  local op_name=""
  if [[ $template == *"Shift"* ]]; then
    op_name="Shift"
  elif [[ $template == *"Get"* ]]; then
    op_name="extract"
  fi

  local kernel_filename="${TEMPLATE_FOLDER}/Kernel-${template}.template"
  local kernel_smoke_filename="${TEMPLATE_FOLDER}/Kernel-${template}-smoke.template"
  local unit_filename="${TEMPLATE_FOLDER}/Unit-${template}.template"
  if [ ! -f $unit_filename ]; then
    # Leverage general unit code snippet if no specialization exists
    unit_filename="${TEMPLATE_FOLDER}/Unit-${template%_*}.template"
    echo $unit_filename
  fi

  local kernel=""
  if [ -f $kernel_filename ]; then
    kernel="$(cat $kernel_filename)"
  fi

  local kernel_smoke=""
  if [ -f $kernel_smoke_filename ]; then
    kernel_smoke="$(cat $kernel_smoke_filename)"
  else
    kernel_smoke="$kernel"
  fi

  # Replace template variables in unit test files (if any)
  replace_variables $unit_filename $unit_output "$kernel" "$test" "$op" "$init" "$guard" "$masked" "$op_name" "$kernel_smoke"

  local gen_perf_tests=$generate_perf_tests
  if [[ $template == *"-Broadcast-"* ]] || [[ $template == "Miscellaneous" ]] ||
     [[ $template == *"Compare-Masked"* ]] || [[ $template == *"Compare-Broadcast"* ]]; then
    gen_perf_tests=false
  fi
  if [ $gen_perf_tests == true ]; then
    # Replace template variables in performance test files (if any)
    local perf_wrapper_filename="${TEMPLATE_FOLDER}/Perf-wrapper.template"
    local perf_vector_filename="${TEMPLATE_FOLDER}/Perf-${template}.template"
    local perf_scalar_filename="${TEMPLATE_FOLDER}/Perf-Scalar-${template}.template"

    if [ -f $perf_vector_filename ]; then
      replace_variables $perf_vector_filename  $perf_output "$kernel" "$test" "$op" "$init" "$guard" "$masked" "$op_name" ""
    elif [ -f $kernel_filename ]; then
      replace_variables $perf_wrapper_filename $perf_output "$kernel" "$test" "$op" "$init" "$guard" "$masked" "$op_name" ""
    elif [[ $template != *"-Scalar-"* ]] && [[ $template != "Get-op" ]] && [[ $template != "With-Op" ]]; then
      echo "Warning: missing perf: $@"
    fi

    if [ -f $perf_scalar_filename ]; then
      replace_variables $perf_scalar_filename $perf_scalar_output "$kernel" "$test" "$op" "$init" "$guard" "$masked" "$op_name" ""
    elif [[ $template != *"-Scalar-"* ]] && [[ $template != "Get-op" ]] && [[ $template != "With-Op" ]]; then
      echo "Warning: Missing PERF SCALAR: $perf_scalar_filename"
    fi
  fi
}

function gen_binary_alu_op {
  echo "Generating binary op $1 ($2)..."
  gen_op_tmpl $binary "$@"
  gen_op_tmpl $binary_masked "$@"
}

function gen_binary_alu_bcst_op {
  echo "Generating binary broadcast op $1 ($2)..."
  gen_op_tmpl $binary_broadcast "$@"
  gen_op_tmpl $binary_broadcast_masked "$@"
}

function gen_binary_alu_bcst_long_op {
  echo "Generating binary broadcast long op $1 ($2)..."
  gen_op_tmpl $binary_broadcast_long "$@"
  gen_op_tmpl $binary_broadcast_masked_long "$@"
}

function gen_shift_cst_op {
  echo "Generating Shift constant op $1 ($2)..."
  gen_op_tmpl $shift_template "$@"
  gen_op_tmpl $shift_masked_template "$@"
}

function gen_unary_alu_op {
  echo "Generating unary op $1 ($2)..."
  gen_op_tmpl $unary_scalar "$@"
  gen_op_tmpl $unary "$@"
  gen_op_tmpl $unary_masked "$@"
}

function gen_ternary_alu_op {
  echo "Generating ternary op $1 ($2)..."
  gen_op_tmpl $ternary_scalar "$@"
  gen_op_tmpl $ternary "$@"
  gen_op_tmpl $ternary_masked "$@"
}

function gen_ternary_alu_bcst_op {
  echo "Generating ternary broadcast op $1 ($2)..."
  gen_op_tmpl $ternary_broadcast "$@"
  gen_op_tmpl $ternary_broadcast_masked "$@"
}

function gen_ternary_alu_double_bcst_op {
  echo "Generating ternary double broadcast op $1 ($2)..."
  gen_op_tmpl $ternary_double_broadcast "$@"
  gen_op_tmpl $ternary_double_broadcast_masked "$@"
}

function gen_binary_op {
  echo "Generating binary op $1 ($2)..."
#  gen_op_tmpl $binary_scalar "$@"
  gen_op_tmpl $binary "$@"
  gen_op_tmpl $binary_masked "$@"
}

function gen_binary_op_no_masked {
  echo "Generating binary op $1 ($2)..."
#  gen_op_tmpl $binary_scalar "$@"
  gen_op_tmpl $binary "$@"
}

function gen_binary_bcst_op_no_masked {
  echo "Generating binary broadcast op $1 ($2)..."
  gen_op_tmpl $binary_broadcast "$@"
}

function gen_compare_op {
  echo "Generating compare op $1 ($2)..."
  gen_op_tmpl $compare_template "$@"
  gen_op_tmpl $compare_masked_template "$@"
}

function gen_compare_bcst_op {
  echo "Generating compare broadcast op $1 ($2)..."
  gen_op_tmpl $compare_broadcast_template "$@"
}

function gen_reduction_op {
  echo "Generating reduction op $1 ($2)..."
  gen_op_tmpl $reduction_scalar "$@"
  gen_op_tmpl $reduction_op "$@"
  gen_op_tmpl $reduction_scalar_masked "$@"
  gen_op_tmpl $reduction_op_masked "$@"
}

function gen_reduction_op_min {
  echo "Generating reduction op $1 ($2)..."
  gen_op_tmpl $reduction_scalar_min "$@"
  gen_op_tmpl $reduction_op_min "$@"
  gen_op_tmpl $reduction_scalar_min_masked "$@"
  gen_op_tmpl $reduction_op_min_masked "$@"
}

function gen_reduction_op_max {
  echo "Generating reduction op $1 ($2)..."
  gen_op_tmpl $reduction_scalar_max "$@"
  gen_op_tmpl $reduction_op_max "$@"
  gen_op_tmpl $reduction_scalar_max_masked "$@"
  gen_op_tmpl $reduction_op_max_masked "$@"
}

function gen_bool_reduction_op {
  echo "Generating boolean reduction op $1 ($2)..."
  gen_op_tmpl $bool_reduction_scalar "$@"
  gen_op_tmpl $bool_reduction_template "$@"
}

function gen_with_op {
  echo "Generating with op $1 ($2)..."
  gen_op_tmpl $with_op_template "$@"
}

function gen_get_op {
  echo "Generating get op $1 ($2)..."
  gen_op_tmpl $get_template "$@"
}

function gen_unit_header {
  cat $TEMPLATE_FOLDER/Unit-header.template > $1
}

function gen_unit_footer {
  cat $TEMPLATE_FOLDER/Unit-footer.template >> $1
}

function gen_perf_header {
  cat $TEMPLATE_FOLDER/Perf-header.template > $1
}

function gen_perf_footer {
  cat $TEMPLATE_FOLDER/Perf-footer.template >> $1
}

function gen_perf_scalar_header {
  cat $TEMPLATE_FOLDER/Perf-Scalar-header.template > $1
}

function gen_perf_scalar_footer {
  cat $TEMPLATE_FOLDER/Perf-Scalar-footer.template >> $1
}

gen_unit_header $unit_output

if [ $generate_perf_tests == true ]; then
  gen_perf_header $perf_output
  gen_perf_scalar_header $perf_scalar_output
fi

# ALU binary ops.
# Here "ADD+add+withMask" says VectorOperator name is "ADD", and we have a dedicate method too named 'add', and add() is also available with mask variant.
gen_binary_alu_op "ADD+add+withMask" "a + b"
gen_binary_alu_op "SUB+sub+withMask" "a - b"
gen_binary_alu_op "MUL+mul+withMask" "a \* b"
gen_binary_alu_op "DIV+div+withMask" "a \/ b" "FP"
gen_op_tmpl "Binary-op_bitwise-div" "DIV+div+withMask" "a \/ b" "BITWISE"
gen_op_tmpl "Binary-Masked-op_bitwise-div" "DIV+div+withMask" "a \/ b" "BITWISE"
gen_binary_alu_op "FIRST_NONZERO" "{#if[FP]?Double.doubleToLongBits}(a)!=0?a:b"
gen_binary_alu_op "AND+and"   "a \& b"  "BITWISE"
gen_binary_alu_op "AND_NOT" "a \& ~b" "BITWISE"
gen_binary_alu_op "OR+or"    "a | b"   "BITWISE"
# Missing:        "OR_UNCHECKED"
gen_binary_alu_op "XOR"   "a ^ b"   "BITWISE"
# Generate the broadcast versions
gen_binary_alu_bcst_op "add+withMask" "a + b"
gen_binary_alu_bcst_op "sub+withMask" "a - b"
gen_binary_alu_bcst_op "mul+withMask" "a \* b"
gen_binary_alu_bcst_op "div+withMask" "a \/ b" "FP"
gen_op_tmpl "Binary-Broadcast-op_bitwise-div" "div+withMask" "a \/ b" "BITWISE"
gen_op_tmpl "Binary-Broadcast-Masked-op_bitwise-div" "div+withMask" "a \/ b" "BITWISE"
gen_binary_alu_bcst_op "OR+or"    "a | b"   "BITWISE"
gen_binary_alu_bcst_op "AND+and"    "a \& b"   "BITWISE"
gen_binary_alu_bcst_long_op "OR"     "a | b"   "BITWISE"
gen_binary_alu_bcst_long_op "ADD"    "a + b"

# Shifts
gen_binary_alu_op "LSHL" "(a << b)" "intOrLong"
gen_binary_alu_op "LSHL" "(a << (b \& 0x7))" "byte"
gen_binary_alu_op "LSHL" "(a << (b \& 0xF))" "short"
gen_binary_alu_op "ASHR" "(a >> b)" "intOrLong"
gen_binary_alu_op "ASHR" "(a >> (b \& 0x7))" "byte"
gen_binary_alu_op "ASHR" "(a >> (b \& 0xF))" "short"
gen_binary_alu_op "LSHR" "(a >>> b)" "intOrLong"
gen_binary_alu_op "LSHR" "((a \& 0xFF) >>> (b \& 0x7))" "byte"
gen_binary_alu_op "LSHR" "((a \& 0xFFFF) >>> (b \& 0xF))" "short"
gen_shift_cst_op  "LSHL" "(a << b)" "intOrLong"
gen_shift_cst_op  "LSHL" "(a << (b \& 7))" "byte"
gen_shift_cst_op  "LSHL" "(a << (b \& 15))" "short"
gen_shift_cst_op  "LSHR" "(a >>> b)" "intOrLong"
gen_shift_cst_op  "LSHR" "((a \& 0xFF) >>> (b \& 7))" "byte"
gen_shift_cst_op  "LSHR" "((a \& 0xFFFF) >>> (b \& 15))" "short"
gen_shift_cst_op  "ASHR" "(a >> b)" "intOrLong"
gen_shift_cst_op  "ASHR" "(a >> (b \& 7))" "byte"
gen_shift_cst_op  "ASHR" "(a >> (b \& 15))" "short"

# Masked reductions.
gen_binary_op_no_masked "MIN+min" "Math.min(a, b)"
gen_binary_op_no_masked "MAX+max" "Math.max(a, b)"
gen_binary_bcst_op_no_masked "MIN+min" "Math.min(a, b)"
gen_binary_bcst_op_no_masked "MAX+max" "Math.max(a, b)"

# Reductions.
gen_reduction_op "AND" "\&" "BITWISE" "-1"
gen_reduction_op "OR" "|" "BITWISE" "0"
gen_reduction_op "XOR" "^" "BITWISE" "0"
gen_reduction_op "ADD" "+" "" "0"
gen_reduction_op "MUL" "*" "" "1"
gen_reduction_op_min "MIN" "" "" "\$Wideboxtype\$.\$MaxValue\$"
gen_reduction_op_max "MAX" "" "" "\$Wideboxtype\$.\$MinValue\$"
#gen_reduction_op "reduce_FIRST_NONZERO" "lanewise_FIRST_NONZERO" "{#if[FP]?Double.doubleToLongBits}(a)=0?a:b" "" "1"

# Boolean reductions.
gen_bool_reduction_op "anyTrue" "|" "BITWISE" "false"
gen_bool_reduction_op "allTrue" "\&" "BITWISE" "true"

#Insert
gen_with_op "withLane" "" "" ""

# Tests
gen_op_tmpl $test_template "IS_DEFAULT" "bits(a)==0"
gen_op_tmpl $test_template "IS_NEGATIVE" "bits(a)<0"
gen_op_tmpl $test_template "IS_FINITE" "\$Boxtype\$.isFinite(a)" "FP"
gen_op_tmpl $test_template "IS_NAN" "\$Boxtype\$.isNaN(a)" "FP"
gen_op_tmpl $test_template "IS_INFINITE" "\$Boxtype\$.isInfinite(a)" "FP"

# Compares
gen_compare_op "LT+lt" "lt"
gen_compare_op "GT" "gt"
gen_compare_op "EQ+eq" "eq"
gen_compare_op "NE" "neq"
gen_compare_op "LE" "le"
gen_compare_op "GE" "ge"

gen_compare_op "UNSIGNED_LT" "ult" "BITWISE"
gen_compare_op "UNSIGNED_GT" "ugt" "BITWISE"
gen_compare_op "UNSIGNED_LE" "ule" "BITWISE"
gen_compare_op "UNSIGNED_GE" "uge" "BITWISE"


gen_compare_bcst_op "LT" "<"
gen_compare_bcst_op "EQ" "=="

# Blend.
gen_op_tmpl $blend "blend" ""

# Rearrange
gen_op_tmpl $rearrange_template "rearrange" ""

# Get
gen_get_op "lane" ""

# Broadcast
gen_op_tmpl $broadcast_template "broadcast" ""

# Zero
gen_op_tmpl $zero_template "zero" ""

# Slice
gen_op_tmpl $slice_template "sliceUnary" ""
gen_op_tmpl $slice1_template "sliceBinary" ""
gen_op_tmpl $slice1_masked_template "slice" ""

# Unslice
gen_op_tmpl $unslice_template "unsliceUnary" ""
gen_op_tmpl $unslice1_template "unsliceBinary" ""
gen_op_tmpl $unslice1_masked_template "unslice" ""

# Math
gen_op_tmpl $unary_math_template "SIN" "Math.sin((double)a)" "FP"
gen_op_tmpl $unary_math_template "EXP" "Math.exp((double)a)" "FP"
gen_op_tmpl $unary_math_template "LOG1P" "Math.log1p((double)a)" "FP"
gen_op_tmpl $unary_math_template "LOG" "Math.log((double)a)" "FP"
gen_op_tmpl $unary_math_template "LOG10" "Math.log10((double)a)" "FP"
gen_op_tmpl $unary_math_template "EXPM1" "Math.expm1((double)a)" "FP"
gen_op_tmpl $unary_math_template "COS" "Math.cos((double)a)" "FP"
gen_op_tmpl $unary_math_template "TAN" "Math.tan((double)a)" "FP"
gen_op_tmpl $unary_math_template "SINH" "Math.sinh((double)a)" "FP"
gen_op_tmpl $unary_math_template "COSH" "Math.cosh((double)a)" "FP"
gen_op_tmpl $unary_math_template "TANH" "Math.tanh((double)a)" "FP"
gen_op_tmpl $unary_math_template "ASIN" "Math.asin((double)a)" "FP"
gen_op_tmpl $unary_math_template "ACOS" "Math.acos((double)a)" "FP"
gen_op_tmpl $unary_math_template "ATAN" "Math.atan((double)a)" "FP"
gen_op_tmpl $unary_math_template "CBRT" "Math.cbrt((double)a)" "FP"
gen_op_tmpl $binary_math_template "HYPOT" "Math.hypot((double)a, (double)b)" "FP"
gen_op_tmpl $binary_math_template "POW+pow" "Math.pow((double)a, (double)b)" "FP"
gen_op_tmpl $binary_math_template "ATAN2" "Math.atan2((double)a, (double)b)" "FP"
gen_op_tmpl $binary_math_broadcast_template "POW+pow" "Math.pow((double)a, (double)b)" "FP"

# Ternary operations.
gen_ternary_alu_op "FMA+fma" "Math.fma(a, b, c)" "FP"
gen_ternary_alu_op "BITWISE_BLEND+bitwiseBlend" "(a\&~(c))|(b\&c)" "BITWISE"
gen_ternary_alu_bcst_op "FMA" "Math.fma(a, b, c)" "FP"
gen_ternary_alu_bcst_op "BITWISE_BLEND+bitwiseBlend" "(a\&~(c))|(b\&c)" "BITWISE"
gen_ternary_alu_double_bcst_op "FMA+fma" "Math.fma(a, b, c)" "FP"
gen_ternary_alu_double_bcst_op "BITWISE_BLEND+bitwiseBlend" "(a\&~(c))|(b\&c)" "BITWISE"

# Unary operations.
gen_unary_alu_op "NEG+neg" "-((\$type\$)a)"
gen_unary_alu_op "ABS+abs" "Math.abs((\$type\$)a)"
gen_unary_alu_op "NOT+not" "~((\$type\$)a)" "BITWISE"
gen_unary_alu_op "ZOMO" "(a==0?0:-1)" "BITWISE"
gen_unary_alu_op "SQRT+sqrt" "Math.sqrt((double)a)" "FP"

# Miscellaneous Smoke Tests
gen_op_tmpl $miscellaneous_template "MISC" "" ""

gen_unit_footer $unit_output

if [ $generate_perf_tests == true ]; then
  gen_perf_footer $perf_output
  gen_perf_scalar_footer $perf_scalar_output
fi

rm -f templates/*.current*
