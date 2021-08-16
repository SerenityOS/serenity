@echo off
REM
REM Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
REM DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
REM
REM This code is free software; you can redistribute it and/or modify it
REM under the terms of the GNU General Public License version 2 only, as
REM published by the Free Software Foundation.
REM
REM This code is distributed in the hope that it will be useful, but WITHOUT
REM ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
REM FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
REM version 2 for more details (a copy is included in the LICENSE file that
REM accompanied this code).
REM
REM You should have received a copy of the GNU General Public License version
REM 2 along with this work; if not, write to the Free Software Foundation,
REM Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
REM
REM Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
REM or visit www.oracle.com if you need additional information or have any
REM questions.
REM

set vcvarscmd=%1
set output=%2
if not "%3" == "auto" set version=-vcvars_ver=%3

set PATH_BEFORE=%PATH%

call %vcvarscmd% %version% %4 %5 %6 %7 %8 %9
if exist %output% del %output%

call :extract "%PATH_BEFORE%", PATH_BEFORE
call :extract "%PATH%", PATH_AFTER
call :extract "%INCLUDE%", VS_INCLUDE
call :extract "%LIB%", VS_LIB
call :extract "%VCINSTALLDIR%", VCINSTALLDIR
call :extract "%VCToolsRedistDir%", VCToolsRedistDir
call :extract "%WindowsSdkDir%", WindowsSdkDir
call :extract "%WINDOWSSDKDIR%", WINDOWSSDKDIR

exit /b 0

:extract
echo %~2=$($BASH $TOPDIR/make/scripts/fixpath.sh -i import '%~1 ') >> %output%
exit /b 0
