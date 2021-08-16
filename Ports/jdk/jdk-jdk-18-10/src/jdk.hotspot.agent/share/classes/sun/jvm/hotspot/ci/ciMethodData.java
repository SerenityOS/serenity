/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.ci;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.types.Field;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class ciMethodData extends ciMetadata implements MethodDataInterface<ciKlass,ciMethod> {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type      = db.lookupType("ciMethodData");
    origField = type.getField("_orig");
    currentMileageField = new CIntField(type.getCIntegerField("_current_mileage"), 0);
    argReturnedField = new CIntField(type.getCIntegerField("_arg_returned"), 0);
    argStackField = new CIntField(type.getCIntegerField("_arg_stack"), 0);
    argLocalField = new CIntField(type.getCIntegerField("_arg_local"), 0);
    eflagsField = new CIntField(type.getCIntegerField("_eflags"), 0);
    hintDiField = new CIntField(type.getCIntegerField("_hint_di"), 0);
    currentMileageField = new CIntField(type.getCIntegerField("_current_mileage"), 0);
    dataField = type.getAddressField("_data");
    extraDataSizeField = new CIntField(type.getCIntegerField("_extra_data_size"), 0);
    dataSizeField = new CIntField(type.getCIntegerField("_data_size"), 0);
    stateField = new CIntField(type.getCIntegerField("_state"), 0);
    Type typeMethodData = db.lookupType("MethodData");
    sizeofMethodDataOopDesc = (int)typeMethodData.getSize();
    parametersTypeDataDi = new CIntField(typeMethodData.getCIntegerField("_parameters_type_data_di"), 0);
  }

  private static Field origField;
  private static CIntField currentMileageField;
  private static CIntField argReturnedField;
  private static CIntField argStackField;
  private static CIntField argLocalField;
  private static CIntField eflagsField;
  private static CIntField hintDiField;
  private static AddressField dataField;
  private static CIntField extraDataSizeField;
  private static CIntField dataSizeField;
  private static CIntField stateField;
  private static int sizeofMethodDataOopDesc;
  private static CIntField parametersTypeDataDi;

  public ciMethodData(Address addr) {
    super(addr);
  }

  public ciKlass getKlassAtAddress(Address addr) {
    return (ciKlass)ciObjectFactory.getMetadata(addr);
  }

  public ciMethod getMethodAtAddress(Address addr) {
    return (ciMethod)ciObjectFactory.getMetadata(addr);
  }

  public void printKlassValueOn(ciKlass klass, PrintStream st) {
    klass.printValueOn(st);
  }

  public void printMethodValueOn(ciMethod method, PrintStream st) {
    method.printValueOn(st);
  }

  private byte[] fetchDataAt(Address base, long size) {
    byte[] result = new byte[(int)size];
    for (int i = 0; i < size; i++) {
      result[i] = base.getJByteAt(i);
    }
    return result;
  }

  public byte[] orig() {
    // fetch the orig MethodData data between header and dataSize
    Address base = getAddress().addOffsetTo(origField.getOffset());
    byte[] result = new byte[(int)origField.getType().getSize()];
    for (int i = 0; i < result.length; i++) {
      result[i] = base.getJByteAt(i);
    }
    return result;
  }

  public  long[] data() {
    // Read the data as an array of intptr_t elements
    Address base = dataField.getValue(getAddress());
    int elements = (dataSize() + extraDataSize()) / MethodData.cellSize;
    long[] result = new long[elements];
    for (int i = 0; i < elements; i++) {
      Address value = base.getAddressAt(i * MethodData.cellSize);
      if (value != null) {
        result[i] = value.minus(null);
      }
    }
    return result;
  }

  int dataSize() {
    return (int)dataSizeField.getValue(getAddress());
  }

  int extraDataSize() {
    return (int)extraDataSizeField.getValue(getAddress());
  }

  int state() {
    return (int)stateField.getValue(getAddress());
  }

  int currentMileage() {
    return (int)currentMileageField.getValue(getAddress());
  }

  boolean outOfBounds(int dataIndex) {
    return dataIndex >= dataSize();
  }

  ParametersTypeData<ciKlass,ciMethod> parametersTypeData() {
    int di = (int)parametersTypeDataDi.getValue(getMetadata().getAddress());
    if (di == -1 || di == -2) {
      return null;
    }
    DataLayout dataLayout = new DataLayout(dataField.getValue(getAddress()), di);
    return new ParametersTypeData<ciKlass,ciMethod>(this, dataLayout);
  }

  ProfileData dataAt(int dataIndex) {
    if (outOfBounds(dataIndex)) {
      return null;
    }
    DataLayout dataLayout = new DataLayout(dataField.getValue(getAddress()), dataIndex);

    switch (dataLayout.tag()) {
    case DataLayout.noTag:
    default:
      throw new InternalError();
    case DataLayout.bitDataTag:
      return new BitData(dataLayout);
    case DataLayout.counterDataTag:
      return new CounterData(dataLayout);
    case DataLayout.jumpDataTag:
      return new JumpData(dataLayout);
    case DataLayout.receiverTypeDataTag:
      return new ReceiverTypeData<ciKlass,ciMethod>(this, dataLayout);
    case DataLayout.virtualCallDataTag:
      return new VirtualCallData<ciKlass,ciMethod>(this, dataLayout);
    case DataLayout.retDataTag:
      return new RetData(dataLayout);
    case DataLayout.branchDataTag:
      return new BranchData(dataLayout);
    case DataLayout.multiBranchDataTag:
      return new MultiBranchData(dataLayout);
    case DataLayout.callTypeDataTag:
      return new CallTypeData<ciKlass,ciMethod>(this, dataLayout);
    case DataLayout.virtualCallTypeDataTag:
      return new VirtualCallTypeData<ciKlass,ciMethod>(this, dataLayout);
    case DataLayout.parametersTypeDataTag:
      return new ParametersTypeData<ciKlass,ciMethod>(this, dataLayout);
    }
  }

  int dpToDi(int dp) {
    return dp;
  }

  int firstDi() { return 0; }
  ProfileData firstData() { return dataAt(firstDi()); }
  ProfileData nextData(ProfileData current) {
    int currentIndex = dpToDi(current.dp());
    int nextIndex = currentIndex + current.sizeInBytes();
    return dataAt(nextIndex);
  }
  boolean isValid(ProfileData current) { return current != null; }

  DataLayout limitDataPosition() {
    return new DataLayout(dataField.getValue(getAddress()), dataSize());
  }
  DataLayout extraDataBase() {
    return limitDataPosition();
  }
  DataLayout extraDataLimit() {
    return new DataLayout(dataField.getValue(getAddress()), dataSize() + extraDataSize());
  }
  DataLayout nextExtra(DataLayout dataLayout) {
    return new DataLayout(dataField.getValue(getAddress()), dataLayout.dp() + DataLayout.computeSizeInBytes(MethodData.extraNbCells(dataLayout)));
  }

  public void printDataOn(PrintStream st) {
    if (parametersTypeData() != null) {
      parametersTypeData().printDataOn(st);
    }
    ProfileData data = firstData();
    for ( ; isValid(data); data = nextData(data)) {
      st.print(dpToDi(data.dp()));
      st.print(" ");
      // st->fillTo(6);
      data.printDataOn(st);
    }
    st.println("--- Extra data:");
    DataLayout dp    = extraDataBase();
    DataLayout end   = extraDataLimit();
    for (;; dp = nextExtra(dp)) {
      switch(dp.tag()) {
      case DataLayout.noTag:
        continue;
      case DataLayout.bitDataTag:
        data = new BitData(dp);
        break;
      case DataLayout.speculativeTrapDataTag:
        data = new SpeculativeTrapData<ciKlass,ciMethod>(this, dp);
        break;
      case DataLayout.argInfoDataTag:
        data = new ArgInfoData(dp);
        dp = end; // ArgInfoData is at the end of extra data section.
        break;
      default:
        throw new InternalError("unexpected tag " +  dp.tag());
      }
      st.print(dpToDi(data.dp()));
      st.print(" ");
      data.printDataOn(st);
      if (dp == end) return;
    }
  }

  int dumpReplayDataTypeHelper(PrintStream out, int round, int count, int index, ProfileData pdata, ciKlass k) {
    if (k != null) {
      if (round == 0) count++;
      else out.print(" " + ((pdata.dp() + pdata.cellOffset(index)) / MethodData.cellSize) + " " + k.name());
    }
    return count;
  }

  int dumpReplayDataReceiverTypeHelper(PrintStream out, int round, int count, ReceiverTypeData<ciKlass,ciMethod> vdata) {
    for (int i = 0; i < vdata.rowLimit(); i++) {
      ciKlass k = vdata.receiver(i);
      count = dumpReplayDataTypeHelper(out, round, count, vdata.receiverCellIndex(i), vdata, k);
    }
    return count;
  }

  int dumpReplayDataCallTypeHelper(PrintStream out, int round, int count, CallTypeDataInterface<ciKlass> callTypeData) {
    if (callTypeData.hasArguments()) {
      for (int i = 0; i < callTypeData.numberOfArguments(); i++) {
        count = dumpReplayDataTypeHelper(out, round, count, callTypeData.argumentTypeIndex(i), (ProfileData)callTypeData, callTypeData.argumentType(i));
      }
    }
    if (callTypeData.hasReturn()) {
      count = dumpReplayDataTypeHelper(out, round, count, callTypeData.returnTypeIndex(), (ProfileData)callTypeData, callTypeData.returnType());
    }
    return count;
  }

  int dumpReplayDataExtraDataHelper(PrintStream out, int round, int count) {
    DataLayout dp    = extraDataBase();
    DataLayout end   = extraDataLimit();

    for (;dp != end; dp = nextExtra(dp)) {
      switch(dp.tag()) {
      case DataLayout.noTag:
      case DataLayout.argInfoDataTag:
        return count;
      case DataLayout.bitDataTag:
        break;
      case DataLayout.speculativeTrapDataTag: {
        SpeculativeTrapData<ciKlass,ciMethod> data = new SpeculativeTrapData<ciKlass,ciMethod>(this, dp);
        ciMethod m = data.method();
        if (m != null) {
          if (round == 0) {
            count++;
          } else {
            out.print(" " +  (dpToDi(data.dp() + data.cellOffset(SpeculativeTrapData.methodIndex())) / MethodData.cellSize) + " " +  m.nameAsAscii());
          }
        }
        break;
      }
      default:
        throw new InternalError("bad tag "  + dp.tag());
      }
    }
    return count;
  }

  public void dumpReplayData(PrintStream out) {
    MethodData mdo = (MethodData)getMetadata();
    Method method = mdo.getMethod();
    out.print("ciMethodData " +
              method.nameAsAscii() + " " +
              state() + " " + currentMileage());
    byte[] orig = orig();
    out.print(" orig " + orig.length);
    for (int i = 0; i < orig.length; i++) {
      out.print(" " + (orig[i] & 0xff));
    }

    long[] data = data();
    out.print(" data " +  data.length);
    for (int i = 0; i < data.length; i++) {
      out.print(" 0x" + Long.toHexString(data[i]));
    }
    int count = 0;
    ParametersTypeData<ciKlass,ciMethod> parameters = parametersTypeData();
    for (int round = 0; round < 2; round++) {
      if (round == 1) out.print(" oops " + count);
      ProfileData pdata = firstData();
      for ( ; isValid(pdata); pdata = nextData(pdata)) {
        if (pdata instanceof ReceiverTypeData) {
          @SuppressWarnings("unchecked")
          ReceiverTypeData<ciKlass,ciMethod> receiverTypeData = (ReceiverTypeData<ciKlass,ciMethod>)pdata;
          count = dumpReplayDataReceiverTypeHelper(out, round, count, receiverTypeData);
        }
        if (pdata instanceof CallTypeDataInterface) {
          @SuppressWarnings("unchecked")
          CallTypeDataInterface<ciKlass> callTypeData = (CallTypeDataInterface<ciKlass>)pdata;
          count = dumpReplayDataCallTypeHelper(out, round, count, callTypeData);
        }
      }
      if (parameters != null) {
        for (int i = 0; i < parameters.numberOfParameters(); i++) {
          count = dumpReplayDataTypeHelper(out, round, count, ParametersTypeData.typeIndex(i), parameters, parameters.type(i));
        }
      }
    }
    count = 0;
    for (int round = 0; round < 2; round++) {
      if (round == 1) out.print(" methods " + count);
      count = dumpReplayDataExtraDataHelper(out, round, count);
    }
    out.println();
  }
}
