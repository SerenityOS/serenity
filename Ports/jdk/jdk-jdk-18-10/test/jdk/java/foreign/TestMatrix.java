/*
 * @test
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @modules jdk.incubator.foreign/jdk.internal.foreign
 * @build NativeTestHelper CallGeneratorHelper TestUpcallHighArity TestUpcall TestDowncall
 *
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcallHighArity
 *
  * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcallHighArity
 *
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcallHighArity
 *
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcallHighArity
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcallHighArity
 *
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   TestDowncall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   TestDowncall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   TestDowncall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   TestDowncall
 *
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcall
 *
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcall
 *
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=true
 *   TestUpcall
 *
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcall
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_SPEC=false
 *   -Djdk.internal.foreign.ProgrammableUpcallHandler.USE_INTRINSICS=false
 *   TestUpcall
 */
