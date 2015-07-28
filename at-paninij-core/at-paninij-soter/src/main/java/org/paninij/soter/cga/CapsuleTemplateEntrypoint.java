package org.paninij.soter.cga;

import static org.paninij.soter.util.PaniniModel.getChildDecls;
import static org.paninij.soter.util.PaniniModel.getCapsuleMockupClassReference;
import static org.paninij.soter.util.PaniniModel.getProceduresList;
import static org.paninij.soter.util.PaniniModel.getRunDecl;
import static org.paninij.soter.util.PaniniModel.getStateDecls;
import static org.paninij.soter.util.PaniniModel.getWiredDecls;
import static org.paninij.soter.util.PaniniModel.isCapsuleTemplate;
import static org.paninij.soter.util.PaniniModel.isProcedure;
import static org.paninij.soter.util.SoterUtil.isKnownToBeEffectivelyImmutable;

import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.paninij.soter.util.PaniniModel;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;


public class CapsuleTemplateEntrypoint extends DefaultEntrypoint
{
    protected IClass template;

    /**
     * @param method Either a `run()` declaration or a procedure on a well-formed capsule template
     *               (i.e. a class annotated with `@Capsule` which passed all `@PaniniJ` checks).
     */
    public CapsuleTemplateEntrypoint(IMethod method)
    {
        super(method, method.getClassHierarchy());
        template = method.getDeclaringClass();
        assert isCapsuleTemplate(template);
    }


    /**
     * @param root  The fake root method to which the instantiated argument and any other required
     *              instantiations are being added.
     * @param i     The index of the argument being created, where 0 is the receiver object.
     * @return      The value number of the created argument instance; -1 if there was some error.
     */
    @Override
    protected int makeArgument(AbstractRootMethod root, int i)
    {
        return (i == 0) ? makeReceiver(root) : makeProcedureArgument(root, i);
    }
    
    
    /**
     * @see makeArgument
     */
    protected int makeReceiver(AbstractRootMethod root)
    {
        // Instantiate a capsule template instance to serve as this entrypoint's receiver object.
        // Note that every capsule template must (only) have the default constructor.
        TypeReference receiverType = method.getParameterType(0);
        SSANewInstruction receiverAllocation = root.addAllocation(receiverType);
        if (receiverAllocation == null)
            return -1;
        int receiverValueNumber = receiverAllocation.getDef();
        
        // Make a capsule mockup instance for each of the receiver's capsule fields (i.e. all of its
        // `@Child` and `@Wired` fields).
        Stream.concat(getChildDecls(template),
                      getWiredDecls(template))
              .forEach(f -> addWiredInstance(root, f, receiverValueNumber));
        
        // Transitively instantiated state variables.
        getStateDecls(template).forEach(f -> addStateInstance(root, f, receiverValueNumber));
        
        // Initialize the newly created receiver object.
        // TODO: Debug and re-enable this.
        //makeReceiverInitInvocation(root, receiverValueNumber);
        
        return receiverValueNumber;
    }
    
    
    /**
     * @see makeArgument
     */
    protected int makeProcedureArgument(AbstractRootMethod root, int i)
    {
        // This should not be used to make a capsule template receiver object.
        assert i > 0;

        // Note that if this is called `method` cannot be a run decl and must be a procedure, since
        // run decls have exactly one argument: the capsule template reciever object.
        assert isProcedure(method);

        // TODO: Everything! But for now just use the default behavior.
        return super.makeArgument(root, i);
    }
    
    
    /**
     * Makes an invocation instruction on template object with value number `i`, and adds this
     * instruction to the given fake root method.
     * 
     * @see makeArgument
     */
    protected void makeReceiverInitInvocation(AbstractRootMethod root, int i)
    {
        IMethod initMethod = PaniniModel.getInitDecl(template);
        if (initMethod != null)
        {
            CallSiteReference initCall = CallSiteReference.make(root.getStatements().length,
                                                                initMethod.getReference(),
                                                                IInvokeInstruction.Dispatch.STATIC);
            root.addInvocation(new int[] {i}, initCall);
        }
    }
    
