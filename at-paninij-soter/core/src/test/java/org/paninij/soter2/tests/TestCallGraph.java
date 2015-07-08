package org.paninij.soter2.tests;

import java.util.function.Consumer;

import org.junit.Test;
import org.paninij.soter.util.WalaDebug;
import org.paninij.soter2.NoisyPaniniZeroOneCFA;
import org.paninij.soter2.PaniniZeroOneCFA;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;


public class TestCallGraph
{
    private static final String CLASSPATH = "lib/at-paninij-soter-tests.jar:lib/at-paninij-runtime.jar"; 
    
    @Test
    public void testCallGraphWithLeakyServer() throws Throwable {
        makeCallGraph("Lorg/paninij/soter/tests/LeakyServerTemplate", CLASSPATH,
                      "logs/LeakyServerCallGraph.pdf", "logs/LeakyServerHeapGraph.pdf");
    }
    
    @Test
    public void testCallGraphWithActiveClient() throws Throwable {
        makeCallGraph("Lorg/paninij/soter/tests/ActiveClientTemplate", CLASSPATH,
                      "logs/ActiveClientCallGraph.pdf", "logs/ActiveClientHeapGraph.pdf");
    }

    private void makeCallGraph(String template, String classPath,
                               String callGraphPDF, String heapGraphPDF) throws Throwable
    {
        PaniniZeroOneCFA cfa = NoisyPaniniZeroOneCFA.make(template, classPath);
        cfa.perform();

        Consumer<CallGraph> makeCallGraph = (cg -> WalaDebug.makeGraphFile(cg, callGraphPDF));
        Consumer<HeapGraph<InstanceKey>> makeHeapGraph = (hg -> WalaDebug.makeGraphFile(hg, heapGraphPDF));

        cfa.acceptUponCallGraph(makeCallGraph);
        cfa.acceptUponHeapGraph(makeHeapGraph);
    }
}
