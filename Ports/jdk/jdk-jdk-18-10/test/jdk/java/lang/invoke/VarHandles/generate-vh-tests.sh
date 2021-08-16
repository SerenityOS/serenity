#!/bin/bash

javac -d . ../../../../../../make/jdk/src/classes/build/tools/spp/Spp.java

SPP=build.tools.spp.Spp

# Generates variable handle tests for objects and all primitive types
# This is likely to be a temporary testing approach as it may be more
# desirable to generate code using ASM which will allow more flexibility
# in the kinds of tests that are generated.

for type in boolean byte short char int long float double String
do
  Type="$(tr '[:lower:]' '[:upper:]' <<< ${type:0:1})${type:1}"
  args="-K$type -Dtype=$type -DType=$Type"

  args="$args -KCAS"

  case $type in
    byte|short|char|int|long|float|double)
      args="$args -KAtomicAdd"
      ;;
  esac

  case $type in
    boolean|byte|short|char|int|long)
      args="$args -KBitwise"
      ;;
  esac

  wrong_primitive_type=boolean

  case $type in
    boolean)
      value1=true
      value2=false
      value3=false
      wrong_primitive_type=int
      ;;
    byte)
      value1=(byte)0x01
      value2=(byte)0x23
      value3=(byte)0x45
      ;;
    short)
      value1=(short)0x0123
      value2=(short)0x4567
      value3=(short)0x89AB
      ;;
    char)
      value1=\'\\\\u0123\'
      value2=\'\\\\u4567\'
      value3=\'\\\\u89AB\'
      ;;
    int)
      value1=0x01234567
      value2=0x89ABCDEF
      value3=0xCAFEBABE
      ;;
    long)
      value1=0x0123456789ABCDEFL
      value2=0xCAFEBABECAFEBABEL
      value3=0xDEADBEEFDEADBEEFL
      ;;
    float)
      value1=1.0f
      value2=2.0f
      value3=3.0f
      ;;
    double)
      value1=1.0d
      value2=2.0d
      value3=3.0d
      ;;
    String)
      value1=\"foo\"
      value2=\"bar\"
      value3=\"baz\"
      ;;
  esac

  args="$args -Dvalue1=$value1 -Dvalue2=$value2 -Dvalue3=$value3 -Dwrong_primitive_type=$wrong_primitive_type"

  echo $args
  out=VarHandleTestAccess${Type}.java
  rm -f $out
  java $SPP -nel $args -iX-VarHandleTestAccess.java.template -o$out
  out=VarHandleTestMethodHandleAccess${Type}.java
  rm -f $out
  java $SPP -nel $args -iX-VarHandleTestMethodHandleAccess.java.template -o$out
  out=VarHandleTestMethodType${Type}.java
  rm -f $out
  java $SPP -nel $args -iX-VarHandleTestMethodType.java.template -o$out
done

for type in short char int long float double
do
  Type="$(tr '[:lower:]' '[:upper:]' <<< ${type:0:1})${type:1}"
  args="-K$type -Dtype=$type -DType=$Type"

  BoxType=$Type
  case $type in
    char)
      BoxType=Character
      ;;
    int)
      BoxType=Integer
      ;;
  esac
  args="$args -DBoxType=$BoxType"

  case $type in
    int|long|float|double)
      args="$args -KCAS"
      ;;
  esac

  case $type in
    int|long)
      args="$args -KAtomicAdd"
      ;;
  esac

  case $type in
    int|long)
      args="$args -KBitwise"
      ;;
  esac

  # The value of `value3` is chosen such that when added to `value1` or `value2`
  # it will result in carrying of bits over to the next byte, thereby detecting
  # possible errors in endianness conversion e.g. if say for atomic addition the
  # augend is incorrectly processed
  case $type in
    short)
      value1=(short)0x0102
      value2=(short)0x1112
      value3=(short)0xFFFE
      ;;
    char)
      value1=(char)0x0102
      value2=(char)0x1112
      value3=(char)0xFFFE
      ;;
    int)
      value1=0x01020304
      value2=0x11121314
      value3=0xFFFEFDFC
      ;;
    long)
      value1=0x0102030405060708L
      value2=0x1112131415161718L
      value3=0xFFFEFDFCFBFAF9F8L
      ;;
    float)
      value1=0x01020304
      value2=0x11121314
      value3=0xFFFEFDFC
      ;;
    double)
      value1=0x0102030405060708L
      value2=0x1112131415161718L
      value3=0xFFFEFDFCFBFAF9F8L
      ;;
  esac

  args="$args -Dvalue1=$value1 -Dvalue2=$value2 -Dvalue3=$value3"

  echo $args
  out=VarHandleTestByteArrayAs${Type}.java
  rm -f $out
  java $SPP -nel $args -iX-VarHandleTestByteArrayView.java.template -o$out
done

rm -fr build
