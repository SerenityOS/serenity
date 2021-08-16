/*
 * @test /nodynamiccopyright/
 * @bug 8000484
 * @summary Bad error recovery when 'catch' without 'try' is found
 * @compile/fail/ref=T8000484.out -XDrawDiagnostics T8000484.java
 */

public class T8000484 {
    void m() {
        catch (Exception e){}
        else{}
        finally{}
        catch (Exception e) {catch (Exception e){}}
        else{else{}}
        finally{finally{}}
    }
}
