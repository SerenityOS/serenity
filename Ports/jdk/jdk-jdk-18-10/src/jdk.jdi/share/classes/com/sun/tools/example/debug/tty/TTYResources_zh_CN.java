/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package com.sun.tools.example.debug.tty;

/**
 * <p> This class represents the <code>ResourceBundle</code>
 * for the following package(s):
 *
 * <ol>
 * <li> com.sun.tools.example.debug.tty
 * </ol>
 *
 */
public class TTYResources_zh_CN extends java.util.ListResourceBundle {


    /**
     * Returns the contents of this <code>ResourceBundle</code>.
     *
     * <p>
     *
     * @return the contents of this <code>ResourceBundle</code>.
     */
    @Override
    public Object[][] getContents() {
        Object[][] temp = new Object[][] {
        // NOTE: The value strings in this file containing "{0}" are
        //       processed by the java.text.MessageFormat class.  Any
        //       single quotes appearing in these strings need to be
        //       doubled up.
        //
        // LOCALIZE THIS
        {"** classes list **", "** \u7C7B\u5217\u8868 **\n{0}"},
        {"** fields list **", "** \u5B57\u6BB5\u5217\u8868 **\n{0}"},
        {"** methods list **", "** \u65B9\u6CD5\u5217\u8868 **\n{0}"},
        {"*** Reading commands from", "*** \u6B63\u5728\u4ECE{0}\u8BFB\u53D6\u547D\u4EE4"},
        {"All threads resumed.", "\u5DF2\u6062\u590D\u6240\u6709\u7EBF\u7A0B\u3002"},
        {"All threads suspended.", "\u5DF2\u6302\u8D77\u6240\u6709\u7EBF\u7A0B\u3002"},
        {"Argument is not defined for connector:", "\u6CA1\u6709\u4E3A\u8FDE\u63A5\u5668{1}\u5B9A\u4E49\u53C2\u6570{0}"},
        {"Arguments match no method", "\u53C2\u6570\u4E0D\u4E0E\u4EFB\u4F55\u65B9\u6CD5\u5339\u914D"},
        {"Array:", "\u6570\u7EC4: {0}"},
        {"Array element is not a method", "\u6570\u7EC4\u5143\u7D20\u4E0D\u662F\u65B9\u6CD5"},
        {"Array index must be a integer type", "\u6570\u7EC4\u7D22\u5F15\u5FC5\u987B\u4E3A\u6574\u6570\u7C7B\u578B"},
        {"base directory:", "\u57FA\u76EE\u5F55: {0}"},
        {"Breakpoint hit:", "\u65AD\u70B9\u547D\u4E2D: "},
        {"breakpoint", "\u65AD\u70B9{0}"},
        {"Breakpoints set:", "\u65AD\u70B9\u96C6:"},
        {"Breakpoints can be located only in classes.", "\u65AD\u70B9\u53EA\u80FD\u4F4D\u4E8E\u7C7B\u4E2D\u3002{0}\u662F\u63A5\u53E3\u6216\u6570\u7EC4\u3002"},
        {"Can only trace", "\u53EA\u80FD\u8DDF\u8E2A 'methods', 'method exit' \u6216 'method exits'"},
        {"cannot redefine existing connection", "{0}\u65E0\u6CD5\u91CD\u65B0\u5B9A\u4E49\u73B0\u6709\u8FDE\u63A5"},
        {"Cannot assign to a method invocation", "\u65E0\u6CD5\u5206\u914D\u5230\u65B9\u6CD5\u8C03\u7528"},
        {"Cannot specify command line with connector:", "\u65E0\u6CD5\u6307\u5B9A\u5E26\u6709\u8FDE\u63A5\u5668\u7684\u547D\u4EE4\u884C: {0}"},
        {"Cannot specify target vm arguments with connector:", "\u65E0\u6CD5\u6307\u5B9A\u5E26\u6709\u8FDE\u63A5\u5668\u7684\u76EE\u6807 VM \u53C2\u6570: {0}"},
        {"Class containing field must be specified.", "\u5FC5\u987B\u6307\u5B9A\u5305\u542B\u5B57\u6BB5\u7684\u7C7B\u3002"},
        {"Class:", "\u7C7B: {0}"},
        {"Classic VM no longer supported.", "\u4E0D\u518D\u652F\u6301\u7ECF\u5178 VM\u3002"},
        {"classpath:", "\u7C7B\u8DEF\u5F84: {0}"},
        {"colon mark", ":"},
        {"colon space", ": "},
        {"Command is not supported on the target VM", "\u76EE\u6807 VM \u4E0D\u652F\u6301\u547D\u4EE4 ''{0}''"},
        {"Command is not supported on a read-only VM connection", "\u53EA\u8BFB VM \u8FDE\u63A5\u4E0D\u652F\u6301\u547D\u4EE4 ''{0}''"},
        {"Command not valid until the VM is started with the run command", "\u5728\u4F7F\u7528 ''run'' \u547D\u4EE4\u542F\u52A8 VM \u524D, \u547D\u4EE4 ''{0}'' \u662F\u65E0\u6548\u7684"},
        {"Condition must be boolean", "\u6761\u4EF6\u5FC5\u987B\u662F\u5E03\u5C14\u578B"},
        {"Connector and Transport name", "  \u8FDE\u63A5\u5668: {0}, \u4F20\u8F93: {1}"},
        {"Connector argument nodefault", "    \u53C2\u6570: {0} (\u65E0\u9ED8\u8BA4\u503C)"},
        {"Connector argument default", "    \u53C2\u6570: {0}, \u9ED8\u8BA4\u503C: {1}"},
        {"Connector description", "    \u8BF4\u660E: {0}"},
        {"Connector required argument nodefault", "    \u6240\u9700\u7684\u53C2\u6570: {0} (\u65E0\u9ED8\u8BA4\u503C)"},
        {"Connector required argument default", "    \u6240\u9700\u7684\u53C2\u6570: {0}, \u9ED8\u8BA4\u503C: {1}"},
        {"Connectors available", "\u53EF\u7528\u8FDE\u63A5\u5668\u4E3A:"},
        {"Constant is not a method", "\u5E38\u91CF\u4E0D\u662F\u65B9\u6CD5"},
        {"Could not open:", "\u65E0\u6CD5\u6253\u5F00: {0}"},
        {"Current method is native", "\u5F53\u524D\u65B9\u6CD5\u4E3A\u672C\u673A\u65B9\u6CD5"},
        {"Current thread died. Execution continuing...", "\u5F53\u524D\u7EBF\u7A0B{0}\u5DF2\u6210\u4E3A\u6B7B\u7EBF\u7A0B\u3002\u7EE7\u7EED\u6267\u884C..."},
        {"Current thread isnt suspended.", "\u5F53\u524D\u7EBF\u7A0B\u672A\u6302\u8D77\u3002"},
        {"Current thread not set.", "\u5F53\u524D\u7EBF\u7A0B\u672A\u8BBE\u7F6E\u3002"},
        {"dbgtrace flag value must be an integer:", "dbgtrace \u6807\u8BB0\u503C\u5FC5\u987B\u4E3A\u6574\u6570: {0}"},
        {"dbgtrace command value must be an integer:", "dbgtrace \u547D\u4EE4\u503C\u5FC5\u987B\u4E3A\u6574\u6570\uFF1A{0}"},
        {"Deferring.", "\u6B63\u5728\u5EF6\u8FDF{0}\u3002\n\u5C06\u5728\u52A0\u8F7D\u7C7B\u540E\u8BBE\u7F6E\u3002"},
        {"End of stack.", "\u5806\u6808\u7ED3\u675F\u3002"},
        {"Error popping frame", "\u4F7F\u5E27\u51FA\u6808\u65F6\u51FA\u9519 - {0}"},
        {"Error reading file", "\u8BFB\u53D6 ''{0}'' \u65F6\u51FA\u9519 - {1}"},
        {"Error redefining class to file", "\u5C06{0}\u91CD\u65B0\u5B9A\u4E49\u4E3A{1}\u65F6\u51FA\u9519 - {2}"},
        {"exceptionSpec all", "\u6240\u6709{0}"},
        {"exceptionSpec caught", "\u6355\u83B7\u7684{0}"},
        {"exceptionSpec uncaught", "\u672A\u6355\u83B7\u7684{0}"},
        {"Exception in expression:", "\u8868\u8FBE\u5F0F\u4E2D\u51FA\u73B0\u5F02\u5E38\u9519\u8BEF: {0}"},
        {"Exception occurred caught", "\u51FA\u73B0\u5F02\u5E38\u9519\u8BEF: {0} (\u5C06\u5728\u4EE5\u4E0B\u4F4D\u7F6E\u6355\u83B7: {1})"},
        {"Exception occurred uncaught", "\u51FA\u73B0\u5F02\u5E38\u9519\u8BEF: {0} (\u672A\u6355\u83B7)"},
        {"Exceptions caught:", "\u51FA\u73B0\u8FD9\u4E9B\u5F02\u5E38\u9519\u8BEF\u65F6\u4E2D\u65AD:"},
        {"Expected at, in, or an integer <thread_id>:", "\u5E94\u4E3A \"at\"\u3001\"in\" \u6216\u4EE5\u6574\u6570\u8868\u793A\u7684 <thread_id>\uFF1A{0}"},
        {"expr is null", "{0} = \u7A7A\u503C"},
        {"expr is value", "{0} = {1}"},
        {"expr is value <collected>", "  {0} = {1} <\u5DF2\u6536\u96C6>"},
        {"Expression cannot be void", "\u8868\u8FBE\u5F0F\u4E0D\u80FD\u4E3A\u7A7A"},
        {"Expression must evaluate to an object", "\u8868\u8FBE\u5F0F\u7684\u8BA1\u7B97\u7ED3\u679C\u5FC5\u987B\u4E3A\u5BF9\u8C61"},
        {"extends:", "\u6269\u5C55: {0}"},
        {"Extra tokens after breakpoint location", "\u7AEF\u70B9\u4F4D\u7F6E\u540E\u9762\u6709\u989D\u5916\u7684\u6807\u8BB0"},
        {"Failed reading output", "\u65E0\u6CD5\u8BFB\u53D6\u5B50 Java \u89E3\u91CA\u5668\u7684\u8F93\u51FA\u3002"},
        {"Fatal error", "\u81F4\u547D\u9519\u8BEF:"},
        {"Field access encountered before after", "\u5B57\u6BB5 ({0}) \u4E3A{1}, \u5C06\u4E3A{2}: "},
        {"Field access encountered", "\u9047\u5230\u5B57\u6BB5 ({0}) \u8BBF\u95EE: "},
        {"Field to unwatch not specified", "\u672A\u6307\u5B9A\u8981\u53D6\u6D88\u76D1\u89C6\u7684\u5B57\u6BB5\u3002"},
        {"Field to watch not specified", "\u672A\u6307\u5B9A\u8981\u76D1\u89C6\u7684\u5B57\u6BB5\u3002"},
        {"GC Disabled for", "\u5DF2\u5BF9{0}\u7981\u7528 GC:"},
        {"GC Enabled for", "\u5DF2\u5BF9{0}\u542F\u7528 GC:"},
        {"grouping begin character", "{"},
        {"grouping end character", "}"},
        {"Illegal Argument Exception", "\u975E\u6CD5\u53C2\u6570\u5F02\u5E38\u9519\u8BEF"},
        {"Illegal connector argument", "\u975E\u6CD5\u8FDE\u63A5\u5668\u53C2\u6570: {0}"},
        {"implementor:", "\u5B9E\u73B0\u8005: {0}"},
        {"implements:", "\u5B9E\u73B0: {0}"},
        {"Initializing progname", "\u6B63\u5728\u521D\u59CB\u5316{0}..."},
        {"Input stream closed.", "\u8F93\u5165\u6D41\u5DF2\u5173\u95ED\u3002"},
        {"Interface:", "\u63A5\u53E3: {0}"},
        {"Internal debugger error.", "\u5185\u90E8\u8C03\u8BD5\u5668\u9519\u8BEF\u3002"},
        {"Internal error: null ThreadInfo created", "\u5185\u90E8\u9519\u8BEF: \u521B\u5EFA\u4E86\u7A7A\u503C ThreadInfo"},
        {"Internal error; unable to set", "\u5185\u90E8\u9519\u8BEF; \u65E0\u6CD5\u8BBE\u7F6E{0}"},
        {"Internal exception during operation:", "\u64CD\u4F5C\u671F\u95F4\u51FA\u73B0\u5185\u90E8\u5F02\u5E38\u9519\u8BEF:\n    {0}"},
        {"Internal exception:", "\u5185\u90E8\u5F02\u5E38\u9519\u8BEF:"},
        {"Invalid argument type name", "\u53C2\u6570\u7C7B\u578B\u540D\u79F0\u65E0\u6548"},
        {"Invalid assignment syntax", "\u8D4B\u503C\u8BED\u6CD5\u65E0\u6548"},
        {"Invalid command syntax", "\u547D\u4EE4\u8BED\u6CD5\u65E0\u6548"},
        {"Invalid connect type", "\u8FDE\u63A5\u7C7B\u578B\u65E0\u6548"},
        {"Invalid consecutive invocations", "\u8FDE\u7EED\u8C03\u7528\u65E0\u6548"},
        {"Invalid exception object", "\u5F02\u5E38\u9519\u8BEF\u5BF9\u8C61\u65E0\u6548"},
        {"Invalid line number specified", "\u6307\u5B9A\u7684\u884C\u53F7\u65E0\u6548"},
        {"Invalid <method_name> specification:", "<method_name> \u89C4\u8303\u65E0\u6548\uFF1A{0}"},
        {"Invalid option on class command", "\u7C7B\u547D\u4EE4\u7684\u9009\u9879\u65E0\u6548"},
        {"invalid option", "\u9009\u9879\u65E0\u6548: {0}"},
        {"Invalid thread status.", "\u7EBF\u7A0B\u72B6\u6001\u65E0\u6548\u3002"},
        {"Invalid <thread_id>:", "<thread_id> \u65E0\u6548\uFF1A{0}"},
        {"Invalid transport name:", "\u4F20\u8F93\u540D\u79F0\u65E0\u6548: {0}"},
        {"Invalid <class>.<method_name> specification", "<class>.<method_name> \u89C4\u8303\u65E0\u6548"},
        {"I/O exception occurred:", "\u51FA\u73B0 I/O \u5F02\u5E38\u9519\u8BEF: {0}"},
        {"is an ambiguous method name in", "\"{0}\" \u5728 \"{1}\" \u4E2D\u662F\u4E0D\u660E\u786E\u7684\u65B9\u6CD5\u540D\u79F0"},
        {"is an invalid line number for",  "{0,number,integer} \u662F{1}\u7684\u65E0\u6548\u884C\u53F7"},
        {"is not a valid class name", "\"{0}\" \u4E0D\u662F\u6709\u6548\u7684\u7C7B\u540D\u3002"},
        {"is not a valid field name", "\"{0}\" \u4E0D\u662F\u6709\u6548\u7684\u5B57\u6BB5\u540D\u3002"},
        {"is not a valid id or class name", "\"{0}\" \u4E0D\u662F\u6709\u6548\u7684 ID \u6216\u7C7B\u540D\u3002"},
        {"is not a valid line number or method name for", "\"{0}\" \u4E0D\u662F\u7C7B \"{1}\" \u7684\u6709\u6548\u884C\u53F7\u6216\u65B9\u6CD5\u540D"},
        {"is not a valid method name", "\"{0}\" \u4E0D\u662F\u6709\u6548\u7684\u65B9\u6CD5\u540D\u3002"},
        {"is not a valid thread id", "\"{0}\" \u4E0D\u662F\u6709\u6548\u7684\u7EBF\u7A0B ID\u3002"},
        {"is not a valid threadgroup name", "\"{0}\" \u4E0D\u662F\u6709\u6548\u7684\u7EBF\u7A0B\u7EC4\u540D\u79F0\u3002"},
        {"jdb prompt with no current thread", "> "},
        {"jdb prompt thread name and current stack frame", "{0}[{1,number,integer}] "},
        {"killed", "{0}\u5DF2\u7EC8\u6B62"},
        {"killing thread:", "\u6B63\u5728\u7EC8\u6B62\u7EBF\u7A0B: {0}"},
        {"Line number information not available for", "\u6B64\u4F4D\u7F6E\u7684\u6E90\u884C\u53F7\u4E0D\u53EF\u7528\u3002"},
        {"line number", ":{0,number,integer}"},
        {"list field typename and name", "{0} {1}\n"},
        {"list field typename and name inherited", "{0} {1} (\u7EE7\u627F\u81EA{2})\n"},
        {"list field typename and name hidden", "{0} {1} (\u9690\u85CF)\n"},
        {"Listening at address:", "\u76D1\u542C\u5730\u5740: {0}"},
        {"Local variable information not available.", "\u672C\u5730\u53D8\u91CF\u4FE1\u606F\u4E0D\u53EF\u7528\u3002\u8BF7\u4F7F\u7528 -g \u7F16\u8BD1\u4EE5\u751F\u6210\u53D8\u91CF\u4FE1\u606F"},
        {"Local variables:", "\u672C\u5730\u53D8\u91CF:"},
        {"<location unavailable>", "<\u4F4D\u7F6E\u4E0D\u53EF\u7528>"},
        {"location", "\"\u7EBF\u7A0B={0}\", {1}"},
        {"locationString", "{0}.{1}(), \u884C={2,number,integer} bci={3,number,integer}"},
        {"Main class and arguments must be specified", "\u5FC5\u987B\u6307\u5B9A\u4E3B\u7C7B\u548C\u53C2\u6570"},
        {"Method arguments:", "\u65B9\u6CD5\u53C2\u6570:"},
        {"Method entered:", "\u5DF2\u8FDB\u5165\u65B9\u6CD5: "},
        {"Method exited:",  "\u5DF2\u9000\u51FA\u65B9\u6CD5"},
        {"Method exitedValue:", "\u5DF2\u9000\u51FA\u65B9\u6CD5: \u8FD4\u56DE\u503C = {0}, "},
        {"Method is overloaded; specify arguments", "\u5DF2\u91CD\u8F7D\u65B9\u6CD5{0}; \u8BF7\u6307\u5B9A\u53C2\u6570"},
        {"minus version", "\u8FD9\u662F{0}\u7248\u672C {1,number,integer}.{2,number,integer} (Java SE \u7248\u672C {3})"},
        {"Missing at or in", "\u7F3A\u5C11 \"at\" \u6216 \"in\""},
        {"Monitor information for thread", "\u76D1\u89C6\u7EBF\u7A0B{0}\u7684\u4FE1\u606F:"},
        {"Monitor information for expr", "\u76D1\u89C6{0} ({1}) \u7684\u4FE1\u606F:"},
        {"More than one class named", "\u591A\u4E2A\u7C7B\u7684\u540D\u79F0\u4E3A: ''{0}''"},
        {"native method", "\u672C\u673A\u65B9\u6CD5"},
        {"nested:", "\u5D4C\u5957: {0}"},
        {"No attach address specified.", "\u672A\u6307\u5B9A\u9644\u52A0\u5730\u5740\u3002"},
        {"No breakpoints set.", "\u672A\u8BBE\u7F6E\u65AD\u70B9\u3002"},
        {"No class named", "\u6CA1\u6709\u540D\u4E3A ''{0}'' \u7684\u7C7B"},
        {"No class specified.", "\u672A\u6307\u5B9A\u7C7B\u3002"},
        {"No classpath specified.", "\u672A\u6307\u5B9A\u7C7B\u8DEF\u5F84\u3002"},
        {"No code at line", "{1}\u4E2D\u7684\u884C {0,number,integer} \u5904\u6CA1\u6709\u4EE3\u7801"},
        {"No connect specification.", "\u6CA1\u6709\u8FDE\u63A5\u89C4\u8303\u3002"},
        {"No connector named:", "\u6CA1\u6709\u540D\u4E3A{0}\u7684\u8FDE\u63A5\u5668"},
        {"No current thread", "\u6CA1\u6709\u5F53\u524D\u7EBF\u7A0B"},
        {"No default thread specified:", "\u672A\u6307\u5B9A\u9ED8\u8BA4\u7EBF\u7A0B: \u8BF7\u5148\u4F7F\u7528 \"thread\" \u547D\u4EE4\u3002"},
        {"No exception object specified.", "\u672A\u6307\u5B9A\u5F02\u5E38\u9519\u8BEF\u5BF9\u8C61\u3002"},
        {"No exceptions caught.", "\u672A\u6355\u83B7\u5230\u5F02\u5E38\u9519\u8BEF\u3002"},
        {"No expression specified.", "\u672A\u6307\u5B9A\u8868\u8FBE\u5F0F\u3002"},
        {"No field in", "{1}\u4E2D\u6CA1\u6709\u5B57\u6BB5{0}"},
        {"No frames on the current call stack", "\u5F53\u524D\u8C03\u7528\u5806\u6808\u4E0A\u6CA1\u6709\u5E27"},
        {"No linenumber information for", "{0}\u6CA1\u6709\u884C\u53F7\u4FE1\u606F\u3002\u8BF7\u5C1D\u8BD5\u5728\u542F\u7528\u8C03\u8BD5\u7684\u60C5\u51B5\u4E0B\u7F16\u8BD1\u3002"},
        {"No local variables", "\u6CA1\u6709\u672C\u5730\u53D8\u91CF"},
        {"No method in", "{1}\u4E2D\u6CA1\u6709\u65B9\u6CD5{0}"},
        {"No method specified.", "\u672A\u6307\u5B9A\u65B9\u6CD5\u3002"},
        {"No monitor numbered:", "\u6CA1\u6709\u7F16\u53F7\u4E3A {0} \u7684\u76D1\u89C6\u5668"},
        {"No monitors owned", "  \u4E0D\u62E5\u6709\u76D1\u89C6\u5668"},
        {"No object specified.", "\u672A\u6307\u5B9A\u5BF9\u8C61\u3002"},
        {"No objects specified.", "\u672A\u6307\u5B9A\u5BF9\u8C61\u3002"},
        {"No save index specified.", "\u672A\u6307\u5B9A\u4FDD\u5B58\u7D22\u5F15\u3002"},
        {"No saved values", "\u6CA1\u6709\u4FDD\u5B58\u7684\u503C"},
        {"No source information available for:", "\u6CA1\u6709\u53EF\u7528\u4E8E{0}\u7684\u6E90\u4FE1\u606F"},
        {"No sourcedebugextension specified", "\u672A\u6307\u5B9A SourceDebugExtension"},
        {"No sourcepath specified.", "\u672A\u6307\u5B9A\u6E90\u8DEF\u5F84\u3002"},
        {"No thread specified.", "\u672A\u6307\u5B9A\u7EBF\u7A0B\u3002"},
        {"No VM connected", "\u672A\u8FDE\u63A5 VM"},
        {"No waiters", "  \u6CA1\u6709\u7B49\u5F85\u8FDB\u7A0B"},
        {"not a class", "{0}\u4E0D\u662F\u7C7B"},
        {"Not a monitor number:", "\u4E0D\u662F\u76D1\u89C6\u5668\u7F16\u53F7: ''{0}''"},
        {"not found (try the full name)", "\u627E\u4E0D\u5230{0} (\u8BF7\u5C1D\u8BD5\u4F7F\u7528\u5168\u540D)"},
        {"Not found:", "\u627E\u4E0D\u5230: {0}"},
        {"not found", "\u627E\u4E0D\u5230{0}"},
        {"Not owned", "  \u4E0D\u62E5\u6709"},
        {"Not waiting for a monitor", "  \u672A\u7B49\u5F85\u76D1\u89C6\u5668"},
        {"Nothing suspended.", "\u672A\u6302\u8D77\u4EFB\u4F55\u5BF9\u8C61\u3002"},
        {"object description and id", "({0}){1}"},
        {"Operation is not supported on the target VM", "\u76EE\u6807 VM \u4E0D\u652F\u6301\u8BE5\u64CD\u4F5C"},
        {"operation not yet supported", "\u5C1A\u4E0D\u652F\u6301\u8BE5\u64CD\u4F5C"},
        {"Owned by:", "  \u62E5\u6709\u8005: {0}, \u6761\u76EE\u8BA1\u6570: {1,number,integer}"},
        {"Owned monitor:", "  \u62E5\u6709\u7684\u76D1\u89C6\u5668: {0}"},
        {"Parse exception:", "\u8BED\u6CD5\u5206\u6790\u5F02\u5E38\u9519\u8BEF: {0}"},
        {"printclearcommandusage", "\u7528\u6CD5\uFF1Aclear <class>:<line_number> \u6216\n      clear <class>.<method_name>[(argument_type,...)]"},
        {"printstopcommandusage",
         "\u7528\u6CD5\uFF1Astop [go|thread] [<thread_id>] <at|in> <location>\n  \u5982\u679C\u6307\u5B9A \"go\"\uFF0C\u5219\u5728\u505C\u6B62\u540E\u7ACB\u5373\u6062\u590D\n  \u5982\u679C\u6307\u5B9A \"thread\"\uFF0C\u5219\u4EC5\u6302\u8D77\u5728\u5176\u4E2D\u505C\u6B62\u7684\u7EBF\u7A0B\n  \u5982\u679C\u65E2\u672A\u6307\u5B9A \"go\" \u4E5F\u672A\u6307\u5B9A \"thread\"\uFF0C\u5219\u6302\u8D77\u6240\u6709\u7EBF\u7A0B\n  \u5982\u679C\u6307\u5B9A\u4EE5\u6574\u6570\u8868\u793A\u7684 <thread_id>\uFF0C\u5219\u4EC5\u5728\u6307\u5B9A\u7684\u7EBF\u7A0B\u4E2D\u505C\u6B62\n  \"at\" \u548C \"in\" \u7684\u542B\u4E49\u76F8\u540C\n  <location> \u53EF\u4EE5\u662F\u884C\u53F7\u6216\u65B9\u6CD5\uFF1A\n    <class_id>:<line_number>\n    <class_id>.<method>[(argument_type,...)]"
        },
        {"Removed:", "\u5DF2\u5220\u9664: {0}"},
        {"Requested stack frame is no longer active:", "\u8BF7\u6C42\u7684\u5806\u6808\u5E27\u4E0D\u518D\u6709\u6548: {0,number,integer}"},
        {"run <args> command is valid only with launched VMs", "'run <args>' \u547D\u4EE4\u4EC5\u5BF9\u542F\u52A8\u7684 VM \u6709\u6548"},
        {"run", "\u8FD0\u884C{0}"},
        {"saved", "{0}\u5DF2\u4FDD\u5B58"},
        {"Set deferred", "\u8BBE\u7F6E\u5EF6\u8FDF\u7684{0}"},
        {"Set", "\u8BBE\u7F6E{0}"},
        {"Source file not found:", "\u627E\u4E0D\u5230\u6E90\u6587\u4EF6: {0}"},
        {"source line number and line", "{0,number,integer}    {1}"},
        {"source line number current line and line", "{0,number,integer} => {1}"},
        {"sourcedebugextension", "SourceDebugExtension -- {0}"},
        {"Specify class and method", "\u6307\u5B9A\u7C7B\u548C\u65B9\u6CD5"},
        {"Specify classes to redefine", "\u6307\u5B9A\u8981\u91CD\u65B0\u5B9A\u4E49\u7684\u7C7B"},
        {"Specify file name for class", "\u6307\u5B9A\u7C7B{0}\u7684\u6587\u4EF6\u540D"},
        {"stack frame dump with pc", "  [{0,number,integer}] {1}.{2} ({3}), pc = {4}"},
        {"stack frame dump", "  [{0,number,integer}] {1}.{2} ({3})"},
        {"Step completed:", "\u5DF2\u5B8C\u6210\u7684\u6B65\u9AA4: "},
        {"Stopping due to deferred breakpoint errors.", "\u7531\u4E8E\u5EF6\u8FDF\u65AD\u70B9\u9519\u8BEF\u800C\u505C\u6B62\u3002\n"},
        {"subclass:", "\u5B50\u7C7B: {0}"},
        {"subinterface:", "\u5B50\u63A5\u53E3: {0}"},
        {"tab", "\t{0}"},
        {"Target VM failed to initialize.", "\u65E0\u6CD5\u521D\u59CB\u5316\u76EE\u6807 VM\u3002"},
        {"The application exited", "\u5E94\u7528\u7A0B\u5E8F\u5DF2\u9000\u51FA"},
        {"The application has been disconnected", "\u5E94\u7528\u7A0B\u5E8F\u5DF2\u65AD\u5F00\u8FDE\u63A5"},
        {"The gc command is no longer necessary.", "\u4E0D\u518D\u9700\u8981 'gc' \u547D\u4EE4\u3002\n\u6240\u6709\u5BF9\u8C61\u5DF2\u7167\u5E38\u8FDB\u884C\u5783\u573E\u6536\u96C6\u3002\u8BF7\u4F7F\u7528 'enablegc' \u548C 'disablegc'\n\u547D\u4EE4\u6765\u63A7\u5236\u5404\u4E2A\u5BF9\u8C61\u7684\u5783\u573E\u6536\u96C6\u3002"},
        {"The load command is no longer supported.", "\u4E0D\u518D\u652F\u6301 'load' \u547D\u4EE4\u3002"},
        {"The memory command is no longer supported.", "\u4E0D\u518D\u652F\u6301 'memory' \u547D\u4EE4\u3002"},
        {"The VM does not use paths", "VM \u4E0D\u4F7F\u7528\u8DEF\u5F84"},
        {"Thread is not running (no stack).", "\u7EBF\u7A0B\u672A\u8FD0\u884C (\u6CA1\u6709\u5806\u6808)\u3002"},
        {"Thread number not specified.", "\u672A\u6307\u5B9A\u7EBF\u7A0B\u7F16\u53F7\u3002"},
        {"Thread:", "{0}:"},
        {"Thread Group:", "\u7EC4{0}:"},
        {"Thread description name unknownStatus BP",  "  {0} {1}\u672A\u77E5 (\u5728\u65AD\u70B9\u5904)"},
        {"Thread description name unknownStatus",     "  {0} {1}\u672A\u77E5"},
        {"Thread description name zombieStatus BP",   "  {0} {1}\u5904\u4E8E\u50F5\u6B7B\u72B6\u6001 (\u5728\u65AD\u70B9\u5904)"},
        {"Thread description name zombieStatus",      "  {0} {1}\u5904\u4E8E\u50F5\u6B7B\u72B6\u6001"},
        {"Thread description name runningStatus BP",  "  {0} {1}\u6B63\u5728\u8FD0\u884C (\u5728\u65AD\u70B9\u5904)"},
        {"Thread description name runningStatus",     "  {0} {1}\u6B63\u5728\u8FD0\u884C"},
        {"Thread description name sleepingStatus BP", "  {0} {1}\u6B63\u5728\u4F11\u7720 (\u5728\u65AD\u70B9\u5904)"},
        {"Thread description name sleepingStatus",    "  {0} {1}\u6B63\u5728\u4F11\u7720"},
        {"Thread description name waitingStatus BP",  "  {0} {1}\u6B63\u5728\u7B49\u5F85\u76D1\u89C6\u5668 (\u5728\u65AD\u70B9\u5904)"},
        {"Thread description name waitingStatus",     "  {0} {1}\u6B63\u5728\u7B49\u5F85\u76D1\u89C6\u5668"},
        {"Thread description name condWaitstatus BP", "  {0} {1}\u6B63\u5728\u6267\u884C\u6761\u4EF6\u7B49\u5F85 (\u5728\u65AD\u70B9\u5904)"},
        {"Thread description name condWaitstatus",    "  {0} {1}\u6B63\u5728\u6267\u884C\u6761\u4EF6\u7B49\u5F85"},
        {"Thread has been resumed", "\u5DF2\u6062\u590D\u7EBF\u7A0B"},
        {"Thread not suspended", "\u672A\u6302\u8D77\u7EBF\u7A0B"},
        {"thread group number description name", "{0,number,integer}\u3002{1} {2}"},
        {"Threadgroup name not specified.", "\u672A\u6307\u5B9A\u7EBF\u7A0B\u7EC4\u540D\u3002"},
        {"<thread_id> option not valid until the VM is started with the run command",
         "\u5728\u4F7F\u7528 run \u547D\u4EE4\u542F\u52A8 VM \u524D\uFF0C<thread_id> \u9009\u9879\u65E0\u6548"},
        {"Threads must be suspended", "\u5FC5\u987B\u6302\u8D77\u7EBF\u7A0B"},
        {"trace method exit in effect for", "\u6B63\u5728\u5BF9{0}\u5B9E\u884C trace method exit"},
        {"trace method exits in effect", "\u6B63\u5728\u5B9E\u884C trace method exits"},
        {"trace methods in effect", "\u6B63\u5728\u5B9E\u884C trace methods"},
        {"trace go method exit in effect for", "\u6B63\u5728\u5BF9{0}\u5B9E\u884C trace go method exit"},
        {"trace go method exits in effect", "\u6B63\u5728\u5B9E\u884C trace go method exits"},
        {"trace go methods in effect", "\u6B63\u5728\u5B9E\u884C trace go methods"},
        {"trace not in effect", "\u672A\u5B9E\u884C trace"},
        {"Unable to attach to target VM.", "\u65E0\u6CD5\u9644\u52A0\u5230\u76EE\u6807 VM\u3002"},
        {"Unable to display process output:", "\u65E0\u6CD5\u663E\u793A\u8FDB\u7A0B\u8F93\u51FA: {0}"},
        {"Unable to launch target VM.", "\u65E0\u6CD5\u542F\u52A8\u76EE\u6807 VM\u3002"},
        {"Unable to set deferred", "\u65E0\u6CD5\u8BBE\u7F6E\u5EF6\u8FDF\u7684{0}: {1}"},
        {"Unable to set main class and arguments", "\u65E0\u6CD5\u8BBE\u7F6E\u4E3B\u7C7B\u548C\u53C2\u6570"},
        {"Unable to set", "\u65E0\u6CD5\u8BBE\u7F6E{0}: {1}"},
        {"Unexpected event type", "\u610F\u5916\u7684\u4E8B\u4EF6\u7C7B\u578B: {0}"},
        {"unknown", "\u672A\u77E5"},
        {"Unmonitoring", "\u53D6\u6D88\u76D1\u89C6{0} "},
        {"Unrecognized command.  Try help...", "\u65E0\u6CD5\u8BC6\u522B\u7684\u547D\u4EE4: ''{0}''\u3002\u8BF7\u5C1D\u8BD5\u83B7\u5F97\u5E2E\u52A9..."},
        {"Usage: catch exception", "\u7528\u6CD5: catch [uncaught|caught|all] <class id>|<class pattern>"},
        {"Usage: ignore exception", "\u7528\u6CD5: ignore [uncaught|caught|all] <class id>|<class pattern>"},
        {"Usage: down [n frames]", "\u7528\u6CD5: down [n frames]"},
        {"Usage: kill <thread id> <throwable>", "\u7528\u6CD5: kill <thread id> <throwable>"},
        {"Usage: read <command-filename>", "\u7528\u6CD5: read <command-filename>"},
        {"Usage: unmonitor <monitor#>", "\u7528\u6CD5: unmonitor <monitor#>"},
        {"Usage: up [n frames]", "\u7528\u6CD5: up [n frames]"},
        {"Use java minus X to see", "\u4F7F\u7528 'java -X' \u53EF\u4EE5\u67E5\u770B\u53EF\u7528\u7684\u975E\u6807\u51C6\u9009\u9879"},
        {"VM already running. use cont to continue after events.", "VM \u5DF2\u5728\u8FD0\u884C\u3002\u8BF7\u4F7F\u7528 'cont' \u4EE5\u5728\u4E8B\u4EF6\u7ED3\u675F\u540E\u7EE7\u7EED\u3002"},
        {"VM Started:", "VM \u5DF2\u542F\u52A8: "},
        {"vmstartexception", "VM \u542F\u52A8\u5F02\u5E38\u9519\u8BEF: {0}"},
        {"Waiting for monitor:", "   \u6B63\u5728\u7B49\u5F85\u76D1\u89C6\u5668: {0}"},
        {"Waiting thread:", " \u6B63\u5728\u7B49\u5F85\u7EBF\u7A0B: {0}"},
        {"watch accesses of", "\u76D1\u89C6{0}.{1}\u7684\u8BBF\u95EE"},
        {"watch modification of", "\u76D1\u89C6{0}.{1}\u7684\u4FEE\u6539"},
        {"zz help text",
             "** \u547D\u4EE4\u5217\u8868 **\nconnectors                -- \u5217\u51FA\u6B64 VM \u4E2D\u53EF\u7528\u7684\u8FDE\u63A5\u5668\u548C\u4F20\u8F93\n\nrun [class [args]]        -- \u5F00\u59CB\u6267\u884C\u5E94\u7528\u7A0B\u5E8F\u7684\u4E3B\u7C7B\n\nthreads [threadgroup]     -- \u5217\u51FA\u7EBF\u7A0B\nthread <thread id>        -- \u8BBE\u7F6E\u9ED8\u8BA4\u7EBF\u7A0B\nsuspend [thread id(s)]    -- \u6302\u8D77\u7EBF\u7A0B (\u9ED8\u8BA4\u503C: all)\nresume [thread id(s)]     -- \u6062\u590D\u7EBF\u7A0B (\u9ED8\u8BA4\u503C: all)\nwhere [<thread id> | all] -- \u8F6C\u50A8\u7EBF\u7A0B\u7684\u5806\u6808\nwherei [<thread id> | all]-- \u8F6C\u50A8\u7EBF\u7A0B\u7684\u5806\u6808, \u4EE5\u53CA pc \u4FE1\u606F\nup [n frames]             -- \u4E0A\u79FB\u7EBF\u7A0B\u7684\u5806\u6808\ndown [n frames]           -- \u4E0B\u79FB\u7EBF\u7A0B\u7684\u5806\u6808\nkill <thread id> <expr>   -- \u7EC8\u6B62\u5177\u6709\u7ED9\u5B9A\u7684\u5F02\u5E38\u9519\u8BEF\u5BF9\u8C61\u7684\u7EBF\u7A0B\ninterrupt <thread id>     -- \u4E2D\u65AD\u7EBF\u7A0B\n\nprint <expr>              -- \u8F93\u51FA\u8868\u8FBE\u5F0F\u7684\u503C\ndump <expr>               -- \u8F93\u51FA\u6240\u6709\u5BF9\u8C61\u4FE1\u606F\neval <expr>               -- \u5BF9\u8868\u8FBE\u5F0F\u6C42\u503C (\u4E0E print \u76F8\u540C)\nset <lvalue> = <expr>     -- \u5411\u5B57\u6BB5/\u53D8\u91CF/\u6570\u7EC4\u5143\u7D20\u5206\u914D\u65B0\u503C\nlocals                    -- \u8F93\u51FA\u5F53\u524D\u5806\u6808\u5E27\u4E2D\u7684\u6240\u6709\u672C\u5730\u53D8\u91CF\n\nclasses                   -- \u5217\u51FA\u5F53\u524D\u5DF2\u77E5\u7684\u7C7B\nclass <class id>          -- \u663E\u793A\u5DF2\u547D\u540D\u7C7B\u7684\u8BE6\u7EC6\u8D44\u6599\nmethods <class id>        -- \u5217\u51FA\u7C7B\u7684\u65B9\u6CD5\nfields <class id>         -- \u5217\u51FA\u7C7B\u7684\u5B57\u6BB5\n\nthreadgroups              -- \u5217\u51FA\u7EBF\u7A0B\u7EC4\nthreadgroup <name>        -- \u8BBE\u7F6E\u5F53\u524D\u7EBF\u7A0B\u7EC4\n\nstop [go|thread] [<thread_id>] <at|in> <location>\n                          -- \u8BBE\u7F6E\u65AD\u70B9\n                          -- \u5982\u679C\u672A\u63D0\u4F9B\u4EFB\u4F55\u9009\u9879\uFF0C\u5219\u5C06\u6253\u5370\u5F53\u524D\u65AD\u70B9\u5217\u8868\n                          -- \u5982\u679C\u6307\u5B9A \"go\"\uFF0C\u5219\u5728\u505C\u6B62\u540E\u7ACB\u5373\u6062\u590D\n                          -- \u5982\u679C\u6307\u5B9A \"thread\"\uFF0C\u5219\u4EC5\u6302\u8D77\u5728\u5176\u4E2D\u505C\u6B62\u7684\u7EBF\u7A0B\n                          -- \u5982\u679C\u65E2\u672A\u6307\u5B9A \"go\" \u4E5F\u672A\u6307\u5B9A \"thread\"\uFF0C\u5219\u6302\u8D77\u6240\u6709\u7EBF\u7A0B\n                          -- \u5982\u679C\u6307\u5B9A\u4EE5\u6574\u6570\u8868\u793A\u7684 <thread_id>\uFF0C\u5219\u4EC5\u5728\u6307\u5B9A\u7684\u7EBF\u7A0B\u4E2D\u505C\u6B62\n                          -- \"at\" \u548C \"in\" \u7684\u542B\u4E49\u76F8\u540C\n                          -- <location> \u53EF\u4EE5\u662F\u884C\u53F7\u6216\u65B9\u6CD5\uFF1A\n                          --   <class_id>:<line_number>\n                          --   <class_id>.<method>[(argument_type,...)]\nclear <class id>.<method>[(argument_type,...)]\n                          -- \u6E05\u9664\u65B9\u6CD5\u4E2D\u7684\u65AD\u70B9\nclear <class id>:<line>   -- \u6E05\u9664\u884C\u4E2D\u7684\u65AD\u70B9\nclear                     -- \u5217\u51FA\u65AD\u70B9\ncatch [uncaught|caught|all] <class id>|<class pattern>\n                          -- \u51FA\u73B0\u6307\u5B9A\u7684\u5F02\u5E38\u9519\u8BEF\u65F6\u4E2D\u65AD\nignore [uncaught|caught|all] <class id>|<class pattern>\n                          -- \u5BF9\u4E8E\u6307\u5B9A\u7684\u5F02\u5E38\u9519\u8BEF, \u53D6\u6D88 'catch'\nwatch [access|all] <class id>.<field name>\n                          -- \u76D1\u89C6\u5BF9\u5B57\u6BB5\u7684\u8BBF\u95EE/\u4FEE\u6539\nunwatch [access|all] <class id>.<field name>\n                          -- \u505C\u6B62\u76D1\u89C6\u5BF9\u5B57\u6BB5\u7684\u8BBF\u95EE/\u4FEE\u6539\ntrace [go] methods [thread]\n                          -- \u8DDF\u8E2A\u65B9\u6CD5\u8FDB\u5165\u548C\u9000\u51FA\u3002\n                          -- \u9664\u975E\u6307\u5B9A 'go', \u5426\u5219\u6302\u8D77\u6240\u6709\u7EBF\u7A0B\ntrace [go] method exit | exits [thread]\n                          -- \u8DDF\u8E2A\u5F53\u524D\u65B9\u6CD5\u7684\u9000\u51FA, \u6216\u8005\u6240\u6709\u65B9\u6CD5\u7684\u9000\u51FA\n                          -- \u9664\u975E\u6307\u5B9A 'go', \u5426\u5219\u6302\u8D77\u6240\u6709\u7EBF\u7A0B\nuntrace [methods]         -- \u505C\u6B62\u8DDF\u8E2A\u65B9\u6CD5\u8FDB\u5165\u548C/\u6216\u9000\u51FA\nstep                      -- \u6267\u884C\u5F53\u524D\u884C\nstep up                   -- \u4E00\u76F4\u6267\u884C, \u76F4\u5230\u5F53\u524D\u65B9\u6CD5\u8FD4\u56DE\u5230\u5176\u8C03\u7528\u65B9\nstepi                     -- \u6267\u884C\u5F53\u524D\u6307\u4EE4\n\u4E0B\u4E00\u6B65                      -- \u6B65\u8FDB\u4E00\u884C (\u6B65\u8FC7\u8C03\u7528)\ncont                      -- \u4ECE\u65AD\u70B9\u5904\u7EE7\u7EED\u6267\u884C\n\nlist [line number|method] -- \u8F93\u51FA\u6E90\u4EE3\u7801\nuse (\u6216 sourcepath) [source file path]\n                          -- \u663E\u793A\u6216\u66F4\u6539\u6E90\u8DEF\u5F84\nexclude [<class pattern>, ... | \"none\"]\n                          -- \u5BF9\u4E8E\u6307\u5B9A\u7684\u7C7B, \u4E0D\u62A5\u544A\u6B65\u9AA4\u6216\u65B9\u6CD5\u4E8B\u4EF6\nclasspath                 -- \u4ECE\u76EE\u6807 VM \u8F93\u51FA\u7C7B\u8DEF\u5F84\u4FE1\u606F\n\nmonitor <command>         -- \u6BCF\u6B21\u7A0B\u5E8F\u505C\u6B62\u65F6\u6267\u884C\u547D\u4EE4\nmonitor                   -- \u5217\u51FA\u76D1\u89C6\u5668\nunmonitor <monitor#>      -- \u5220\u9664\u76D1\u89C6\u5668\nread <filename>           -- \u8BFB\u53D6\u5E76\u6267\u884C\u547D\u4EE4\u6587\u4EF6\n\nlock <expr>               -- \u8F93\u51FA\u5BF9\u8C61\u7684\u9501\u4FE1\u606F\nthreadlocks [thread id]   -- \u8F93\u51FA\u7EBF\u7A0B\u7684\u9501\u4FE1\u606F\n\npop                       -- \u901A\u8FC7\u5F53\u524D\u5E27\u51FA\u6808, \u4E14\u5305\u542B\u5F53\u524D\u5E27\nreenter                   -- \u4E0E pop \u76F8\u540C, \u4F46\u91CD\u65B0\u8FDB\u5165\u5F53\u524D\u5E27\nredefine <class id> <class file name>\n                          -- \u91CD\u65B0\u5B9A\u4E49\u7C7B\u7684\u4EE3\u7801\n\ndisablegc <expr>          -- \u7981\u6B62\u5BF9\u8C61\u7684\u5783\u573E\u6536\u96C6\nenablegc <expr>           -- \u5141\u8BB8\u5BF9\u8C61\u7684\u5783\u573E\u6536\u96C6\n\n!!                        -- \u91CD\u590D\u6267\u884C\u6700\u540E\u4E00\u4E2A\u547D\u4EE4\n<n> <command>             -- \u5C06\u547D\u4EE4\u91CD\u590D\u6267\u884C n \u6B21\n# <command>               -- \u653E\u5F03 (\u65E0\u64CD\u4F5C)\nhelp (\u6216 ?)               -- \u5217\u51FA\u547D\u4EE4\ndbgtrace [flag]           -- \u4E0E dbgtrace \u547D\u4EE4\u884C\u9009\u9879\u76F8\u540C\nversion                   -- \u8F93\u51FA\u7248\u672C\u4FE1\u606F\nexit (\u6216 quit)            -- \u9000\u51FA\u8C03\u8BD5\u5668\n\n<class id>: \u5E26\u6709\u7A0B\u5E8F\u5305\u9650\u5B9A\u7B26\u7684\u5B8C\u6574\u7C7B\u540D\n<class pattern>: \u5E26\u6709\u524D\u5BFC\u6216\u5C3E\u968F\u901A\u914D\u7B26 ('*') \u7684\u7C7B\u540D\n<thread id>: 'threads' \u547D\u4EE4\u4E2D\u62A5\u544A\u7684\u7EBF\u7A0B\u7F16\u53F7\n<expr>: Java(TM) \u7F16\u7A0B\u8BED\u8A00\u8868\u8FBE\u5F0F\u3002\n\u652F\u6301\u5927\u591A\u6570\u5E38\u89C1\u8BED\u6CD5\u3002\n\n\u53EF\u4EE5\u5C06\u542F\u52A8\u547D\u4EE4\u7F6E\u4E8E \"jdb.ini\" \u6216 \".jdbrc\" \u4E2D\n\u4F4D\u4E8E user.home \u6216 user.dir \u4E2D"},
        {"zz usage text",
             "\u7528\u6CD5\uFF1A{0} <\u9009\u9879> <\u7C7B> <\u53C2\u6570>\n\n\u5176\u4E2D\uFF0C\u9009\u9879\u5305\u62EC\uFF1A\n    -? -h --help -help \u8F93\u51FA\u6B64\u6D88\u606F\u5E76\u9000\u51FA\n    -sourcepath <\u7531 \"{1}\" \u5206\u9694\u7684\u76EE\u5F55>\n                      \u8981\u5728\u5176\u4E2D\u67E5\u627E\u6E90\u6587\u4EF6\u7684\u76EE\u5F55\n    -attach <\u5730\u5740>\n                      \u4F7F\u7528\u6807\u51C6\u8FDE\u63A5\u5668\u9644\u52A0\u5230\u6307\u5B9A\u5730\u5740\u5904\u6B63\u5728\u8FD0\u884C\u7684 VM\n    -listen <\u5730\u5740>\n                      \u7B49\u5F85\u6B63\u5728\u8FD0\u884C\u7684 VM \u4F7F\u7528\u6807\u51C6\u8FDE\u63A5\u5668\u5728\u6307\u5B9A\u5730\u5740\u5904\u8FDE\u63A5\n    -listenany\n                      \u7B49\u5F85\u6B63\u5728\u8FD0\u884C\u7684 VM \u4F7F\u7528\u6807\u51C6\u8FDE\u63A5\u5668\u5728\u4EFB\u4F55\u53EF\u7528\u5730\u5740\u5904\u8FDE\u63A5\n    -launch\n                      \u7ACB\u5373\u542F\u52A8 VM \u800C\u4E0D\u662F\u7B49\u5F85 ''run'' \u547D\u4EE4\n    -listconnectors   \u5217\u51FA\u6B64 VM \u4E2D\u7684\u53EF\u7528\u8FDE\u63A5\u5668\n    -connect <\u8FDE\u63A5\u5668\u540D\u79F0>:<\u540D\u79F0 1>=<\u503C 1>,...\n                      \u4F7F\u7528\u6240\u5217\u53C2\u6570\u503C\u901A\u8FC7\u6307\u5B9A\u7684\u8FDE\u63A5\u5668\u8FDE\u63A5\u5230\u76EE\u6807 VM\n    -dbgtrace [flags] \u8F93\u51FA\u8C03\u8BD5 {0} \u7684\u4FE1\u606F\n    -tclient          \u5728 HotSpot(TM) \u5BA2\u6237\u673A\u7F16\u8BD1\u5668\u4E2D\u8FD0\u884C\u5E94\u7528\u7A0B\u5E8F\n    -tserver          \u5728 HotSpot(TM) \u670D\u52A1\u5668\u7F16\u8BD1\u5668\u4E2D\u8FD0\u884C\u5E94\u7528\u7A0B\u5E8F\n\n\u8F6C\u53D1\u5230\u88AB\u8C03\u8BD5\u8FDB\u7A0B\u7684\u9009\u9879\uFF1A\n    -v -verbose[:class|gc|jni]\n                      \u542F\u7528\u8BE6\u7EC6\u6A21\u5F0F\n    -D<\u540D\u79F0>=<\u503C>  \u8BBE\u7F6E\u7CFB\u7EDF\u5C5E\u6027\n    -classpath <\u7531 \"{1}\" \u5206\u9694\u7684\u76EE\u5F55>\n                      \u5217\u51FA\u8981\u5728\u5176\u4E2D\u67E5\u627E\u7C7B\u7684\u76EE\u5F55\n    -X<\u9009\u9879>        \u975E\u6807\u51C6\u76EE\u6807 VM \u9009\u9879\n\n<\u7C7B> \u662F\u8981\u5F00\u59CB\u8C03\u8BD5\u7684\u7C7B\u7684\u540D\u79F0\n<\u53C2\u6570> \u662F\u4F20\u9012\u5230 <\u7C7B> \u7684 main() \u65B9\u6CD5\u7684\u53C2\u6570\n\n\u8981\u83B7\u5F97\u547D\u4EE4\u7684\u5E2E\u52A9\uFF0C\u8BF7\u5728 {0} \u63D0\u793A\u4E0B\u952E\u5165 ''help''"},
        // END OF MATERIAL TO LOCALIZE
        };

        return temp;
    }
}
