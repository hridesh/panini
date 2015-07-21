package org.paninij.apt.soter;

import static org.paninij.apt.util.ArtifactCompiler.buildEffectiveClassPath;
import static org.paninij.soter.cga.CallGraphAnalysisFactory.performStandardAnalysis;

import java.util.Collection;

import org.junit.Test;
import org.paninij.runtime.util.IdentitySet;
import org.paninij.soter.cga.CallGraphAnalysis;
import org.paninij.soter.util.SoterUtil;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;

/**
 * This includes tests used to drive experiments related to WALA and the SOTER analysis.
 */
public class Scratch
{
    private static final String CLASSPATH_FILE = "target/generated-resources/maven/panini_processor_classpath.txt";
    private static final String CLASSPATH = buildEffectiveClassPath("target/test-classes", CLASSPATH_FILE);

    private static final String ACTIVE_CLIENT_NAME = "Lorg/paninij/soter/tests/ActiveClientTemplate";
    
    @Test
    public void scratch()
    {
        CallGraphAnalysis analysis = performStandardAnalysis(ACTIVE_CLIENT_NAME, CLASSPATH);
        CallGraph cg = analysis.getCallGraph();
        IdentitySet<CGNode> init = SoterUtil.getInitialCallNodes(cg);
        IdentitySet<CGNode> reachable = SoterUtil.makeCalledByClosure(init, cg);

        System.out.println("[`init` nodes]");
        for (CGNode n : init) {
            System.out.println(n);
        }

        System.out.println("[`reachable` nodes]");
        for (CGNode n : reachable) {
            System.out.println(n);
        }
    }
}