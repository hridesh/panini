package org.paninij.soter;

import org.paninij.soter.util.WalaUtil;

import com.ibm.wala.analysis.pointers.BasicHeapGraph;
import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;


/**
 * Builds Zero-One CFA call graph using flow insensitive Andersen style points-to analysis with
 * entrypoints stemming from the procedures of a single template class.
 */
public class CallGraphBuilder
{
    protected AnalysisCache analysisCache;
    
    // Artifacts generated by performing the analysis:
    protected CallGraph callGraph;
    protected PointerAnalysis<InstanceKey> pointerAnalysis;
    protected HeapModel heapModel;
    protected HeapGraph<InstanceKey> heapGraph;


    public CallGraphBuilder() {
        this(new AnalysisCache());
    }
    
    
    public CallGraphBuilder(AnalysisCache analysisCache) {
        this.analysisCache = analysisCache;
    }
 

    /**
     * This performs a zero-one CFA algorithm which simultaneously builds the call graph and
     * performs the pointer analysis. It initializes all of the following fields:
     * 
     *  - callGraph
     *  - pointerAnalysis
     *  - heapModel
     *  - heapGraph
     *  
     * Note that by calling this function, the entrypoints stored in `options` will be overridden.
     */
    @SuppressWarnings("unchecked")
    public void buildCallGraph(String templateName, IClassHierarchy cha, AnalysisOptions options)
    {
        IClass template = WalaUtil.loadTemplateClass(templateName, cha);
        options.setEntrypoints(CapsuleTemplateEntrypoint.makeAll(template));

        ContextSelector contextSelector = new ReceiverInstanceContextSelector();
        PropagationCallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, analysisCache,
                                                                         cha, cha.getScope(),
                                                                         contextSelector, null);
        try
        {
            callGraph = builder.makeCallGraph(options, null);
            pointerAnalysis = builder.getPointerAnalysis();
            heapModel = pointerAnalysis.getHeapModel();
            heapGraph = new BasicHeapGraph(pointerAnalysis, callGraph);
        }
        catch (CallGraphBuilderCancelException ex)
        {
            String msg = "Call graph construction for was unexpectedly cancelled: " + templateName;
            throw new IllegalArgumentException(msg);
        }
    }

    
    public CallGraph getCallGraph()
    {
        if (callGraph == null)
        {
            String msg = "Must call `perform()` before `getCallGraph()`.";
            throw new IllegalStateException(msg);
        }

        return callGraph;
    }


    public PointerAnalysis<InstanceKey> getPointerAnalysis()
    {
        if (pointerAnalysis == null)
        {
            String msg = "Must call `perform()` before `getCallGraph()`.";
            throw new IllegalStateException(msg);
        }
        return pointerAnalysis;
    }
    
    
    public HeapGraph<InstanceKey> getHeapGraph()
    {
        if (heapGraph == null)
        {
            String msg = "Must call `perform()` before `getHeapGraph()`.";
            throw new IllegalStateException(msg);
        }
        return heapGraph;
    }
    
    
    /**
     * A helper method for making a call graph builder and performing the build in the default way.
     * This is useful for building a single call for a template. However, if call graphs for
     * multiple templates are needed, it is recommended (for performance reasons) to instantiate
     * a `PaniniCallGraphBuilder` and then call `buildCallGraph()` separately for each template.
     * 
     * @param templateName  The name of the template to be analyzed. Should be something of the form
     *                      `-Lorg/paninij/soter/FooTemplate`.
     * @param classPath     A colon-separated list of file system locations in which WALA should
     *                      look for application classes.
     */
    public static CallGraphBuilder build(String templateName, String classPath)
    {
        IClassHierarchy cha = WalaUtil.makeClassHierarchy(classPath);
        AnalysisOptions options = WalaUtil.makeAnalysisOptions(cha);

        CallGraphBuilder builder = new CallGraphBuilder();
        builder.buildCallGraph(templateName, cha, options);
        return builder;
    }
}