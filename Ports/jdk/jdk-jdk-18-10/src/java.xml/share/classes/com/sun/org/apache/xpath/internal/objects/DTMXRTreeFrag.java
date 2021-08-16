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

package com.sun.org.apache.xpath.internal.objects;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xpath.internal.XPathContext;
/*
 *
 * @author igorh
 *
 * Simple wrapper to DTM and XPathContext objects.
 * Used in XRTreeFrag for caching references to the objects.
 */
 public final class DTMXRTreeFrag {
  private DTM m_dtm;
  private int m_dtmIdentity = DTM.NULL;
  private XPathContext m_xctxt;

  public DTMXRTreeFrag(int dtmIdentity, XPathContext xctxt){
      m_xctxt = xctxt;
      m_dtmIdentity = dtmIdentity;
      m_dtm = xctxt.getDTM(dtmIdentity);
    }

  public final void destruct(){
    m_dtm = null;
    m_xctxt = null;
 }

final  DTM getDTM(){return m_dtm;}
public final  int getDTMIdentity(){return m_dtmIdentity;}
final  XPathContext getXPathContext(){return m_xctxt;}

public final int hashCode() { return m_dtmIdentity; }
public final boolean equals(Object obj) {
   if (obj instanceof DTMXRTreeFrag) {
       return (m_dtmIdentity == ((DTMXRTreeFrag)obj).getDTMIdentity());
   }
   return false;
 }

}
