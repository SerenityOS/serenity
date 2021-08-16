<?xml version="1.0"?>
<!--
 Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

 This code is free software; you can redistribute it and/or modify it
 under the terms of the GNU General Public License version 2 only, as
 published by the Free Software Foundation.

 This code is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 version 2 for more details (a copy is included in the LICENSE file that
 accompanied this code).

 You should have received a copy of the GNU General Public License version
 2 along with this work; if not, write to the Free Software Foundation,
 Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

 Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 or visit www.oracle.com if you need additional information or have any
 questions.

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="jvmtiLib.xsl"/>

<xsl:output method="text" indent="no" omit-xml-declaration="yes"/>

<xsl:param name="trace"></xsl:param>
<xsl:param name="interface"></xsl:param>


<xsl:template match="specification">
  <xsl:call-template name="sourceHeader"/>
  <xsl:text>
# include "precompiled.hpp"
# include "classfile/javaClasses.inline.hpp"
# include "classfile/vmClasses.hpp"
# include "memory/resourceArea.hpp"
# include "utilities/macros.hpp"
#if INCLUDE_JVMTI
# include "logging/log.hpp"
# include "oops/oop.inline.hpp"
# include "prims/jvmtiEnter.inline.hpp"
# include "prims/jvmtiRawMonitor.hpp"
# include "prims/jvmtiUtil.hpp"
# include "runtime/fieldDescriptor.inline.hpp"
# include "runtime/jniHandles.hpp"
# include "runtime/threadSMR.hpp"

</xsl:text>

  <xsl:if test="$trace = 'Trace'">
   <xsl:text>
#ifdef JVMTI_TRACE
</xsl:text>
  </xsl:if>

 <xsl:if test="$trace != 'Trace'">
    <xsl:text>

// Error names
const char* JvmtiUtil::_error_names[] = {
</xsl:text>
    <xsl:call-template name="fillEntityName">
      <xsl:with-param name="entities" select="errorsection/errorcategory/errorid"/>
    </xsl:call-template>
    <xsl:text>
};


// Event threaded
const bool JvmtiUtil::_event_threaded[] = {
</xsl:text>
    <xsl:call-template name="fillEventThreaded">
      <xsl:with-param name="entities" select="eventsection/event"/>
    </xsl:call-template>
    <xsl:text>
};

</xsl:text>
    <xsl:call-template name="eventCapabilitiesTest"/>
 </xsl:if>

 <xsl:if test="$trace = 'Trace'">

<!--  all this just to return the highest event number -->
  <xsl:variable name="maxEvent">
    <xsl:for-each select="eventsection/event">
      <xsl:variable name="mynum" select="@num"/>
      <xsl:if test="count(../../eventsection/event[@num &gt; $mynum]) = 0">
        <xsl:value-of select="@num"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:text>jbyte JvmtiTrace::_event_trace_flags[</xsl:text>
  <xsl:value-of select="1+$maxEvent"/>
  <xsl:text>];

jint JvmtiTrace::_max_event_index = </xsl:text>
  <xsl:value-of select="$maxEvent"/>
  <xsl:text>;

// Event names
const char* JvmtiTrace::_event_names[] = {
</xsl:text>
    <xsl:call-template name="fillEntityName">
      <xsl:with-param name="entities" select="eventsection/event"/>
    </xsl:call-template>
    <xsl:text>
};
</xsl:text>
    <xsl:apply-templates select="//constants[@kind='enum']"/>
  </xsl:if>
  <xsl:apply-templates select="functionsection"/>

  <xsl:if test="$trace='Trace'">
   <xsl:text>
#endif /*JVMTI_TRACE */
</xsl:text>
  </xsl:if>

</xsl:template>

<xsl:template match="constants">
  <xsl:text>

// </xsl:text>
  <xsl:value-of select="@label"/>
  <xsl:text> names
const char* </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>ConstantNames[] = {
</xsl:text>
  <xsl:apply-templates select="constant" mode="constname"/>
  <xsl:text>  NULL
};

// </xsl:text>
  <xsl:value-of select="@label"/>
  <xsl:text> value
jint </xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>ConstantValues[] = {
</xsl:text>
  <xsl:apply-templates select="constant" mode="constvalue"/>
  <xsl:text>  0
};

</xsl:text>
</xsl:template>

<xsl:template match="constant" mode="constname">
  <xsl:text>  "</xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>",
</xsl:text>
</xsl:template>

<xsl:template match="constant" mode="constvalue">
  <xsl:text>  </xsl:text>
  <xsl:value-of select="@num"/>
  <xsl:text>,
</xsl:text>
</xsl:template>

<xsl:template name="eventCapabilitiesTest">
  <xsl:text>

// Check Event Capabilities
const bool JvmtiUtil::has_event_capability(jvmtiEvent event_type, const jvmtiCapabilities* capabilities_ptr) {
  switch (event_type) {
</xsl:text>
  <xsl:for-each select="//eventsection/event">
    <xsl:variable name="capa" select="capabilities/required"/>
    <xsl:if test="count($capa)">
        <xsl:text>    case </xsl:text>
        <xsl:value-of select="@const"/>
        <xsl:text>:
      return capabilities_ptr-&gt;</xsl:text>
        <xsl:value-of select="$capa/@id"/>
        <xsl:text> != 0;
</xsl:text>
    </xsl:if>
  </xsl:for-each>
  <xsl:text>  default: break; }
  // if it does not have a capability it is required
  return JNI_TRUE;
}

