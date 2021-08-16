/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.serializer;

/**
 * Administrative class to keep track of the version number of
 * the Serializer release.
 * <P>This class implements the upcoming standard of having
 * org.apache.project-name.Version.getVersion() be a standard way
 * to get version information.</P>
 * @xsl.usage general
 */
public final class Version
{

  /**
   * Get the basic version string for the current Serializer.
   * Version String formatted like
   * <CODE>"<B>Serializer</B> <B>Java</B> v.r[.dd| <B>D</B>nn]"</CODE>.
   *
   * Futurework: have this read version info from jar manifest.
   *
   * @return String denoting our current version
   */
  public static String getVersion()
  {
     return getProduct()+" "+getImplementationLanguage()+" "
           +getMajorVersionNum()+"."+getReleaseVersionNum()+"."
           +( (getDevelopmentVersionNum() > 0) ?
               ("D"+getDevelopmentVersionNum()) : (""+getMaintenanceVersionNum()));
  }

  /**
   * Print the processor version to the command line.
   *
   * @param argv command line arguments, unused.
   */
  public static void _main(String argv[])
  {
    System.out.println(getVersion());
  }

  /**
   * Name of product: Serializer.
   */
  public static String getProduct()
  {
    return "Serializer";
  }

  /**
   * Implementation Language: Java.
   */
  public static String getImplementationLanguage()
  {
    return "Java";
  }


  /**
   * Major version number.
   * Version number. This changes only when there is a
   *          significant, externally apparent enhancement from
   *          the previous release. 'n' represents the n'th
   *          version.
   *
   *          Clients should carefully consider the implications
   *          of new versions as external interfaces and behaviour
   *          may have changed.
   */
  public static int getMajorVersionNum()
  {
    return 2;

  }

  /**
   * Release Number.
   * Release number. This changes when:
   *            -  a new set of functionality is to be added, eg,
   *               implementation of a new W3C specification.
   *            -  API or behaviour change.
   *            -  its designated as a reference release.
   */
  public static int getReleaseVersionNum()
  {
    return 7;
  }

  /**
   * Maintenance Drop Number.
   * Optional identifier used to designate maintenance
   *          drop applied to a specific release and contains
   *          fixes for defects reported. It maintains compatibility
   *          with the release and contains no API changes.
   *          When missing, it designates the final and complete
   *          development drop for a release.
   */
  public static int getMaintenanceVersionNum()
  {
    return 0;
  }

  /**
   * Development Drop Number.
   * Optional identifier designates development drop of
   *          a specific release. D01 is the first development drop
   *          of a new release.
   *
   *          Development drops are works in progress towards a
   *          compeleted, final release. A specific development drop
   *          may not completely implement all aspects of a new
   *          feature, which may take several development drops to
   *          complete. At the point of the final drop for the
   *          release, the D suffix will be omitted.
   *
   *          Each 'D' drops can contain functional enhancements as
   *          well as defect fixes. 'D' drops may not be as stable as
   *          the final releases.
   */
  public static int getDevelopmentVersionNum()
  {
    try {
        if ((new String("")).length() == 0)
          return 0;
        else
          return Integer.parseInt("");
    } catch (NumberFormatException nfe) {
           return 0;
    }
  }
}