    /**
     * Instantiates an `@Wired` annotated field. The behavior of this method will depend upon the
     * field's type. For example, if a the field is primitive, it will instantiate nothing; if it
     * is a capsule interface, it will delegate to `addCapsuleMockup()`.
     * 
     * @param root  The fake root method to which the instantiation instructions are being added.
     * @param field A field of 
     *              generated by `@PaniniJ` and is annotated with `@CapsuleInterface`).
     * @param receiverValueNumber Value number of the receiver instance whose field is being
     *                            instantiated with a capsule mockup class.
     */
    protected void addWiredInstance(AbstractRootMethod root, IField field, int receiverValueNumber)
    {
        TypeReference fieldTypeRef = field.getFieldTypeReference();

        if (fieldTypeRef.isPrimitiveType() || isKnownToBeEffectivelyImmutable(fieldTypeRef)) {
            return;  // No need to add instances for these types.
        }

        if (fieldTypeRef.isArrayType())
        {
            String msg = "TODO: SOTER cannot yet add instances for `@Wired` arrays.";
            throw new UnsupportedOperationException(msg);
        }

        IClass fieldType = getCha().lookupClass(fieldTypeRef);
        if (PaniniModel.isCapsuleInterface(fieldType)) {
            addCapsuleMockup(root, field, receiverValueNumber);
        }
    }
    
    
    /**
     * Instantiates a capsule mockup for this capsule template field.
     * 
     * @param root  The fake root method to which the instantiation instructions are being added.
     * @param field A field whose declaring class is a well-formed capsule interface (i.e. a class
     *              generated by `@PaniniJ` and is annotated with `@CapsuleInterface`).
     * @param receiverValueNumber Value number of the receiver instance whose field is being
     *                            instantiated with a capsule mockup class.
     */
    protected void addCapsuleMockup(AbstractRootMethod root, IField field, int receiverValueNumber)
    {
        TypeReference fieldTypeRef = field.getFieldTypeReference();

        TypeReference mockupType = getCapsuleMockupClassReference(fieldTypeRef);
        if (mockupType == null)
        {
            String msg = "Could not load the dummy class associated with " + fieldTypeRef;
            throw new IllegalArgumentException(msg);
        }

        // There's no need to call the dummy's constructor.
        SSANewInstruction dummyAlloc = root.addAllocationWithoutCtor(mockupType);
        if (dummyAlloc == null) {
            // This may happen if the dummy class could not be found.
            throw new RuntimeException("Failed to create an allocation for a dummy: " + mockupType);
        }
        int dummyValueNumber = dummyAlloc.getDef();
        root.addSetInstance(field.getReference(), receiverValueNumber, dummyValueNumber);
    }
    

    /**
     * Transitively instantiates the state associated with this capsule template field.
     * 
     * @param root            The fake root method to which the new instruction is being added.
     * @param field           A field whose declaring class is considered a "state" by `@PaniniJ`.
     * @param instValueNumber Value number of the object instance whose field is being instantiated
     */
    protected void addStateInstance(AbstractRootMethod root, IField field, int instValueNumber)
    {
        // TODO: Everything!
        // TODO: Consider somehow using `DefaultEntrypoint.makeArgument()` here.
        // TODO: This does not yet transitively (a.k.a. recursively) instantiate state.
        TypeReference fieldType = field.getFieldTypeReference();
        if (fieldType == null)
        {
            String msg = "Failed to look up a field's `TypeReference`: " + field;
            throw new RuntimeException(msg);
        }

        // No need to add instances for primitives or objects which known to be truly safe.
        if (fieldType.isPrimitiveType() || isKnownToBeEffectivelyImmutable(fieldType)) {
            return;
        }
        
        SSANewInstruction stateAlloc = root.addAllocation(fieldType);
        if (stateAlloc == null)
        {
            String msg = "While adding a state for the capsule template `{0}`, a 'new' instruction "
                       + "could not be added to the fake root for the field `{1}` whose "
                       + "`TypeReference` is `{2}`";
            throw new RuntimeException(MessageFormat.format(msg, template, field, fieldType));
        }

        int stateValueNumber = stateAlloc.getDef();
        root.addSetInstance(field.getReference(), instValueNumber, stateValueNumber);
    }
    
    
    /**
     * Returns a set of all of the entrypoints on the given capsule template.
     * 
     * @param template A well-formed capsule template class, annotated with `@Capsule`.
     */
    public static Set<Entrypoint> makeAll(IClass template)
    {
        assert isCapsuleTemplate(template);

        Set<Entrypoint> entrypoints = HashSetFactory.make();
        final Consumer<IMethod> addEntrypoint = (m -> entrypoints.add(new CapsuleTemplateEntrypoint(m)));

        // The way in which `entrypoints` is populated depends on whether the capsule template 
        // defines an active or passive capsule. If active, then the only entrypoint is `run()`.
        // If passive, then every procedure is an entrypoint.
        IMethod runDecl = getRunDecl(template);
        if (runDecl != null) {
            addEntrypoint.accept(runDecl);
        } else {
            getProceduresList(template).forEach(addEntrypoint);
        }
        return entrypoints;
    }
}