</xsl:text>
</xsl:template>

<xsl:template match="functionsection">
  <xsl:if test="$trace='Trace'">

<!--  all this just to return the highest function number -->
  <xsl:variable name="maxFunction">
    <xsl:for-each select="category/function">
      <xsl:variable name="mynum" select="@num"/>
      <xsl:if test="count(../../category/function[@num &gt; $mynum]) = 0">
        <xsl:value-of select="@num"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:text>jbyte JvmtiTrace::_trace_flags[</xsl:text>
  <xsl:value-of select="1+$maxFunction"/>
  <xsl:text>];

jint JvmtiTrace::_max_function_index = </xsl:text>
  <xsl:value-of select="$maxFunction"/>
  <xsl:text>;

// Function names
const char* JvmtiTrace::_function_names[] = {
</xsl:text>
  <xsl:call-template name="fillEntityName">
    <xsl:with-param name="entities" select="category/function"/>
  </xsl:call-template>
  <xsl:text>
};

// Exclude list
short JvmtiTrace::_exclude_functions[] = {
  </xsl:text>
  <xsl:apply-templates select="category/function" mode="notrace">
    <xsl:sort select="@num"/>
  </xsl:apply-templates>
  <xsl:text>0
};

</xsl:text>
  </xsl:if>

  <xsl:text>
extern "C" {

</xsl:text>
  <xsl:apply-templates select="category" mode="wrapper"/>
  <xsl:text>
} /* end extern "C" */

// JVMTI API functions
struct jvmtiInterface_1_ jvmti</xsl:text>
  <xsl:value-of select="$trace"/>
  <xsl:text>_Interface = {
</xsl:text>

  <xsl:call-template name="fillFuncStruct">
    <xsl:with-param name="funcs" select="category/function[count(@hide)=0]"/>
  </xsl:call-template>

  <xsl:text>
};
#endif // INCLUDE_JVMTI
</xsl:text>
</xsl:template>

<xsl:template match="function" mode="functionid">
  <xsl:text>jvmti</xsl:text>
  <xsl:value-of select="$trace"/>
  <xsl:text>_</xsl:text>
  <xsl:value-of select="@id"/>
</xsl:template>

<xsl:template name="fillFuncStructDoit">
  <xsl:param name="func"/>
  <xsl:param name="index"/>
  <xsl:text>                              /* </xsl:text>
  <xsl:number value="$index" format="  1"/>
  <xsl:text> : </xsl:text>
  <xsl:choose>
    <xsl:when test="count($func)=1">
      <xsl:value-of select="$func/synopsis"/>
      <xsl:text> */
      </xsl:text>
      <xsl:apply-templates select="$func" mode="functionid"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> RESERVED */
      NULL</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- generic function iterator applied to the function structure -->
<xsl:template name="fillFuncStruct">
  <xsl:param name="funcs"/>
  <xsl:param name="index" select="1"/>
  <xsl:call-template name="fillFuncStructDoit">
    <xsl:with-param name="func" select="$funcs[@num=$index]"/>
    <xsl:with-param name="index" select="$index"/>
  </xsl:call-template>
  <xsl:if test="count($funcs[@num &gt; $index]) &gt; 0">
    <xsl:text>,
</xsl:text>
    <xsl:call-template name="fillFuncStruct">
      <xsl:with-param name="funcs" select="$funcs"/>
      <xsl:with-param name="index" select="1+$index"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<xsl:template name="fillEntityNameDoit">
  <xsl:param name="entity"/>
  <xsl:param name="index"/>
  <xsl:choose>
    <xsl:when test="count($entity) &gt; 0">
      <xsl:text>  "</xsl:text>
      <xsl:value-of select="$entity[position()=1]/@id"/>
      <xsl:text>"</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  NULL</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- generic entity (with id and num) iterator applied to entity names -->
<xsl:template name="fillEntityName">
  <xsl:param name="entities"/>
  <xsl:param name="index" select="0"/>
  <xsl:call-template name="fillEntityNameDoit">
    <xsl:with-param name="entity" select="$entities[@num=$index]"/>
    <xsl:with-param name="index" select="$index"/>
  </xsl:call-template>
  <xsl:if test="count($entities[@num &gt; $index]) &gt; 0">
    <xsl:text>,
</xsl:text>
    <xsl:call-template name="fillEntityName">
      <xsl:with-param name="entities" select="$entities"/>
      <xsl:with-param name="index" select="1+$index"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<xsl:template name="fillEventThreadedDoit">
  <xsl:param name="entity"/>
  <xsl:param name="index"/>
  <xsl:choose>
    <xsl:when test="count($entity) &gt; 0">
      <xsl:choose>
        <xsl:when test="count($entity[position()=1]/@filtered)=0">
          <xsl:text>  false</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>  true</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>  false</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<xsl:template name="fillEventThreaded">
  <xsl:param name="entities"/>
  <xsl:param name="index" select="0"/>
  <xsl:call-template name="fillEventThreadedDoit">
    <xsl:with-param name="entity" select="$entities[@num=$index]"/>
    <xsl:with-param name="index" select="$index"/>
  </xsl:call-template>
  <xsl:if test="count($entities[@num &gt; $index]) &gt; 0">
    <xsl:text>,
