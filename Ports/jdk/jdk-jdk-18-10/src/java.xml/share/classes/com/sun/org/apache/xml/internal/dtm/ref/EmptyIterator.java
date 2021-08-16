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


package com.sun.org.apache.xml.internal.dtm.ref;

import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.DTM;


/**
 * DTM Empty Axis Iterator. The class is immutable
 */
public final class EmptyIterator implements DTMAxisIterator
{
  private static final EmptyIterator INSTANCE =  new EmptyIterator();

  public static DTMAxisIterator  getInstance() {return INSTANCE;}

  private EmptyIterator(){}

  public final  int  next(){ return END; }

  public final DTMAxisIterator reset(){ return this; }

  public final int getLast(){ return 0; }

  public final int getPosition(){ return 1; }

  public final void setMark(){}

  public final void gotoMark(){}

  public final DTMAxisIterator setStartNode(int node){ return this; }

  public final int getStartNode(){ return END; }

  public final boolean isReverse(){return false;}

  public final DTMAxisIterator cloneIterator(){ return this; }

  public final void setRestartable(boolean isRestartable) {}

  public final int getNodeByPosition(int position){ return END; }
}
