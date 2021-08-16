<?xml version='1.0' encoding='UTF-8'?>
<!-- XML Schema schema for XML Schemas: Part 1: Structures -->
<!-- Note this schema is NOT the normative structures schema. -->
<!-- The prose copy in the structures REC is the normative -->
<!-- version (which shouldn't differ from this one except for -->
<!-- this comment and entity expansions, but just in case -->
<!DOCTYPE xs:schema PUBLIC "-//W3C//DTD XMLSCHEMA 200102//EN" "XMLSchema.dtd" [

<!-- provide ID type information even for parsers which only read the
     internal subset -->
<!ATTLIST xs:schema          id  ID  #IMPLIED>
<!ATTLIST xs:complexType     id  ID  #IMPLIED>
<!ATTLIST xs:complexContent  id  ID  #IMPLIED>
<!ATTLIST xs:simpleContent   id  ID  #IMPLIED>
<!ATTLIST xs:extension       id  ID  #IMPLIED>
<!ATTLIST xs:element         id  ID  #IMPLIED>
<!ATTLIST xs:group           id  ID  #IMPLIED> 
<!ATTLIST xs:all             id  ID  #IMPLIED>
<!ATTLIST xs:choice          id  ID  #IMPLIED>
<!ATTLIST xs:sequence        id  ID  #IMPLIED>
<!ATTLIST xs:any             id  ID  #IMPLIED>
<!ATTLIST xs:anyAttribute    id  ID  #IMPLIED>
<!ATTLIST xs:attribute       id  ID  #IMPLIED>
<!ATTLIST xs:attributeGroup  id  ID  #IMPLIED>
<!ATTLIST xs:unique          id  ID  #IMPLIED>
<!ATTLIST xs:key             id  ID  #IMPLIED>
<!ATTLIST xs:keyref          id  ID  #IMPLIED>
<!ATTLIST xs:selector        id  ID  #IMPLIED>
<!ATTLIST xs:field           id  ID  #IMPLIED>
<!ATTLIST xs:include         id  ID  #IMPLIED>
<!ATTLIST xs:import          id  ID  #IMPLIED>
<!ATTLIST xs:redefine        id  ID  #IMPLIED>
<!ATTLIST xs:notation        id  ID  #IMPLIED>
<!--
     keep this schema XML1.0 DTD valid
  -->
        <!ENTITY % schemaAttrs 'xmlns:hfp CDATA #IMPLIED'>

        <!ELEMENT hfp:hasFacet EMPTY>
        <!ATTLIST hfp:hasFacet
                name NMTOKEN #REQUIRED>

        <!ELEMENT hfp:hasProperty EMPTY>
        <!ATTLIST hfp:hasProperty
                name NMTOKEN #REQUIRED
                value CDATA #REQUIRED>
<!--
        Make sure that processors that do not read the external
        subset will know about the various IDs we declare
  -->
        <!ATTLIST xs:simpleType id ID #IMPLIED>
        <!ATTLIST xs:maxExclusive id ID #IMPLIED>
        <!ATTLIST xs:minExclusive id ID #IMPLIED>
        <!ATTLIST xs:maxInclusive id ID #IMPLIED>
        <!ATTLIST xs:minInclusive id ID #IMPLIED>
        <!ATTLIST xs:totalDigits id ID #IMPLIED>
        <!ATTLIST xs:fractionDigits id ID #IMPLIED>
        <!ATTLIST xs:length id ID #IMPLIED>
        <!ATTLIST xs:minLength id ID #IMPLIED>
        <!ATTLIST xs:maxLength id ID #IMPLIED>
        <!ATTLIST xs:enumeration id ID #IMPLIED>
        <!ATTLIST xs:pattern id ID #IMPLIED>
        <!ATTLIST xs:appinfo id ID #IMPLIED>
        <!ATTLIST xs:documentation id ID #IMPLIED>
        <!ATTLIST xs:list id ID #IMPLIED>
        <!ATTLIST xs:union id ID #IMPLIED>
        ]>
<xs:schema targetNamespace="http://www.w3.org/2001/XMLSchema" blockDefault="#all" elementFormDefault="qualified" version="1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xml:lang="EN" xmlns:hfp="http://www.w3.org/2001/XMLSchema-hasFacetAndProperty">
 <xs:annotation>
  <xs:documentation>
    Part 1 version: Id: structures.xsd,v 1.2 2004/01/15 11:34:25 ht Exp 
    Part 2 version: Id: datatypes.xsd,v 1.3 2004/01/23 18:11:13 ht Exp 
  </xs:documentation>
 </xs:annotation>

 <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/2004/PER-xmlschema-1-20040318/structures.html">
   The schema corresponding to this document is normative,
   with respect to the syntactic constraints it expresses in the
   XML Schema language.  The documentation (within &lt;documentation> elements)
   below, is not normative, but rather highlights important aspects of
   the W3C Recommendation of which this is a part</xs:documentation>
 </xs:annotation>

 <xs:annotation>
   <xs:documentation>
   The simpleType element and all of its members are defined
      towards the end of this schema document</xs:documentation>
 </xs:annotation>

 <xs:import namespace="http://www.w3.org/XML/1998/namespace" schemaLocation="http://www.w3.org/2001/xml.xsd">
   <xs:annotation>
     <xs:documentation>
       Get access to the xml: attribute groups for xml:lang
       as declared on 'schema' and 'documentation' below
     </xs:documentation>
   </xs:annotation>
 </xs:import>

 <xs:complexType name="openAttrs">
   <xs:annotation>
     <xs:documentation>
       This type is extended by almost all schema types
       to allow attributes from other namespaces to be
       added to user schemas.
     </xs:documentation>
   </xs:annotation>
   <xs:complexContent>
     <xs:restriction base="xs:anyType">
       <xs:anyAttribute namespace="##other" processContents="lax"/>
     </xs:restriction>
   </xs:complexContent>
 </xs:complexType>

 <xs:complexType name="annotated">
   <xs:annotation>
     <xs:documentation>
       This type is extended by all types which allow annotation
       other than &lt;schema&gt; itself
     </xs:documentation>
   </xs:annotation>
   <xs:complexContent>
     <xs:extension base="xs:openAttrs">
       <xs:sequence>
         <xs:element ref="xs:annotation" minOccurs="0"/>
       </xs:sequence>
       <xs:attribute name="id" type="xs:ID"/>
     </xs:extension>
   </xs:complexContent>
 </xs:complexType>

 <xs:group name="schemaTop">
  <xs:annotation>
   <xs:documentation>
   This group is for the
   elements which occur freely at the top level of schemas.
   All of their types are based on the "annotated" type by extension.</xs:documentation>
  </xs:annotation>
  <xs:choice>
   <xs:group ref="xs:redefinable"/>
   <xs:element ref="xs:element"/>
   <xs:element ref="xs:attribute"/>
   <xs:element ref="xs:notation"/>
  </xs:choice>
 </xs:group>
 
 <xs:group name="redefinable">
  <xs:annotation>
   <xs:documentation>
   This group is for the
   elements which can self-redefine (see &lt;redefine> below).</xs:documentation>
  </xs:annotation>
  <xs:choice>
   <xs:element ref="xs:simpleType"/>
   <xs:element ref="xs:complexType"/>
   <xs:element ref="xs:group"/>
   <xs:element ref="xs:attributeGroup"/>
  </xs:choice>
 </xs:group>

 <xs:simpleType name="formChoice">
  <xs:annotation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
  </xs:annotation>
  <xs:restriction base="xs:NMTOKEN">
   <xs:enumeration value="qualified"/>
   <xs:enumeration value="unqualified"/>
  </xs:restriction>
 </xs:simpleType>

 <xs:simpleType name="reducedDerivationControl">
  <xs:annotation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
  </xs:annotation>
  <xs:restriction base="xs:derivationControl">
   <xs:enumeration value="extension"/>
   <xs:enumeration value="restriction"/>
  </xs:restriction>
 </xs:simpleType>

 <xs:simpleType name="derivationSet">
  <xs:annotation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
   <xs:documentation>
   #all or (possibly empty) subset of {extension, restriction}</xs:documentation>
  </xs:annotation>
  <xs:union>
   <xs:simpleType>    
    <xs:restriction base="xs:token">
     <xs:enumeration value="#all"/>
    </xs:restriction>
   </xs:simpleType>
   <xs:simpleType>
    <xs:list itemType="xs:reducedDerivationControl"/>
   </xs:simpleType>
  </xs:union>
 </xs:simpleType>

 <xs:simpleType name="typeDerivationControl">
  <xs:annotation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
  </xs:annotation>
  <xs:restriction base="xs:derivationControl">
   <xs:enumeration value="extension"/>
   <xs:enumeration value="restriction"/>
   <xs:enumeration value="list"/>
   <xs:enumeration value="union"/>
  </xs:restriction>
 </xs:simpleType>

  <xs:simpleType name="fullDerivationSet">
  <xs:annotation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
   <xs:documentation>
   #all or (possibly empty) subset of {extension, restriction, list, union}</xs:documentation>
  </xs:annotation>
  <xs:union>
   <xs:simpleType>    
    <xs:restriction base="xs:token">
     <xs:enumeration value="#all"/>
    </xs:restriction>
   </xs:simpleType>
   <xs:simpleType>
    <xs:list itemType="xs:typeDerivationControl"/>
   </xs:simpleType>
  </xs:union>
 </xs:simpleType>

 <xs:element name="schema" id="schema">
  <xs:annotation>
    <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-schema"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:openAttrs">
     <xs:sequence>
      <xs:choice minOccurs="0" maxOccurs="unbounded">
       <xs:element ref="xs:include"/>
       <xs:element ref="xs:import"/>
       <xs:element ref="xs:redefine"/>
       <xs:element ref="xs:annotation"/>
      </xs:choice>
      <xs:sequence minOccurs="0" maxOccurs="unbounded">
       <xs:group ref="xs:schemaTop"/>
       <xs:element ref="xs:annotation" minOccurs="0" maxOccurs="unbounded"/>
      </xs:sequence>
     </xs:sequence>
     <xs:attribute name="targetNamespace" type="xs:anyURI"/>
     <xs:attribute name="version" type="xs:token"/>
     <xs:attribute name="finalDefault" type="xs:fullDerivationSet" use="optional" default=""/>
     <xs:attribute name="blockDefault" type="xs:blockSet" use="optional" default=""/>
     <xs:attribute name="attributeFormDefault" type="xs:formChoice" use="optional" default="unqualified"/>
     <xs:attribute name="elementFormDefault" type="xs:formChoice" use="optional" default="unqualified"/>
     <xs:attribute name="id" type="xs:ID"/>
     <xs:attribute ref="xml:lang"/>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>

  <xs:key name="element">
   <xs:selector xpath="xs:element"/>
   <xs:field xpath="@name"/>
  </xs:key>

  <xs:key name="attribute">
   <xs:selector xpath="xs:attribute"/>
   <xs:field xpath="@name"/>
  </xs:key>

  <xs:key name="type">
   <xs:selector xpath="xs:complexType|xs:simpleType"/>
   <xs:field xpath="@name"/>
  </xs:key>
 
  <xs:key name="group">
   <xs:selector xpath="xs:group"/>
   <xs:field xpath="@name"/>
  </xs:key>
 
  <xs:key name="attributeGroup">
   <xs:selector xpath="xs:attributeGroup"/>
   <xs:field xpath="@name"/>
  </xs:key>
 
  <xs:key name="notation">
   <xs:selector xpath="xs:notation"/>
   <xs:field xpath="@name"/>
  </xs:key>

  <xs:key name="identityConstraint">
   <xs:selector xpath=".//xs:key|.//xs:unique|.//xs:keyref"/>
   <xs:field xpath="@name"/>
  </xs:key>

 </xs:element>

 <xs:simpleType name="allNNI">
  <xs:annotation><xs:documentation>
   for maxOccurs</xs:documentation></xs:annotation>
  <xs:union memberTypes="xs:nonNegativeInteger">
   <xs:simpleType>
    <xs:restriction base="xs:NMTOKEN">
     <xs:enumeration value="unbounded"/>
    </xs:restriction>
   </xs:simpleType>
  </xs:union>
 </xs:simpleType>

 <xs:attributeGroup name="occurs">
  <xs:annotation><xs:documentation>
   for all particles</xs:documentation></xs:annotation>
  <xs:attribute name="minOccurs" type="xs:nonNegativeInteger" use="optional" default="1"/>
  <xs:attribute name="maxOccurs" type="xs:allNNI" use="optional" default="1"/>
 </xs:attributeGroup>

 <xs:attributeGroup name="defRef">
  <xs:annotation><xs:documentation>
   for element, group and attributeGroup,
   which both define and reference</xs:documentation></xs:annotation>
  <xs:attribute name="name" type="xs:NCName"/>
  <xs:attribute name="ref" type="xs:QName"/>
 </xs:attributeGroup>

 <xs:group name="typeDefParticle">
  <xs:annotation>
    <xs:documentation>
   'complexType' uses this</xs:documentation></xs:annotation>
  <xs:choice>
   <xs:element name="group" type="xs:groupRef"/>
   <xs:element ref="xs:all"/>
   <xs:element ref="xs:choice"/>
   <xs:element ref="xs:sequence"/>
  </xs:choice>
 </xs:group>
 
 

 <xs:group name="nestedParticle">
  <xs:choice>
   <xs:element name="element" type="xs:localElement"/>
   <xs:element name="group" type="xs:groupRef"/>
   <xs:element ref="xs:choice"/>
   <xs:element ref="xs:sequence"/>
   <xs:element ref="xs:any"/>
  </xs:choice>
 </xs:group>
 
 <xs:group name="particle">
  <xs:choice>
   <xs:element name="element" type="xs:localElement"/>
   <xs:element name="group" type="xs:groupRef"/>
   <xs:element ref="xs:all"/>
   <xs:element ref="xs:choice"/>
   <xs:element ref="xs:sequence"/>
   <xs:element ref="xs:any"/>
  </xs:choice>
 </xs:group>
 
 <xs:complexType name="attribute">
  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:sequence>
     <xs:element name="simpleType" minOccurs="0" type="xs:localSimpleType"/>
    </xs:sequence>
    <xs:attributeGroup ref="xs:defRef"/>
    <xs:attribute name="type" type="xs:QName"/>
    <xs:attribute name="use" use="optional" default="optional">
     <xs:simpleType>
      <xs:restriction base="xs:NMTOKEN">
       <xs:enumeration value="prohibited"/>
       <xs:enumeration value="optional"/>
       <xs:enumeration value="required"/>
      </xs:restriction>
     </xs:simpleType>
    </xs:attribute>
    <xs:attribute name="default" type="xs:string"/>
    <xs:attribute name="fixed" type="xs:string"/>
    <xs:attribute name="form" type="xs:formChoice"/>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="topLevelAttribute">
  <xs:complexContent>
   <xs:restriction base="xs:attribute">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:element name="simpleType" minOccurs="0" type="xs:localSimpleType"/>
    </xs:sequence>
    <xs:attribute name="ref" use="prohibited"/>
    <xs:attribute name="form" use="prohibited"/>
    <xs:attribute name="use" use="prohibited"/>
    <xs:attribute name="name" use="required" type="xs:NCName"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:group name="attrDecls">
  <xs:sequence>
   <xs:choice minOccurs="0" maxOccurs="unbounded">
    <xs:element name="attribute" type="xs:attribute"/>
    <xs:element name="attributeGroup" type="xs:attributeGroupRef"/>
   </xs:choice>
   <xs:element ref="xs:anyAttribute" minOccurs="0"/>
  </xs:sequence>
 </xs:group>

 <xs:element name="anyAttribute" type="xs:wildcard" id="anyAttribute">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-anyAttribute"/>
  </xs:annotation>
 </xs:element>

 <xs:group name="complexTypeModel">
  <xs:choice>
      <xs:element ref="xs:simpleContent"/>
      <xs:element ref="xs:complexContent"/>
      <xs:sequence>
       <xs:annotation>
        <xs:documentation>
   This branch is short for
   &lt;complexContent>
   &lt;restriction base="xs:anyType">
   ...
   &lt;/restriction>
   &lt;/complexContent></xs:documentation>
       </xs:annotation>
       <xs:group ref="xs:typeDefParticle" minOccurs="0"/>
       <xs:group ref="xs:attrDecls"/>
      </xs:sequence>
  </xs:choice>
 </xs:group>

 <xs:complexType name="complexType" abstract="true">
  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:group ref="xs:complexTypeModel"/>
    <xs:attribute name="name" type="xs:NCName">
     <xs:annotation>
      <xs:documentation>
      Will be restricted to required or forbidden</xs:documentation>
     </xs:annotation>
    </xs:attribute>
    <xs:attribute name="mixed" type="xs:boolean" use="optional" default="false">
     <xs:annotation>
      <xs:documentation>
      Not allowed if simpleContent child is chosen.
      May be overriden by setting on complexContent child.</xs:documentation>
    </xs:annotation>
    </xs:attribute>
    <xs:attribute name="abstract" type="xs:boolean" use="optional" default="false"/>
    <xs:attribute name="final" type="xs:derivationSet"/>
    <xs:attribute name="block" type="xs:derivationSet"/>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="topLevelComplexType">
  <xs:complexContent>
   <xs:restriction base="xs:complexType">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:group ref="xs:complexTypeModel"/>
    </xs:sequence>
    <xs:attribute name="name" type="xs:NCName" use="required"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="localComplexType">
  <xs:complexContent>
   <xs:restriction base="xs:complexType">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:group ref="xs:complexTypeModel"/>
    </xs:sequence>
    <xs:attribute name="name" use="prohibited"/>
    <xs:attribute name="abstract" use="prohibited"/>
    <xs:attribute name="final" use="prohibited"/>
    <xs:attribute name="block" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="restrictionType">
  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:sequence>
     <xs:choice minOccurs="0">
      <xs:group ref="xs:typeDefParticle"/>
      <xs:group ref="xs:simpleRestrictionModel"/>
     </xs:choice>
     <xs:group ref="xs:attrDecls"/>
    </xs:sequence>
    <xs:attribute name="base" type="xs:QName" use="required"/>
   </xs:extension>
  </xs:complexContent>       
 </xs:complexType>

 <xs:complexType name="complexRestrictionType">
  <xs:complexContent>
   <xs:restriction base="xs:restrictionType">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:choice minOccurs="0">
      <xs:annotation>
       <xs:documentation>This choice is added simply to
                   make this a valid restriction per the REC</xs:documentation>
      </xs:annotation>
      <xs:group ref="xs:typeDefParticle"/>
     </xs:choice>
     <xs:group ref="xs:attrDecls"/>
    </xs:sequence>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>       
 </xs:complexType>

 <xs:complexType name="extensionType">
  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:sequence>
     <xs:group ref="xs:typeDefParticle" minOccurs="0"/>
     <xs:group ref="xs:attrDecls"/>
    </xs:sequence>
    <xs:attribute name="base" type="xs:QName" use="required"/>
   </xs:extension>
  </xs:complexContent>       
 </xs:complexType>

 <xs:element name="complexContent" id="complexContent">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-complexContent"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:annotated">
     <xs:choice>
      <xs:element name="restriction" type="xs:complexRestrictionType"/>
      <xs:element name="extension" type="xs:extensionType"/>
     </xs:choice>     
     <xs:attribute name="mixed" type="xs:boolean">
      <xs:annotation>
       <xs:documentation>
       Overrides any setting on complexType parent.</xs:documentation>
      </xs:annotation>
    </xs:attribute>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>
 </xs:element>

 <xs:complexType name="simpleRestrictionType">
  <xs:complexContent>
   <xs:restriction base="xs:restrictionType">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:choice minOccurs="0">
      <xs:annotation>
       <xs:documentation>This choice is added simply to
                   make this a valid restriction per the REC</xs:documentation>
      </xs:annotation>
      <xs:group ref="xs:simpleRestrictionModel"/>
     </xs:choice>
     <xs:group ref="xs:attrDecls"/>
    </xs:sequence>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:complexType name="simpleExtensionType">
  <xs:complexContent>
   <xs:restriction base="xs:extensionType">
    <xs:sequence>
     <xs:annotation>
      <xs:documentation>
      No typeDefParticle group reference</xs:documentation>
     </xs:annotation>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:group ref="xs:attrDecls"/>
    </xs:sequence>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:element name="simpleContent" id="simpleContent">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-simpleContent"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:annotated">
     <xs:choice>
      <xs:element name="restriction" type="xs:simpleRestrictionType"/>
      <xs:element name="extension" type="xs:simpleExtensionType"/>
     </xs:choice>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>
 </xs:element>
 
 <xs:element name="complexType" type="xs:topLevelComplexType" id="complexType">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-complexType"/>
  </xs:annotation>
 </xs:element>


  <xs:simpleType name="blockSet">
   <xs:annotation>
    <xs:documentation>
    A utility type, not for public use</xs:documentation>
    <xs:documentation>
    #all or (possibly empty) subset of {substitution, extension,
    restriction}</xs:documentation>
   </xs:annotation>
   <xs:union>
    <xs:simpleType>    
     <xs:restriction base="xs:token">
      <xs:enumeration value="#all"/>
     </xs:restriction>
    </xs:simpleType>
    <xs:simpleType>
     <xs:list>
      <xs:simpleType>
       <xs:restriction base="xs:derivationControl">
        <xs:enumeration value="extension"/>
        <xs:enumeration value="restriction"/>
        <xs:enumeration value="substitution"/>
       </xs:restriction>
      </xs:simpleType>
     </xs:list>
    </xs:simpleType>
   </xs:union>  
  </xs:simpleType>

 <xs:complexType name="element" abstract="true">
  <xs:annotation>
   <xs:documentation>
   The element element can be used either
   at the top level to define an element-type binding globally,
   or within a content model to either reference a globally-defined
   element or type or declare an element-type binding locally.
   The ref form is not allowed at the top level.</xs:documentation>
  </xs:annotation>

  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:sequence>
     <xs:choice minOccurs="0">
      <xs:element name="simpleType" type="xs:localSimpleType"/>
      <xs:element name="complexType" type="xs:localComplexType"/>
     </xs:choice>
     <xs:group ref="xs:identityConstraint" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
    <xs:attributeGroup ref="xs:defRef"/>
    <xs:attribute name="type" type="xs:QName"/>
    <xs:attribute name="substitutionGroup" type="xs:QName"/>
    <xs:attributeGroup ref="xs:occurs"/>
    <xs:attribute name="default" type="xs:string"/>
    <xs:attribute name="fixed" type="xs:string"/>
    <xs:attribute name="nillable" type="xs:boolean" use="optional" default="false"/>
    <xs:attribute name="abstract" type="xs:boolean" use="optional" default="false"/>
    <xs:attribute name="final" type="xs:derivationSet"/>
    <xs:attribute name="block" type="xs:blockSet"/>
    <xs:attribute name="form" type="xs:formChoice"/>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="topLevelElement">
  <xs:complexContent>
   <xs:restriction base="xs:element">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:choice minOccurs="0">
      <xs:element name="simpleType" type="xs:localSimpleType"/>
      <xs:element name="complexType" type="xs:localComplexType"/>
     </xs:choice>
     <xs:group ref="xs:identityConstraint" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
    <xs:attribute name="ref" use="prohibited"/>
    <xs:attribute name="form" use="prohibited"/>
    <xs:attribute name="minOccurs" use="prohibited"/>
    <xs:attribute name="maxOccurs" use="prohibited"/>
    <xs:attribute name="name" use="required" type="xs:NCName"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="localElement">
  <xs:complexContent>
   <xs:restriction base="xs:element">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:choice minOccurs="0">
      <xs:element name="simpleType" type="xs:localSimpleType"/>
      <xs:element name="complexType" type="xs:localComplexType"/>
     </xs:choice>
     <xs:group ref="xs:identityConstraint" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
    <xs:attribute name="substitutionGroup" use="prohibited"/>
    <xs:attribute name="final" use="prohibited"/>
    <xs:attribute name="abstract" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:element name="element" type="xs:topLevelElement" id="element">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-element"/>
  </xs:annotation>
 </xs:element>

 <xs:complexType name="group" abstract="true">
  <xs:annotation>
   <xs:documentation>
   group type for explicit groups, named top-level groups and
   group references</xs:documentation>
  </xs:annotation>
  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:group ref="xs:particle" minOccurs="0" maxOccurs="unbounded"/>
    <xs:attributeGroup ref="xs:defRef"/>
    <xs:attributeGroup ref="xs:occurs"/>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="realGroup">
  <xs:complexContent>
   <xs:restriction base="xs:group">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:choice minOccurs="0" maxOccurs="1">
      <xs:element ref="xs:all"/>
      <xs:element ref="xs:choice"/>
      <xs:element ref="xs:sequence"/>
     </xs:choice>
    </xs:sequence>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:complexType name="namedGroup">
  <xs:complexContent>
   <xs:restriction base="xs:realGroup">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:choice minOccurs="1" maxOccurs="1">
      <xs:element name="all">
       <xs:complexType>
        <xs:complexContent>
         <xs:restriction base="xs:all">
          <xs:group ref="xs:allModel"/>
          <xs:attribute name="minOccurs" use="prohibited"/>
          <xs:attribute name="maxOccurs" use="prohibited"/>
          <xs:anyAttribute namespace="##other" processContents="lax"/>
         </xs:restriction>
        </xs:complexContent>
       </xs:complexType>
      </xs:element>
      <xs:element name="choice" type="xs:simpleExplicitGroup"/>
      <xs:element name="sequence" type="xs:simpleExplicitGroup"/>
     </xs:choice>
    </xs:sequence>
    <xs:attribute name="name" use="required" type="xs:NCName"/>
    <xs:attribute name="ref" use="prohibited"/>
    <xs:attribute name="minOccurs" use="prohibited"/>
    <xs:attribute name="maxOccurs" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:complexType name="groupRef">
  <xs:complexContent>
   <xs:restriction base="xs:realGroup">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
    </xs:sequence>
    <xs:attribute name="ref" use="required" type="xs:QName"/>
    <xs:attribute name="name" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:complexType name="explicitGroup">
  <xs:annotation>
   <xs:documentation>
   group type for the three kinds of group</xs:documentation>
  </xs:annotation>
  <xs:complexContent>
   <xs:restriction base="xs:group">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:group ref="xs:nestedParticle" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
    <xs:attribute name="name" type="xs:NCName" use="prohibited"/>
    <xs:attribute name="ref" type="xs:QName" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="simpleExplicitGroup">
  <xs:complexContent>
   <xs:restriction base="xs:explicitGroup">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:group ref="xs:nestedParticle" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
    <xs:attribute name="minOccurs" use="prohibited"/>
    <xs:attribute name="maxOccurs" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:group name="allModel">
  <xs:sequence>
      <xs:element ref="xs:annotation" minOccurs="0"/>
      <xs:choice minOccurs="0" maxOccurs="unbounded">
       <xs:annotation>
        <xs:documentation>This choice with min/max is here to
                          avoid a pblm with the Elt:All/Choice/Seq
                          Particle derivation constraint</xs:documentation>
       </xs:annotation>
       <xs:element name="element" type="xs:narrowMaxMin"/>
      </xs:choice>
     </xs:sequence>
 </xs:group>
 
 
 <xs:complexType name="narrowMaxMin">
  <xs:annotation>
   <xs:documentation>restricted max/min</xs:documentation>
  </xs:annotation>
  <xs:complexContent>
   <xs:restriction base="xs:localElement">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:choice minOccurs="0">
      <xs:element name="simpleType" type="xs:localSimpleType"/>
      <xs:element name="complexType" type="xs:localComplexType"/>
     </xs:choice>
     <xs:group ref="xs:identityConstraint" minOccurs="0" maxOccurs="unbounded"/>
    </xs:sequence>
    <xs:attribute name="minOccurs" use="optional" default="1">
     <xs:simpleType>
      <xs:restriction base="xs:nonNegativeInteger">
       <xs:enumeration value="0"/>
       <xs:enumeration value="1"/>
      </xs:restriction>
     </xs:simpleType>
    </xs:attribute>
    <xs:attribute name="maxOccurs" use="optional" default="1">
     <xs:simpleType>
      <xs:restriction base="xs:allNNI">
       <xs:enumeration value="0"/>
       <xs:enumeration value="1"/>
      </xs:restriction>
     </xs:simpleType>
    </xs:attribute>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

  <xs:complexType name="all">
   <xs:annotation>
    <xs:documentation>
   Only elements allowed inside</xs:documentation>
   </xs:annotation>
   <xs:complexContent>
    <xs:restriction base="xs:explicitGroup">
     <xs:group ref="xs:allModel"/>
     <xs:attribute name="minOccurs" use="optional" default="1">
      <xs:simpleType>
       <xs:restriction base="xs:nonNegativeInteger">
        <xs:enumeration value="0"/>
        <xs:enumeration value="1"/>
       </xs:restriction>
      </xs:simpleType>
     </xs:attribute>
     <xs:attribute name="maxOccurs" use="optional" default="1">
      <xs:simpleType>
       <xs:restriction base="xs:allNNI">
        <xs:enumeration value="1"/>
       </xs:restriction>
      </xs:simpleType>
     </xs:attribute>
     <xs:anyAttribute namespace="##other" processContents="lax"/>
    </xs:restriction>
   </xs:complexContent>
  </xs:complexType>

 <xs:element name="all" id="all" type="xs:all">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-all"/>
  </xs:annotation>
 </xs:element>

 <xs:element name="choice" type="xs:explicitGroup" id="choice">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-choice"/>
  </xs:annotation>
 </xs:element>

 <xs:element name="sequence" type="xs:explicitGroup" id="sequence">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-sequence"/>
  </xs:annotation>
 </xs:element>

 <xs:element name="group" type="xs:namedGroup" id="group">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-group"/>
  </xs:annotation>
 </xs:element>

 <xs:complexType name="wildcard">
  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:attribute name="namespace" type="xs:namespaceList" use="optional" default="##any"/>
    <xs:attribute name="processContents" use="optional" default="strict">
     <xs:simpleType>
      <xs:restriction base="xs:NMTOKEN">
       <xs:enumeration value="skip"/>
       <xs:enumeration value="lax"/>
       <xs:enumeration value="strict"/>
      </xs:restriction>
     </xs:simpleType>
    </xs:attribute>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>

 <xs:element name="any" id="any">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-any"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:wildcard">
     <xs:attributeGroup ref="xs:occurs"/>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>
 </xs:element>

  <xs:annotation>
   <xs:documentation>
   simple type for the value of the 'namespace' attr of
   'any' and 'anyAttribute'</xs:documentation>
  </xs:annotation>
  <xs:annotation>
   <xs:documentation>
   Value is
              ##any      - - any non-conflicting WFXML/attribute at all

              ##other    - - any non-conflicting WFXML/attribute from
                              namespace other than targetNS

              ##local    - - any unqualified non-conflicting WFXML/attribute 

              one or     - - any non-conflicting WFXML/attribute from
              more URI        the listed namespaces
              references
              (space separated)

    ##targetNamespace or ##local may appear in the above list, to
        refer to the targetNamespace of the enclosing
        schema or an absent targetNamespace respectively</xs:documentation>
  </xs:annotation>

 <xs:simpleType name="namespaceList">
  <xs:annotation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
  </xs:annotation>
  <xs:union>
   <xs:simpleType>
    <xs:restriction base="xs:token">
     <xs:enumeration value="##any"/>
     <xs:enumeration value="##other"/>
    </xs:restriction>
   </xs:simpleType>
   <xs:simpleType>
    <xs:list>
     <xs:simpleType>
      <xs:union memberTypes="xs:anyURI">
       <xs:simpleType>
        <xs:restriction base="xs:token">
         <xs:enumeration value="##targetNamespace"/>
         <xs:enumeration value="##local"/>
        </xs:restriction>
       </xs:simpleType>
      </xs:union>
     </xs:simpleType>
    </xs:list>
   </xs:simpleType>
  </xs:union>
 </xs:simpleType>

 <xs:element name="attribute" type="xs:topLevelAttribute" id="attribute">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-attribute"/>
  </xs:annotation>
 </xs:element>

 <xs:complexType name="attributeGroup" abstract="true">
  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:group ref="xs:attrDecls"/>
    <xs:attributeGroup ref="xs:defRef"/>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>
 
 <xs:complexType name="namedAttributeGroup">
  <xs:complexContent>
   <xs:restriction base="xs:attributeGroup">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
     <xs:group ref="xs:attrDecls"/>
    </xs:sequence>
    <xs:attribute name="name" use="required" type="xs:NCName"/>
    <xs:attribute name="ref" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:complexType name="attributeGroupRef">
  <xs:complexContent>
   <xs:restriction base="xs:attributeGroup">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
    </xs:sequence>
    <xs:attribute name="ref" use="required" type="xs:QName"/>
    <xs:attribute name="name" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

 <xs:element name="attributeGroup" type="xs:namedAttributeGroup" id="attributeGroup">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-attributeGroup"/>
  </xs:annotation>
 </xs:element>

 <xs:element name="include" id="include">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-include"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:annotated">
     <xs:attribute name="schemaLocation" type="xs:anyURI" use="required"/>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>
 </xs:element>

 <xs:element name="redefine" id="redefine">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-redefine"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:openAttrs">
     <xs:choice minOccurs="0" maxOccurs="unbounded">
      <xs:element ref="xs:annotation"/>
      <xs:group ref="xs:redefinable"/>
     </xs:choice>
     <xs:attribute name="schemaLocation" type="xs:anyURI" use="required"/>
     <xs:attribute name="id" type="xs:ID"/>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>
 </xs:element>

 <xs:element name="import" id="import">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-import"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:annotated">
     <xs:attribute name="namespace" type="xs:anyURI"/>
     <xs:attribute name="schemaLocation" type="xs:anyURI"/>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>
 </xs:element>

 <xs:element name="selector" id="selector">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-selector"/>
  </xs:annotation>
  <xs:complexType>
  <xs:complexContent>
   <xs:extension base="xs:annotated">
     <xs:attribute name="xpath" use="required">
      <xs:simpleType>
       <xs:annotation>
        <xs:documentation>A subset of XPath expressions for use
