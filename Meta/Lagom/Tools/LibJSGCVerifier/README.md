### LibJSGCVerifier

This is a simple Clang tool to validate certain behavior relating to LibJS's GC. It currently validates
two things:

- For all types wrapped by `GCPtr` or `NonnullGCPtr`, that the wrapped type inherits from `Cell`
- For all types not wrapped by `GCPtr` or `NonnullGCPtr`, that the wrapped type does not inherit from `Cell`
  (otherwise it should be wrapped).

This tool currently requires having first built Serenity with the Clang toolchain for x86_64:
```bash
./Meta/serenity.sh build x86_64 Clang
```

Once Serenity is built, this tool can be built with:
```bash
cmake -GNinja -B build
cmake --build build
```

Then run the tool with:
```bash
src/main.py -b <path to serenity>/Build
```
