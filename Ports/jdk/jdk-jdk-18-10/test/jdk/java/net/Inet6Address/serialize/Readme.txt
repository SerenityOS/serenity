This test uses 2 binary data files that were each created by serializing an Inet6Address instance.
In both cases this has to do with the tricky issue of scopes in serialized addresses.

serial1.4.2.ser: Was created by serializing an Inet6Address (::1) with J2SE 1.4.2 and is used to check for backward compatibility.

serial-bge0.ser: Was created on a Sparc workstation because it has an uncommon interface name ('bge0') which is useful for the test.