in selectors</xs:documentation>
        <xs:documentation>A utility type, not for public
use</xs:documentation>
       </xs:annotation>
       <xs:restriction base="xs:token">
        <xs:annotation>
         <xs:documentation>The following pattern is intended to allow XPath
                           expressions per the following EBNF:
          Selector    ::=    Path ( '|' Path )*  
          Path    ::=    ('.//')? Step ( '/' Step )*  
          Step    ::=    '.' | NameTest  
          NameTest    ::=    QName | '*' | NCName ':' '*'  
                           child:: is also allowed
         </xs:documentation>
        </xs:annotation>
        <xs:pattern value="(\.//)?(((child::)?((\i\c*:)?(\i\c*|\*)))|\.)(/(((child::)?((\i\c*:)?(\i\c*|\*)))|\.))*(\|(\.//)?(((child::)?((\i\c*:)?(\i\c*|\*)))|\.)(/(((child::)?((\i\c*:)?(\i\c*|\*)))|\.))*)*">
        </xs:pattern>
       </xs:restriction>
      </xs:simpleType>
     </xs:attribute>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>
 </xs:element>

 <xs:element name="field" id="field">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-field"/>
  </xs:annotation>
  <xs:complexType>
  <xs:complexContent>
   <xs:extension base="xs:annotated">
     <xs:attribute name="xpath" use="required">
      <xs:simpleType>
       <xs:annotation>
        <xs:documentation>A subset of XPath expressions for use