</xsl:text>
    <xsl:call-template name="fillEventThreaded">
      <xsl:with-param name="entities" select="$entities"/>
      <xsl:with-param name="index" select="1+$index"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<xsl:template match="function" mode="notrace">
  <xsl:if test="count(@impl)=1 and contains(@impl,'notrace')">
    <xsl:value-of select="@num"/>
    <xsl:text>,
  </xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="category" mode="wrapper">
  <xsl:text>
  //
  // </xsl:text><xsl:value-of select="@label"/><xsl:text> functions
  //
</xsl:text>
  <xsl:apply-templates select="function[count(@hide)=0]"/>
</xsl:template>

<xsl:template match="function" mode="transition">
  <xsl:param name="space">
    <xsl:text>
  </xsl:text>
  </xsl:param>
  <xsl:value-of select="$space"/>

  <xsl:choose>
    <xsl:when test="count(@callbacksafe)=0 or not(contains(@callbacksafe,'safe'))">
      <xsl:text>if (this_thread == NULL || !this_thread->is_Java_thread()) {</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="count(@phase)=0 or contains(@phase,'live') or contains(@phase,'start')">
	  <xsl:text>if (this_thread == NULL || (!this_thread->is_Java_thread() &amp;&amp; !this_thread->is_Named_thread())) {</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>if (!this_thread->is_Java_thread()) {</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
     </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="$trace='Trace'">
    <xsl:value-of select="$space"/>
    <xsl:text>  if (trace_flags) {</xsl:text>
    <xsl:value-of select="$space"/>
    <xsl:text>    log_trace(jvmti)("[non-attached thread] %s %s",  func_name,</xsl:text>
    <xsl:value-of select="$space"/>
    <xsl:text>    JvmtiUtil::error_name(JVMTI_ERROR_UNATTACHED_THREAD));</xsl:text>
    <xsl:value-of select="$space"/>
    <xsl:text>  }</xsl:text>
  </xsl:if>
  <xsl:value-of select="$space"/>
  <xsl:text>  return JVMTI_ERROR_UNATTACHED_THREAD;</xsl:text>
  <xsl:value-of select="$space"/>
  <xsl:text>}</xsl:text>
  <xsl:value-of select="$space"/>
  <xsl:if test="count(@impl)=0 or not(contains(@impl,'innative'))">
    <xsl:text>JavaThread* current_thread = JavaThread::cast(this_thread);</xsl:text>
    <xsl:value-of select="$space"/>
    <xsl:text>MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, current_thread));</xsl:text>
    <xsl:value-of select="$space"/>
    <xsl:text>ThreadInVMfromNative __tiv(current_thread);</xsl:text>
    <xsl:value-of select="$space"/>
    <xsl:text>VM_ENTRY_BASE(jvmtiError, </xsl:text>
    <xsl:apply-templates select="." mode="functionid"/>
    <xsl:text> , current_thread)</xsl:text>
    <xsl:value-of select="$space"/>
    <xsl:text>debug_only(VMNativeEntryWrapper __vew;)</xsl:text>
    <xsl:if test="count(@callbacksafe)=0 or not(contains(@callbacksafe,'safe'))">
      <xsl:value-of select="$space"/>
      <xsl:text>PreserveExceptionMark __em(this_thread);</xsl:text>
    </xsl:if>
  </xsl:if>
</xsl:template>


<xsl:template match="required">
  <xsl:text>
  if (jvmti_env-&gt;get_capabilities()-&gt;</xsl:text>
    <xsl:value-of select="@id"/>
    <xsl:text> == 0) {
</xsl:text>
    <xsl:if test="$trace='Trace'">
      <xsl:text>    if (trace_flags) {
          log_trace(jvmti)("[%s] %s %s",  curr_thread_name, func_name,
                    JvmtiUtil::error_name(JVMTI_ERROR_MUST_POSSESS_CAPABILITY));
    }
</xsl:text>
    </xsl:if>
    <xsl:text>    return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
  }
</xsl:text>
</xsl:template>


<xsl:template match="function">
  <xsl:text>
