/* /nodynamiccopyright/ */
// combinations of methods defined in a base class
// and overridden in subtypes

// class should compile with warnings as shown

class R extends Q {
    @Deprecated public void pDep_qDep_rDep() { }
                public void pDep_qDep_rUnd() { } // warn
    //          public void pDep_qDep_rInh() { }
    @Deprecated public void pDep_qUnd_rDep() { }
                public void pDep_qUnd_rUnd() { }
    //          public void pDep_qUnd_rInh() { }
    @Deprecated public void pDep_qInh_rDep() { }
                public void pDep_qInh_rUnd() { } // warn
    //          public void pDep_qInh_rInh() { }
    @Deprecated public void pUnd_qDep_rDep() { }
                public void pUnd_qDep_rUnd() { } // warn
    //          public void pUnd_qDep_rInh() { }
    @Deprecated public void pUnd_qUnd_rDep() { }
                public void pUnd_qUnd_rUnd() { }
    //          public void pUnd_qUnd_rInh() { }
    @Deprecated public void pUnd_qInh_rDep() { }
                public void pUnd_qInh_rUnd() { }
    //          public void pUnd_qInh_rInh() { }
}