in fields</xs:documentation>
        <xs:documentation>A utility type, not for public
use</xs:documentation>
       </xs:annotation>
       <xs:restriction base="xs:token">
        <xs:annotation>
         <xs:documentation>The following pattern is intended to allow XPath
                           expressions per the same EBNF as for selector,
                           with the following change:
          Path    ::=    ('.//')? ( Step '/' )* ( Step | '@' NameTest ) 
         </xs:documentation>
        </xs:annotation>
        <xs:pattern value="(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*">
        </xs:pattern>
       </xs:restriction>
      </xs:simpleType>
     </xs:attribute>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>
 </xs:element>

 <xs:complexType name="keybase">
  <xs:complexContent>
   <xs:extension base="xs:annotated">
    <xs:sequence>
     <xs:element ref="xs:selector"/>
     <xs:element ref="xs:field" minOccurs="1" maxOccurs="unbounded"/>
    </xs:sequence>
    <xs:attribute name="name" type="xs:NCName" use="required"/>
   </xs:extension>
  </xs:complexContent>
 </xs:complexType>

 <xs:group name="identityConstraint">
  <xs:annotation>
   <xs:documentation>The three kinds of identity constraints, all with
                     type of or derived from 'keybase'.
   </xs:documentation>
  </xs:annotation>
  <xs:choice>
   <xs:element ref="xs:unique"/>
   <xs:element ref="xs:key"/>
   <xs:element ref="xs:keyref"/>
  </xs:choice>
 </xs:group>

 <xs:element name="unique" type="xs:keybase" id="unique">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-unique"/>
  </xs:annotation>
 </xs:element>
 <xs:element name="key" type="xs:keybase" id="key">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-key"/>
  </xs:annotation>
 </xs:element>
 <xs:element name="keyref" id="keyref">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-keyref"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:keybase">
     <xs:attribute name="refer" type="xs:QName" use="required"/>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>
 </xs:element>

 <xs:element name="notation" id="notation">
  <xs:annotation>
   <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-notation"/>
  </xs:annotation>
  <xs:complexType>
   <xs:complexContent>
    <xs:extension base="xs:annotated">
     <xs:attribute name="name" type="xs:NCName" use="required"/>
     <xs:attribute name="public" type="xs:public"/>
     <xs:attribute name="system" type="xs:anyURI"/>
    </xs:extension>
   </xs:complexContent>
  </xs:complexType>
 </xs:element>

 <xs:simpleType name="public">
  <xs:annotation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
   <xs:documentation>
   A public identifier, per ISO 8879</xs:documentation>
  </xs:annotation>
  <xs:restriction base="xs:token"/>
 </xs:simpleType>

 <xs:element name="appinfo" id="appinfo">
   <xs:annotation>
     <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-appinfo"/>
   </xs:annotation>
   <xs:complexType mixed="true">
    <xs:sequence minOccurs="0" maxOccurs="unbounded">
     <xs:any processContents="lax"/>
    </xs:sequence>
    <xs:attribute name="source" type="xs:anyURI"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:complexType>
 </xs:element>

 <xs:element name="documentation" id="documentation">
   <xs:annotation>
     <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-documentation"/>
   </xs:annotation>
   <xs:complexType mixed="true">
    <xs:sequence minOccurs="0" maxOccurs="unbounded">
     <xs:any processContents="lax"/>
    </xs:sequence>
    <xs:attribute name="source" type="xs:anyURI"/>
    <xs:attribute ref="xml:lang"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:complexType>
 </xs:element>

 <xs:element name="annotation" id="annotation">
   <xs:annotation>
     <xs:documentation source="http://www.w3.org/TR/xmlschema-1/#element-annotation"/>
   </xs:annotation>
   <xs:complexType>
    <xs:complexContent>
     <xs:extension base="xs:openAttrs">
      <xs:choice minOccurs="0" maxOccurs="unbounded">
       <xs:element ref="xs:appinfo"/>
       <xs:element ref="xs:documentation"/>
      </xs:choice>
      <xs:attribute name="id" type="xs:ID"/>
     </xs:extension>
    </xs:complexContent>
   </xs:complexType>
 </xs:element>

 <xs:annotation>
  <xs:documentation>
   notations for use within XML Schema schemas</xs:documentation>
 </xs:annotation>

 <xs:notation name="XMLSchemaStructures" public="structures" system="http://www.w3.org/2000/08/XMLSchema.xsd"/>
 <xs:notation name="XML" public="REC-xml-19980210" system="http://www.w3.org/TR/1998/REC-xml-19980210"/>
  
 <xs:complexType name="anyType" mixed="true">
  <xs:annotation>
   <xs:documentation>
   Not the real urType, but as close an approximation as we can
   get in the XML representation</xs:documentation>
  </xs:annotation>
  <xs:sequence>
   <xs:any minOccurs="0" maxOccurs="unbounded" processContents="lax"/>
  </xs:sequence>
  <xs:anyAttribute processContents="lax"/>
 </xs:complexType>

  <xs:annotation>
    <xs:documentation>
      First the built-in primitive datatypes.  These definitions are for
      information only, the real built-in definitions are magic.
    </xs:documentation>

    <xs:documentation>
      For each built-in datatype in this schema (both primitive and
      derived) can be uniquely addressed via a URI constructed
      as follows:
        1) the base URI is the URI of the XML Schema namespace
        2) the fragment identifier is the name of the datatype

      For example, to address the int datatype, the URI is:

        http://www.w3.org/2001/XMLSchema#int

      Additionally, each facet definition element can be uniquely
      addressed via a URI constructed as follows:
        1) the base URI is the URI of the XML Schema namespace
        2) the fragment identifier is the name of the facet

      For example, to address the maxInclusive facet, the URI is:

        http://www.w3.org/2001/XMLSchema#maxInclusive

      Additionally, each facet usage in a built-in datatype definition
      can be uniquely addressed via a URI constructed as follows:
        1) the base URI is the URI of the XML Schema namespace
        2) the fragment identifier is the name of the datatype, followed
           by a period (".") followed by the name of the facet

      For example, to address the usage of the maxInclusive facet in
      the definition of int, the URI is:

        http://www.w3.org/2001/XMLSchema#int.maxInclusive

    </xs:documentation>
  </xs:annotation>

  <xs:simpleType name="string" id="string">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality" value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
                source="http://www.w3.org/TR/xmlschema-2/#string"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="preserve" id="string.preserve"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="boolean" id="boolean">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality" value="finite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#boolean"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse" fixed="true"
        id="boolean.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="float" id="float">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="total"/>
        <hfp:hasProperty name="bounded" value="true"/>
        <hfp:hasProperty name="cardinality" value="finite"/>
        <hfp:hasProperty name="numeric" value="true"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#float"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse" fixed="true"
        id="float.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="double" id="double">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="total"/>
        <hfp:hasProperty name="bounded" value="true"/>
        <hfp:hasProperty name="cardinality" value="finite"/>
        <hfp:hasProperty name="numeric" value="true"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#double"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="double.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="decimal" id="decimal">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="totalDigits"/>
        <hfp:hasFacet name="fractionDigits"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="total"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="true"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#decimal"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="decimal.whiteSpace"/>
    </xs:restriction>
   </xs:simpleType>

   <xs:simpleType name="duration" id="duration">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#duration"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="duration.whiteSpace"/>
    </xs:restriction>
   </xs:simpleType>

 <xs:simpleType name="dateTime" id="dateTime">
    <xs:annotation>
    <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#dateTime"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="dateTime.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="time" id="time">
    <xs:annotation>
    <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#time"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="time.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="date" id="date">
   <xs:annotation>
    <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#date"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="date.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="gYearMonth" id="gYearMonth">
   <xs:annotation>
    <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#gYearMonth"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="gYearMonth.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="gYear" id="gYear">
    <xs:annotation>
    <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#gYear"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="gYear.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

 <xs:simpleType name="gMonthDay" id="gMonthDay">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
       <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#gMonthDay"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
         <xs:whiteSpace value="collapse" fixed="true"
                id="gMonthDay.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="gDay" id="gDay">
    <xs:annotation>
  <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#gDay"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
         <xs:whiteSpace value="collapse"  fixed="true"
                id="gDay.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

 <xs:simpleType name="gMonth" id="gMonth">
    <xs:annotation>
  <xs:appinfo>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="maxInclusive"/>
        <hfp:hasFacet name="maxExclusive"/>
        <hfp:hasFacet name="minInclusive"/>
        <hfp:hasFacet name="minExclusive"/>
        <hfp:hasProperty name="ordered" value="partial"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#gMonth"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
         <xs:whiteSpace value="collapse"  fixed="true"
                id="gMonth.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

   <xs:simpleType name="hexBinary" id="hexBinary">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#binary"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse" fixed="true"
        id="hexBinary.whiteSpace"/>
    </xs:restriction>
   </xs:simpleType>

 <xs:simpleType name="base64Binary" id="base64Binary">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
                source="http://www.w3.org/TR/xmlschema-2/#base64Binary"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse" fixed="true"
        id="base64Binary.whiteSpace"/>
    </xs:restriction>
   </xs:simpleType>

   <xs:simpleType name="anyURI" id="anyURI">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#anyURI"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="anyURI.whiteSpace"/>
    </xs:restriction>
   </xs:simpleType>

  <xs:simpleType name="QName" id="QName">
    <xs:annotation>
        <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#QName"/>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="QName.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

   <xs:simpleType name="NOTATION" id="NOTATION">
    <xs:annotation>
        <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#NOTATION"/>
      <xs:documentation>
        NOTATION cannot be used directly in a schema; rather a type
        must be derived from it by specifying at least one enumeration
        facet whose value is the name of a NOTATION declared in the
        schema.
      </xs:documentation>
    </xs:annotation>
    <xs:restriction base="xs:anySimpleType">
      <xs:whiteSpace value="collapse"  fixed="true"
        id="NOTATION.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:annotation>
    <xs:documentation>
      Now the derived primitive types
    </xs:documentation>
  </xs:annotation>

  <xs:simpleType name="normalizedString" id="normalizedString">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#normalizedString"/>
    </xs:annotation>
    <xs:restriction base="xs:string">
      <xs:whiteSpace value="replace"
        id="normalizedString.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="token" id="token">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#token"/>
    </xs:annotation>
    <xs:restriction base="xs:normalizedString">
      <xs:whiteSpace value="collapse" id="token.whiteSpace"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="language" id="language">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#language"/>
    </xs:annotation>
    <xs:restriction base="xs:token">
      <xs:pattern
        value="[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"
                id="language.pattern">
        <xs:annotation>
          <xs:documentation
                source="http://www.ietf.org/rfc/rfc3066.txt">
            pattern specifies the content of section 2.12 of XML 1.0e2
            and RFC 3066 (Revised version of RFC 1766).
          </xs:documentation>
        </xs:annotation>
      </xs:pattern>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="IDREFS" id="IDREFS">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#IDREFS"/>
    </xs:annotation>
    <xs:restriction>
      <xs:simpleType>
        <xs:list itemType="xs:IDREF"/>
      </xs:simpleType>
        <xs:minLength value="1" id="IDREFS.minLength"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="ENTITIES" id="ENTITIES">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#ENTITIES"/>
    </xs:annotation>
    <xs:restriction>
      <xs:simpleType>
        <xs:list itemType="xs:ENTITY"/>
      </xs:simpleType>
        <xs:minLength value="1" id="ENTITIES.minLength"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="NMTOKEN" id="NMTOKEN">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#NMTOKEN"/>
    </xs:annotation>
    <xs:restriction base="xs:token">
      <xs:pattern value="\c+" id="NMTOKEN.pattern">
        <xs:annotation>
          <xs:documentation
                source="http://www.w3.org/TR/REC-xml#NT-Nmtoken">
            pattern matches production 7 from the XML spec
          </xs:documentation>
        </xs:annotation>
      </xs:pattern>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="NMTOKENS" id="NMTOKENS">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasFacet name="length"/>
        <hfp:hasFacet name="minLength"/>
        <hfp:hasFacet name="maxLength"/>
        <hfp:hasFacet name="enumeration"/>
        <hfp:hasFacet name="whiteSpace"/>
        <hfp:hasFacet name="pattern"/>
        <hfp:hasProperty name="ordered" value="false"/>
        <hfp:hasProperty name="bounded" value="false"/>
        <hfp:hasProperty name="cardinality"
                value="countably infinite"/>
        <hfp:hasProperty name="numeric" value="false"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#NMTOKENS"/>
    </xs:annotation>
    <xs:restriction>
      <xs:simpleType>
        <xs:list itemType="xs:NMTOKEN"/>
      </xs:simpleType>
        <xs:minLength value="1" id="NMTOKENS.minLength"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="Name" id="Name">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#Name"/>
    </xs:annotation>
    <xs:restriction base="xs:token">
      <xs:pattern value="\i\c*" id="Name.pattern">
        <xs:annotation>
          <xs:documentation
                        source="http://www.w3.org/TR/REC-xml#NT-Name">
            pattern matches production 5 from the XML spec
          </xs:documentation>
        </xs:annotation>
      </xs:pattern>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="NCName" id="NCName">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#NCName"/>
    </xs:annotation>
    <xs:restriction base="xs:Name">
      <xs:pattern value="[\i-[:]][\c-[:]]*" id="NCName.pattern">
        <xs:annotation>
          <xs:documentation
                source="http://www.w3.org/TR/REC-xml-names/#NT-NCName">
            pattern matches production 4 from the Namespaces in XML spec
          </xs:documentation>
        </xs:annotation>
      </xs:pattern>
    </xs:restriction>
  </xs:simpleType>

   <xs:simpleType name="ID" id="ID">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#ID"/>
    </xs:annotation>
    <xs:restriction base="xs:NCName"/>
   </xs:simpleType>

   <xs:simpleType name="IDREF" id="IDREF">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#IDREF"/>
    </xs:annotation>
    <xs:restriction base="xs:NCName"/>
   </xs:simpleType>

   <xs:simpleType name="ENTITY" id="ENTITY">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#ENTITY"/>
    </xs:annotation>
    <xs:restriction base="xs:NCName"/>
   </xs:simpleType>

  <xs:simpleType name="integer" id="integer">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#integer"/>
    </xs:annotation>
    <xs:restriction base="xs:decimal">
      <xs:fractionDigits value="0" fixed="true" id="integer.fractionDigits"/>
      <xs:pattern value="[\-+]?[0-9]+"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="nonPositiveInteger" id="nonPositiveInteger">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#nonPositiveInteger"/>
    </xs:annotation>
    <xs:restriction base="xs:integer">
      <xs:maxInclusive value="0" id="nonPositiveInteger.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="negativeInteger" id="negativeInteger">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#negativeInteger"/>
    </xs:annotation>
    <xs:restriction base="xs:nonPositiveInteger">
      <xs:maxInclusive value="-1" id="negativeInteger.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="long" id="long">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasProperty name="bounded" value="true"/>
        <hfp:hasProperty name="cardinality" value="finite"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#long"/>
    </xs:annotation>
    <xs:restriction base="xs:integer">
      <xs:minInclusive value="-9223372036854775808" id="long.minInclusive"/>
      <xs:maxInclusive value="9223372036854775807" id="long.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="int" id="int">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#int"/>
    </xs:annotation>
    <xs:restriction base="xs:long">
      <xs:minInclusive value="-2147483648" id="int.minInclusive"/>
      <xs:maxInclusive value="2147483647" id="int.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="short" id="short">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#short"/>
    </xs:annotation>
    <xs:restriction base="xs:int">
      <xs:minInclusive value="-32768" id="short.minInclusive"/>
      <xs:maxInclusive value="32767" id="short.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="byte" id="byte">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#byte"/>
    </xs:annotation>
    <xs:restriction base="xs:short">
      <xs:minInclusive value="-128" id="byte.minInclusive"/>
      <xs:maxInclusive value="127" id="byte.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="nonNegativeInteger" id="nonNegativeInteger">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#nonNegativeInteger"/>
    </xs:annotation>
    <xs:restriction base="xs:integer">
      <xs:minInclusive value="0" id="nonNegativeInteger.minInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="unsignedLong" id="unsignedLong">
    <xs:annotation>
      <xs:appinfo>
        <hfp:hasProperty name="bounded" value="true"/>
        <hfp:hasProperty name="cardinality" value="finite"/>
      </xs:appinfo>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#unsignedLong"/>
    </xs:annotation>
    <xs:restriction base="xs:nonNegativeInteger">
      <xs:maxInclusive value="18446744073709551615"
        id="unsignedLong.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="unsignedInt" id="unsignedInt">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#unsignedInt"/>
    </xs:annotation>
    <xs:restriction base="xs:unsignedLong">
      <xs:maxInclusive value="4294967295"
        id="unsignedInt.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="unsignedShort" id="unsignedShort">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#unsignedShort"/>
    </xs:annotation>
    <xs:restriction base="xs:unsignedInt">
      <xs:maxInclusive value="65535"
        id="unsignedShort.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="unsignedByte" id="unsignedByte">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#unsignedByte"/>
    </xs:annotation>
    <xs:restriction base="xs:unsignedShort">
      <xs:maxInclusive value="255" id="unsignedByte.maxInclusive"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:simpleType name="positiveInteger" id="positiveInteger">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#positiveInteger"/>
    </xs:annotation>
    <xs:restriction base="xs:nonNegativeInteger">
      <xs:minInclusive value="1" id="positiveInteger.minInclusive"/>
    </xs:restriction>
  </xs:simpleType>

 <xs:simpleType name="derivationControl">
  <xs:annotation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
  </xs:annotation>
  <xs:restriction base="xs:NMTOKEN">
   <xs:enumeration value="substitution"/>
   <xs:enumeration value="extension"/>
   <xs:enumeration value="restriction"/>
   <xs:enumeration value="list"/>
   <xs:enumeration value="union"/>
  </xs:restriction>
 </xs:simpleType>

 <xs:group name="simpleDerivation">
  <xs:choice>
    <xs:element ref="xs:restriction"/>
    <xs:element ref="xs:list"/>
    <xs:element ref="xs:union"/>
  </xs:choice>
 </xs:group>

 <xs:simpleType name="simpleDerivationSet">
  <xs:annotation>
   <xs:documentation>
   #all or (possibly empty) subset of {restriction, union, list}
   </xs:documentation>
   <xs:documentation>
   A utility type, not for public use</xs:documentation>
  </xs:annotation>
  <xs:union>
   <xs:simpleType>
    <xs:restriction base="xs:token">
     <xs:enumeration value="#all"/>
    </xs:restriction>
   </xs:simpleType>
   <xs:simpleType>
    <xs:list>
     <xs:simpleType>
      <xs:restriction base="xs:derivationControl">
       <xs:enumeration value="list"/>
       <xs:enumeration value="union"/>
       <xs:enumeration value="restriction"/>
      </xs:restriction>
     </xs:simpleType>
    </xs:list>
   </xs:simpleType>
  </xs:union>
 </xs:simpleType>

  <xs:complexType name="simpleType" abstract="true">
    <xs:complexContent>
      <xs:extension base="xs:annotated">
        <xs:group ref="xs:simpleDerivation"/>
        <xs:attribute name="final" type="xs:simpleDerivationSet"/>
        <xs:attribute name="name" type="xs:NCName">
          <xs:annotation>
            <xs:documentation>
              Can be restricted to required or forbidden
            </xs:documentation>
          </xs:annotation>
        </xs:attribute>
      </xs:extension>
    </xs:complexContent>
  </xs:complexType>

  <xs:complexType name="topLevelSimpleType">
    <xs:complexContent>
      <xs:restriction base="xs:simpleType">
        <xs:sequence>
          <xs:element ref="xs:annotation" minOccurs="0"/>
          <xs:group ref="xs:simpleDerivation"/>
        </xs:sequence>
        <xs:attribute name="name" use="required"
             type="xs:NCName">
          <xs:annotation>
            <xs:documentation>
              Required at the top level
            </xs:documentation>
          </xs:annotation>
        </xs:attribute>
       <xs:anyAttribute namespace="##other" processContents="lax"/>
      </xs:restriction>
    </xs:complexContent>
  </xs:complexType>

  <xs:complexType name="localSimpleType">
    <xs:complexContent>
      <xs:restriction base="xs:simpleType">
        <xs:sequence>
          <xs:element ref="xs:annotation" minOccurs="0"/>
          <xs:group ref="xs:simpleDerivation"/>
        </xs:sequence>
        <xs:attribute name="name" use="prohibited">
          <xs:annotation>
            <xs:documentation>
              Forbidden when nested
            </xs:documentation>
          </xs:annotation>
        </xs:attribute>
        <xs:attribute name="final" use="prohibited"/>
       <xs:anyAttribute namespace="##other" processContents="lax"/>
      </xs:restriction>
    </xs:complexContent>
  </xs:complexType>

  <xs:element name="simpleType" type="xs:topLevelSimpleType" id="simpleType">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-simpleType"/>
    </xs:annotation>
  </xs:element>

  <xs:group name="facets">
   <xs:annotation>
    <xs:documentation>
       We should use a substitution group for facets, but
       that's ruled out because it would allow users to
       add their own, which we're not ready for yet.
    </xs:documentation>
   </xs:annotation>
   <xs:choice>
    <xs:element ref="xs:minExclusive"/>
    <xs:element ref="xs:minInclusive"/>
    <xs:element ref="xs:maxExclusive"/>
    <xs:element ref="xs:maxInclusive"/>
    <xs:element ref="xs:totalDigits"/>
    <xs:element ref="xs:fractionDigits"/>
    <xs:element ref="xs:length"/>
    <xs:element ref="xs:minLength"/>
    <xs:element ref="xs:maxLength"/>
    <xs:element ref="xs:enumeration"/>
    <xs:element ref="xs:whiteSpace"/>
    <xs:element ref="xs:pattern"/>
   </xs:choice>
  </xs:group>

  <xs:group name="simpleRestrictionModel">
   <xs:sequence>
    <xs:element name="simpleType" type="xs:localSimpleType" minOccurs="0"/>
    <xs:group ref="xs:facets" minOccurs="0" maxOccurs="unbounded"/>
   </xs:sequence>
  </xs:group>

  <xs:element name="restriction" id="restriction">
   <xs:complexType>
    <xs:annotation>
      <xs:documentation
                source="http://www.w3.org/TR/xmlschema-2/#element-restriction">
          base attribute and simpleType child are mutually
          exclusive, but one or other is required
        </xs:documentation>
      </xs:annotation>
      <xs:complexContent>
        <xs:extension base="xs:annotated">
         <xs:group ref="xs:simpleRestrictionModel"/>
         <xs:attribute name="base" type="xs:QName" use="optional"/>
        </xs:extension>
      </xs:complexContent>
    </xs:complexType>
  </xs:element>

  <xs:element name="list" id="list">
   <xs:complexType>
    <xs:annotation>
      <xs:documentation
                source="http://www.w3.org/TR/xmlschema-2/#element-list">
          itemType attribute and simpleType child are mutually
          exclusive, but one or other is required
        </xs:documentation>
      </xs:annotation>
      <xs:complexContent>
        <xs:extension base="xs:annotated">
          <xs:sequence>
            <xs:element name="simpleType" type="xs:localSimpleType"
                minOccurs="0"/>
          </xs:sequence>
          <xs:attribute name="itemType" type="xs:QName" use="optional"/>
        </xs:extension>
      </xs:complexContent>
    </xs:complexType>
  </xs:element>

  <xs:element name="union" id="union">
   <xs:complexType>
    <xs:annotation>
      <xs:documentation
                source="http://www.w3.org/TR/xmlschema-2/#element-union">
          memberTypes attribute must be non-empty or there must be
          at least one simpleType child
        </xs:documentation>
      </xs:annotation>
      <xs:complexContent>
        <xs:extension base="xs:annotated">
          <xs:sequence>
            <xs:element name="simpleType" type="xs:localSimpleType"
                minOccurs="0" maxOccurs="unbounded"/>
          </xs:sequence>
          <xs:attribute name="memberTypes" use="optional">
            <xs:simpleType>
              <xs:list itemType="xs:QName"/>
            </xs:simpleType>
          </xs:attribute>
        </xs:extension>
      </xs:complexContent>
    </xs:complexType>
  </xs:element>

  <xs:complexType name="facet">
    <xs:complexContent>
      <xs:extension base="xs:annotated">
        <xs:attribute name="value" use="required"/>
        <xs:attribute name="fixed" type="xs:boolean" use="optional"
                      default="false"/>
      </xs:extension>
    </xs:complexContent>
  </xs:complexType>

 <xs:complexType name="noFixedFacet">
  <xs:complexContent>
   <xs:restriction base="xs:facet">
    <xs:sequence>
     <xs:element ref="xs:annotation" minOccurs="0"/>
    </xs:sequence>
    <xs:attribute name="fixed" use="prohibited"/>
    <xs:anyAttribute namespace="##other" processContents="lax"/>
   </xs:restriction>
  </xs:complexContent>
 </xs:complexType>

  <xs:element name="minExclusive" id="minExclusive" type="xs:facet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-minExclusive"/>
    </xs:annotation>
  </xs:element>
  <xs:element name="minInclusive" id="minInclusive" type="xs:facet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-minInclusive"/>
    </xs:annotation>
  </xs:element>

  <xs:element name="maxExclusive" id="maxExclusive" type="xs:facet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-maxExclusive"/>
    </xs:annotation>
  </xs:element>
  <xs:element name="maxInclusive" id="maxInclusive" type="xs:facet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-maxInclusive"/>
    </xs:annotation>
  </xs:element>

  <xs:complexType name="numFacet">
    <xs:complexContent>
      <xs:restriction base="xs:facet">
       <xs:sequence>
         <xs:element ref="xs:annotation" minOccurs="0"/>
       </xs:sequence>
       <xs:attribute name="value" type="xs:nonNegativeInteger" use="required"/>
       <xs:anyAttribute namespace="##other" processContents="lax"/>
      </xs:restriction>
    </xs:complexContent>
  </xs:complexType>

  <xs:element name="totalDigits" id="totalDigits">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-totalDigits"/>
    </xs:annotation>
    <xs:complexType>
      <xs:complexContent>
        <xs:restriction base="xs:numFacet">
          <xs:sequence>
            <xs:element ref="xs:annotation" minOccurs="0"/>
          </xs:sequence>
          <xs:attribute name="value" type="xs:positiveInteger" use="required"/>
         <xs:anyAttribute namespace="##other" processContents="lax"/>
        </xs:restriction>
      </xs:complexContent>
    </xs:complexType>
  </xs:element>
  <xs:element name="fractionDigits" id="fractionDigits" type="xs:numFacet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-fractionDigits"/>
    </xs:annotation>
  </xs:element>

  <xs:element name="length" id="length" type="xs:numFacet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-length"/>
    </xs:annotation>
  </xs:element>
  <xs:element name="minLength" id="minLength" type="xs:numFacet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-minLength"/>
    </xs:annotation>
  </xs:element>
  <xs:element name="maxLength" id="maxLength" type="xs:numFacet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-maxLength"/>
    </xs:annotation>
  </xs:element>

  <xs:element name="enumeration" id="enumeration" type="xs:noFixedFacet">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-enumeration"/>
    </xs:annotation>
  </xs:element>

  <xs:element name="whiteSpace" id="whiteSpace">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-whiteSpace"/>
    </xs:annotation>
    <xs:complexType>
      <xs:complexContent>
        <xs:restriction base="xs:facet">
          <xs:sequence>
            <xs:element ref="xs:annotation" minOccurs="0"/>
          </xs:sequence>
          <xs:attribute name="value" use="required">
            <xs:simpleType>
              <xs:restriction base="xs:NMTOKEN">
                <xs:enumeration value="preserve"/>
                <xs:enumeration value="replace"/>
                <xs:enumeration value="collapse"/>
              </xs:restriction>
            </xs:simpleType>
          </xs:attribute>
         <xs:anyAttribute namespace="##other" processContents="lax"/>
        </xs:restriction>
      </xs:complexContent>
    </xs:complexType>
  </xs:element>

  <xs:element name="pattern" id="pattern">
    <xs:annotation>
      <xs:documentation
        source="http://www.w3.org/TR/xmlschema-2/#element-pattern"/>
    </xs:annotation>
    <xs:complexType>
      <xs:complexContent>
        <xs:restriction base="xs:noFixedFacet">
          <xs:sequence>
            <xs:element ref="xs:annotation" minOccurs="0"/>
          </xs:sequence>
          <xs:attribute name="value" type="xs:string" use="required"/>
         <xs:anyAttribute namespace="##other" processContents="lax"/>
        </xs:restriction>
      </xs:complexContent>
    </xs:complexType>
  </xs:element>

</xs:schema>