static jvmtiError JNICALL
</xsl:text>
  <xsl:apply-templates select="." mode="functionid"/>
  <xsl:text>(jvmtiEnv* env</xsl:text>
  <xsl:apply-templates select="parameters" mode="signature"/>
  <xsl:text>) {
</xsl:text>

  <xsl:if test="not(contains(@jkernel,'yes'))">
  <xsl:text>&#xA;#if !INCLUDE_JVMTI &#xA;</xsl:text>
  <xsl:text>  return JVMTI_ERROR_NOT_AVAILABLE; &#xA;</xsl:text>
  <xsl:text>#else &#xA;</xsl:text>
  </xsl:if>

  <xsl:apply-templates select="." mode="traceSetUp"/>
  <xsl:choose>
    <xsl:when test="count(@phase)=0 or contains(@phase,'live')">
      <xsl:text>  if(!JvmtiEnv::is_vm_live()) {
</xsl:text>
    <xsl:if test="$trace='Trace'">
      <xsl:text>    if (trace_flags) {
          log_trace(jvmti)("[-] %s %s(%d)", func_name,
                    JvmtiUtil::error_name(JVMTI_ERROR_WRONG_PHASE), JvmtiEnv::get_phase());
    }
</xsl:text>
    </xsl:if>
    <xsl:text>    return JVMTI_ERROR_WRONG_PHASE;
  }</xsl:text>

      <xsl:text>
  Thread* this_thread = Thread::current_or_null(); </xsl:text>

      <xsl:apply-templates select="." mode="transition"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="contains(@phase,'onload')">
        <xsl:text>  if(JvmtiEnv::get_phase()!=JVMTI_PHASE_ONLOAD</xsl:text>
        <xsl:if test="not(contains(@phase,'onloadOnly'))">
          <xsl:text> &amp;&amp; JvmtiEnv::get_phase()!=JVMTI_PHASE_LIVE</xsl:text>
        </xsl:if>
        <xsl:text>) {
</xsl:text>
    <xsl:if test="$trace='Trace'">
      <xsl:text>    if (trace_flags) {
          log_trace(jvmti)("[-] %s %s",  func_name,
                    JvmtiUtil::error_name(JVMTI_ERROR_WRONG_PHASE));
    }
</xsl:text>
    </xsl:if>
    <xsl:text>    return JVMTI_ERROR_WRONG_PHASE;
  }</xsl:text>
      </xsl:if>
      <xsl:if test="contains(@phase,'start')">
        <xsl:text>  if(JvmtiEnv::get_phase(env)!=JVMTI_PHASE_START &amp;&amp; JvmtiEnv::get_phase()!=JVMTI_PHASE_LIVE) {
</xsl:text>
    <xsl:if test="$trace='Trace'">
      <xsl:text>    if (trace_flags) {
          log_trace(jvmti)("[-] %s %s",  func_name,
                    JvmtiUtil::error_name(JVMTI_ERROR_WRONG_PHASE));
    }
</xsl:text>
    </xsl:if>
    <xsl:text>    return JVMTI_ERROR_WRONG_PHASE;
  }
  Thread* this_thread = Thread::current_or_null(); </xsl:text>
      <xsl:apply-templates select="." mode="transition"/>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:text>
  JvmtiEnv* jvmti_env = JvmtiEnv::JvmtiEnv_from_jvmti_env(env);
  if (!jvmti_env->is_valid()) {
</xsl:text>
    <xsl:if test="$trace='Trace'">
      <xsl:text>    if (trace_flags) {
          log_trace(jvmti)("[%s] %s %s  env=" PTR_FORMAT,  curr_thread_name, func_name,
                    JvmtiUtil::error_name(JVMTI_ERROR_INVALID_ENVIRONMENT), p2i(env));
    }
</xsl:text>
    </xsl:if>
    <xsl:text>    return JVMTI_ERROR_INVALID_ENVIRONMENT;
  }
</xsl:text>

  <xsl:apply-templates select="capabilities/required"/>

  <xsl:text>  jvmtiError err;
</xsl:text>
  <xsl:choose>
    <xsl:when test="count(@phase)=1 and not(contains(@phase,'live')) and not(contains(@phase,'start'))">
      <xsl:choose>
        <xsl:when test="count(@callbacksafe)=0 or not(contains(@callbacksafe,'safe'))">
          <xsl:text>  if (Threads::number_of_threads() != 0) {
    Thread* this_thread = Thread::current_or_null();</xsl:text>
        </xsl:when>
        <xsl:otherwise>

	  <xsl:text>  Thread* this_thread = NULL;
  bool transition;
  if (Threads::number_of_threads() == 0) {
    transition = false;
  } else {
    this_thread = Thread::current_or_null();
    transition = ((this_thread != NULL) &amp;&amp; !this_thread->is_Named_thread());
  }
  if (transition) {</xsl:text>
	</xsl:otherwise>

      </xsl:choose>
      <!-- we allow use in early phases but there are threads now, -->
      <!-- so do thread transition -->
      <xsl:apply-templates select="." mode="transition">
          <xsl:with-param name="space">
            <xsl:text>
    </xsl:text>
          </xsl:with-param>
      </xsl:apply-templates>
      <xsl:text>
  </xsl:text>
      <xsl:apply-templates select="." mode="doCall"/>
      <xsl:text>  } else {
  </xsl:text>
      <!-- we are pre-thread - no thread transition code -->
      <xsl:apply-templates select="." mode="doCall"/>
      <xsl:text>  }
</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="." mode="doCall"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>  return err;
</xsl:text>

  <xsl:if test="not(contains(@jkernel,'yes'))">
  <xsl:text>#endif // INCLUDE_JVMTI&#xA;</xsl:text>
  </xsl:if>

  <xsl:text>}&#xA;</xsl:text>
</xsl:template>

<xsl:template match="function" mode="doCall">
  <xsl:apply-templates select="parameters" mode="dochecks"/>
  <xsl:apply-templates select="." mode="traceBefore"/>
  <xsl:apply-templates select="." mode="genCall"/>
  <xsl:apply-templates select="." mode="traceAfter"/>
</xsl:template>

<xsl:template match="function" mode="genCall">
  <xsl:text>  err = jvmti_env-&gt;</xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>(</xsl:text>
  <xsl:apply-templates select="parameters" mode="HotSpotValue"/>
  <xsl:text>);
</xsl:text>
</xsl:template>


<xsl:template match="function" mode="traceSetUp">
  <xsl:if test="$trace='Trace'">
    <xsl:text>  SafeResourceMark rm;
  jint trace_flags = JvmtiTrace::trace_flags(</xsl:text>
      <xsl:value-of select="@num"/>
      <xsl:text>);
  const char *func_name = NULL;
  const char *curr_thread_name = NULL;
  if (trace_flags) {
    func_name = JvmtiTrace::function_name(</xsl:text>
      <xsl:value-of select="@num"/>
      <xsl:text>);
    curr_thread_name = JvmtiTrace::safe_get_current_thread_name();
  }
</xsl:text>
  </xsl:if>
</xsl:template>


<xsl:template match="function" mode="traceBefore">
  <xsl:if test="$trace='Trace'">
    <xsl:text>
  if ((trace_flags &amp; JvmtiTrace::SHOW_IN) != 0) {
    </xsl:text>
    <xsl:apply-templates select="." mode="traceIn"/>
    <xsl:text>  }
</xsl:text>
  </xsl:if>
</xsl:template>


<xsl:template match="param" mode="traceError">
  <xsl:param name="err"/>
  <xsl:param name="comment"></xsl:param>
  <xsl:param name="extraValue"></xsl:param>
  <xsl:if test="$trace='Trace'">
  <xsl:text>      if ((trace_flags &amp; JvmtiTrace::SHOW_ERROR) != 0) {
        if ((trace_flags &amp; JvmtiTrace::SHOW_IN) == 0) {
</xsl:text>
  <xsl:apply-templates select="../.." mode="traceIn">
    <xsl:with-param name="endParam" select="."/>
  </xsl:apply-templates>
  <xsl:text>      }
        log_error(jvmti)("[%s] %s } %s - erroneous arg is </xsl:text>
    <xsl:value-of select="@id"/>
    <xsl:value-of select="$comment"/>
    <xsl:text>",  curr_thread_name, func_name,
                  JvmtiUtil::error_name(</xsl:text>
    <xsl:value-of select="$err"/>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="$extraValue"/>
    <xsl:text>);
      }
</xsl:text>
  </xsl:if>
    <xsl:text>      return </xsl:text>
    <xsl:value-of select="$err"/>
    <xsl:text>;</xsl:text>
</xsl:template>


<xsl:template match="function" mode="traceAfter">
  <xsl:if test="$trace='Trace'">
    <xsl:text>  if ( err != JVMTI_ERROR_NONE &amp;&amp; (trace_flags &amp; JvmtiTrace::SHOW_ERROR) != 0) {
      if ((trace_flags &amp; JvmtiTrace::SHOW_IN) == 0) {
</xsl:text>
    <xsl:apply-templates select="." mode="traceIn"/>
    <xsl:text>    }
    log_error(jvmti)("[%s] %s } %s",  curr_thread_name, func_name,
                  JvmtiUtil::error_name(err));
  } else if ((trace_flags &amp; JvmtiTrace::SHOW_OUT) != 0) {
    log_trace(jvmti)("[%s] %s }",  curr_thread_name, func_name);
  }
</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="function" mode="traceIn">
  <xsl:param name="endParam"></xsl:param>
  <xsl:text>          log_trace(jvmti)("[%s] %s { </xsl:text>
  <xsl:apply-templates select="parameters" mode="traceInFormat">
    <xsl:with-param name="endParam" select="$endParam"/>
  </xsl:apply-templates>
  <xsl:text>", curr_thread_name, func_name</xsl:text>
  <xsl:apply-templates select="parameters" mode="traceInValue">
    <xsl:with-param name="endParam" select="$endParam"/>
  </xsl:apply-templates>
  <xsl:text>);
</xsl:text>
</xsl:template>

<xsl:template match="parameters" mode="dochecks">
  <xsl:apply-templates select="param" mode="dochecks"/>
</xsl:template>

<xsl:template match="param" mode="dochecks">
  <xsl:apply-templates select="child::*[position()=1]" mode="dochecks">
    <xsl:with-param name="name" select="@id"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="outptr|outbuf|allocfieldbuf|ptrtype|inptr|inbuf|vmbuf|allocbuf|agentbuf|allocallocbuf" mode="dochecks">
  <xsl:param name="name"/>
  <xsl:if test="count(nullok)=0">
    <xsl:text>  if (</xsl:text>
    <xsl:value-of select="$name"/>
    <xsl:text> == NULL) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_NULL_POINTER</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="jrawMonitorID" mode="dochecks">
  <xsl:param name="name"/>
  <xsl:text>  JvmtiRawMonitor *rmonitor = (JvmtiRawMonitor *)</xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>;
  if (rmonitor == NULL) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_INVALID_MONITOR</xsl:with-param>
      <xsl:with-param name="comment"> - raw monitor is NULL</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
  if (!rmonitor->is_valid()) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_INVALID_MONITOR</xsl:with-param>
      <xsl:with-param name="comment"> - not a raw monitor " PTR_FORMAT "</xsl:with-param>
      <xsl:with-param name="extraValue">, p2i(rmonitor)</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
</xsl:text>
</xsl:template>

<xsl:template match="jthread" mode="dochecksbody">
  <xsl:param name="name"/>
    <xsl:text>    err = JvmtiExport::cv_external_thread_to_JavaThread(tlh.list(), </xsl:text>
    <xsl:value-of select="$name"/>
    <xsl:text>, &amp;java_thread, NULL);
    if (err != JVMTI_ERROR_NONE) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">err</xsl:with-param>
      <xsl:with-param name="comment"> - jthread did not convert to a JavaThread - jthread = " PTR_FORMAT "</xsl:with-param>
      <xsl:with-param name="extraValue">, p2i(<xsl:value-of select="$name"/>)</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
    }
</xsl:text>
</xsl:template>

<xsl:template match="jthread" mode="dochecks">
  <xsl:param name="name"/>
  <!-- If we convert and test threads -->
  <xsl:if test="count(@impl)=0 or not(contains(@impl,'noconvert'))">
    <xsl:text>  JavaThread* java_thread = NULL;
  ThreadsListHandle tlh(this_thread);
</xsl:text>
    <xsl:choose>
      <xsl:when test="count(@null)=0">
        <xsl:apply-templates select="." mode="dochecksbody">
          <xsl:with-param name="name" select="$name"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>  if (</xsl:text>
        <xsl:value-of select="$name"/>
        <xsl:text> == NULL) {
    java_thread = current_thread;
  } else {
</xsl:text>
        <xsl:apply-templates select="." mode="dochecksbody">
          <xsl:with-param name="name" select="$name"/>
        </xsl:apply-templates>
        <xsl:text>  }
</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<xsl:template match="jframeID" mode="dochecks">
  <xsl:param name="name"/>
  <xsl:text>
  if (depth &lt; 0) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_ILLEGAL_ARGUMENT</xsl:with-param>
      <xsl:with-param name="comment"> - negative depth - jthread = " INT32_FORMAT "</xsl:with-param>
      <xsl:with-param name="extraValue">, <xsl:value-of select="$name"/></xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
</xsl:text>
</xsl:template>

<xsl:template match="jclass" mode="dochecks">
 <xsl:param name="name"/>
 <!-- for JVMTI a jclass/jmethodID becomes just jmethodID -->
 <xsl:if test="count(@method)=0">
  <xsl:text>  oop k_mirror = JNIHandles::resolve_external_guard(</xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>);
  if (k_mirror == NULL) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_INVALID_CLASS</xsl:with-param>
      <xsl:with-param name="comment"> - resolved to NULL - jclass = " PTR_FORMAT "</xsl:with-param>
      <xsl:with-param name="extraValue">, p2i(<xsl:value-of select="$name"/>)</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
  if (!k_mirror->is_a(vmClasses::Class_klass())) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_INVALID_CLASS</xsl:with-param>
      <xsl:with-param name="comment"> - not a class - jclass = " PTR_FORMAT "</xsl:with-param>
      <xsl:with-param name="extraValue">, p2i(<xsl:value-of select="$name"/>)</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
</xsl:text>
  <xsl:if test="count(@method|@field)=1">
    <xsl:text>
  if (java_lang_Class::is_primitive(k_mirror)) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_INVALID_CLASS</xsl:with-param>
      <xsl:with-param name="comment"> - is a primitive class - jclass = " PTR_FORMAT "</xsl:with-param>
      <xsl:with-param name="extraValue">, p2i(<xsl:value-of select="$name"/>)</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
  Klass* k_oop = java_lang_Class::as_Klass(k_mirror);
  if (k_oop == NULL) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_INVALID_CLASS</xsl:with-param>
      <xsl:with-param name="comment"> - no Klass* - jclass = " PTR_FORMAT "</xsl:with-param>
      <xsl:with-param name="extraValue">, p2i(<xsl:value-of select="$name"/>)</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
</xsl:text>
  </xsl:if>
 </xsl:if>
</xsl:template>


<xsl:template match="jmethodID" mode="dochecks">
  <xsl:param name="name"/>
  <xsl:text>  Method* checked_method = Method::checked_resolve_jmethod_id(</xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>);&#xA;</xsl:text>
  <xsl:text>  if (checked_method == NULL) {&#xA;</xsl:text>
  <xsl:apply-templates select=".." mode="traceError">
    <xsl:with-param name="err">JVMTI_ERROR_INVALID_METHODID</xsl:with-param>
    <xsl:with-param name="comment"></xsl:with-param>
    <xsl:with-param name="extraValue"></xsl:with-param>
  </xsl:apply-templates>
  <xsl:text>&#xA;</xsl:text>
  <xsl:text>  }&#xA;</xsl:text>
  <xsl:if test="count(@native)=1 and contains(@native,'error')">
    <xsl:text>  if (checked_method->is_native()) {&#xA;</xsl:text>
    <xsl:text>    return JVMTI_ERROR_NATIVE_METHOD;&#xA;</xsl:text>
    <xsl:text>  }&#xA;</xsl:text>
  </xsl:if>
</xsl:template>


<xsl:template match="jfieldID" mode="dochecks">
  <xsl:param name="name"/>
  <xsl:text>  ResourceMark rm_fdesc(current_thread);&#xA;</xsl:text>
  <xsl:text>  fieldDescriptor fdesc;&#xA;</xsl:text>
  <xsl:text>  if (!JvmtiEnv::get_field_descriptor(k_oop, </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>, &amp;fdesc)) {&#xA;</xsl:text>
  <xsl:apply-templates select=".." mode="traceError">
    <xsl:with-param name="err">JVMTI_ERROR_INVALID_FIELDID</xsl:with-param>
  </xsl:apply-templates>
  <xsl:text>&#xA;</xsl:text>
  <xsl:text>  }&#xA;</xsl:text>
</xsl:template>


<xsl:template match="jint" mode="dochecks">
  <xsl:param name="name"/>
  <xsl:if test="count(@min)=1">
    <xsl:text>  if (</xsl:text>
    <xsl:value-of select="$name"/>
    <xsl:text> &lt; </xsl:text>
    <xsl:value-of select="@min"/>
    <xsl:text>) {
</xsl:text>
    <xsl:apply-templates select=".." mode="traceError">
      <xsl:with-param name="err">JVMTI_ERROR_ILLEGAL_ARGUMENT</xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>
  }
</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="jobject|jvalue|jthreadGroup|enum|jchar|jlong|jfloat|jdouble|jlocation|jboolean|char|uchar|size_t|void|struct" mode="dochecks">
</xsl:template>

<!-- iterate over parameters, stopping if specified is encountered -->
<xsl:template name="traceInValueParamsUpTo">
  <xsl:param name="params"/>
  <xsl:param name="endParam"></xsl:param>
  <xsl:param name="index" select="1"/>
  <xsl:variable name="cParam" select="$params[position()=$index]"/>
  <xsl:if test="$cParam!=$endParam">
    <xsl:apply-templates select="$cParam" mode="traceInValue"/>
    <xsl:if test="count($params) &gt; $index">
      <xsl:call-template name="traceInValueParamsUpTo">
        <xsl:with-param name="params" select="$params"/>
        <xsl:with-param name="endParam" select="$endParam"/>
        <xsl:with-param name="index" select="1+$index"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template name="traceInFormatParamsUpTo">
  <xsl:param name="params"/>
  <xsl:param name="endParam"></xsl:param>
  <xsl:param name="index" select="1"/>
  <xsl:variable name="cParam" select="$params[position()=$index]"/>
  <xsl:if test="$cParam!=$endParam">
    <xsl:apply-templates select="$cParam" mode="traceInFormat"/>
    <xsl:if test="count($params) &gt; $index">
      <xsl:call-template name="traceInFormatParamsUpTo">
        <xsl:with-param name="params" select="$params"/>
        <xsl:with-param name="endParam" select="$endParam"/>
        <xsl:with-param name="index" select="1+$index"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="parameters" mode="traceInFormat">
  <xsl:param name="endParam"></xsl:param>
  <xsl:call-template name="traceInFormatParamsUpTo">
    <xsl:with-param name="params" select="param"/>
    <xsl:with-param name="endParam" select="$endParam"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="parameters" mode="traceInValue">
  <xsl:param name="endParam"></xsl:param>
  <xsl:call-template name="traceInValueParamsUpTo">
    <xsl:with-param name="params" select="param"/>
    <xsl:with-param name="endParam" select="$endParam"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="param" mode="traceInFormat">
  <xsl:apply-templates select="child::*[position()=1]" mode="traceInFormat">
    <xsl:with-param name="name" select="@id"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="param" mode="traceInValue">
  <xsl:apply-templates select="child::*[position()=1]" mode="traceInValue">
    <xsl:with-param name="name" select="@id"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="outptr|outbuf|allocfieldbuf|vmbuf|allocbuf|agentbuf|allocallocbuf" mode="traceInFormat">
</xsl:template>

<xsl:template match="outptr|outbuf|allocfieldbuf|vmbuf|allocbuf|agentbuf|allocallocbuf" mode="traceInValue">
</xsl:template>

<xsl:template match="inbuf" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:variable name="child" select="child::*[position()=1]"/>
  <xsl:choose>g
    <xsl:when test="name($child)='char'">
      <xsl:text>='%s'</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>=" PTR_FORMAT "</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="inbuf" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>, </xsl:text>
  <xsl:variable name="child" select="child::*[position()=1]"/>
  <xsl:choose>
    <xsl:when test="name($child)='char'">
      <xsl:value-of select="$name"/>
    </xsl:when>
    <xsl:otherwise>
      p2i(<xsl:value-of select="$name"/>)
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="ptrtype" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:variable name="child" select="child::*[position()=1]"/>
  <xsl:choose>
    <xsl:when test="name($child)='jclass'">
      <xsl:text> </xsl:text>
      <xsl:value-of select="$name"/>
      <xsl:text>=" PTR_FORMAT "</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="$child" mode="traceInFormat"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="ptrtype" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:variable name="child" select="child::*[position()=1]"/>
  <xsl:choose>
    <xsl:when test="name($child)='jclass'">
      <xsl:text>, </xsl:text>
      p2i(<xsl:value-of select="$name"/>)
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="$child" mode="traceInValue"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="inptr" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=" PTR_FORMAT "</xsl:text>
</xsl:template>

<xsl:template match="inptr" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>, </xsl:text>
  p2i(<xsl:value-of select="$name"/>)
</xsl:template>

<xsl:template match="jrawMonitorID|jfieldID" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=%s</xsl:text>
</xsl:template>

<xsl:template match="jclass" mode="traceInFormat">
  <xsl:param name="name"/>
  <!-- for JVMTI a jclass/jmethodID becomes just jmethodID -->
  <xsl:if test="count(@method)=0">
    <xsl:text> </xsl:text>
    <xsl:value-of select="$name"/>
    <xsl:text>=%s</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="jrawMonitorID" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>, rmonitor->get_name()</xsl:text>
</xsl:template>

<xsl:template match="jthread" mode="traceInFormat">
  <xsl:param name="name"/>
  <!-- If we convert and test threads -->
  <xsl:if test="count(@impl)=0 or not(contains(@impl,'noconvert'))">
    <xsl:text> </xsl:text>
    <xsl:value-of select="$name"/>
    <xsl:text>=%s</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="jthread" mode="traceInValue">
  <xsl:param name="name"/>
  <!-- If we convert and test threads -->
  <xsl:if test="count(@impl)=0 or not(contains(@impl,'noconvert'))">
    <xsl:text>,
                    JvmtiTrace::safe_get_thread_name(java_thread)</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="jframeID" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text>depth=%d</xsl:text>
</xsl:template>

<xsl:template match="jframeID" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>, </xsl:text>
  <xsl:value-of select="$name"/>
</xsl:template>

<xsl:template match="jclass" mode="traceInValue">
  <!-- for JVMTI a jclass/jmethodID becomes just jmethodID -->
  <xsl:if test="count(@method)=0">
    <xsl:text>,
                    JvmtiTrace::get_class_name(k_mirror)</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="jmethodID" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=%s.%s</xsl:text>
</xsl:template>

<xsl:template match="jmethodID" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>,
                    checked_method == NULL? "NULL" : checked_method->klass_name()->as_C_string(),
                    checked_method == NULL? "NULL" : checked_method->name()->as_C_string()
             </xsl:text>
</xsl:template>

<xsl:template match="jfieldID" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>, fdesc.name()->as_C_string()</xsl:text>
</xsl:template>

<xsl:template match="enum" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=%d:%s</xsl:text>
</xsl:template>

<xsl:template match="enum" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>, </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>,
                    </xsl:text>
  <xsl:choose>
    <xsl:when test=".='jvmtiError'">
      <xsl:text>JvmtiUtil::error_name(</xsl:text>
      <xsl:value-of select="$name"/>
      <xsl:text>)
</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test=".='jvmtiEvent'">
          <xsl:text>JvmtiTrace::event_name(</xsl:text>
          <xsl:value-of select="$name"/>
          <xsl:text>)
        </xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>JvmtiTrace::enum_name(</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>ConstantNames, </xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>ConstantValues, </xsl:text>
        <xsl:value-of select="$name"/>
        <xsl:text>)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="jint" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=" INT32_FORMAT "</xsl:text>
</xsl:template>

<xsl:template match="jlocation" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=" JLONG_FORMAT "</xsl:text>
</xsl:template>

<xsl:template match="jlong" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=" JLONG_FORMAT "</xsl:text>
</xsl:template>

<xsl:template match="size_t" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=" SIZE_FORMAT_HEX "</xsl:text>
</xsl:template>

<xsl:template match="jfloat|jdouble" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=%f</xsl:text>
</xsl:template>

<xsl:template match="char" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=%c</xsl:text>
</xsl:template>

<xsl:template match="uchar|jchar" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=0x%x</xsl:text>
</xsl:template>

<xsl:template match="jint|jlocation|jchar|jlong|jfloat|jdouble|char|uchar|size_t" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>, </xsl:text>
  <xsl:value-of select="$name"/>
</xsl:template>


<xsl:template match="jboolean" mode="traceInFormat">
  <xsl:param name="name"/>
  <xsl:text> </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>=%s</xsl:text>
</xsl:template>

<xsl:template match="jboolean" mode="traceInValue">
  <xsl:param name="name"/>
  <xsl:text>, </xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>? "true" : "false"</xsl:text>
</xsl:template>

<xsl:template match="jobject|jvalue|jthreadGroup|void|struct" mode="traceInFormat">
</xsl:template>

<xsl:template match="jobject|jvalue|jthreadGroup|void|struct" mode="traceInValue">
</xsl:template>



</xsl:stylesheet>
