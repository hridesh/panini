package org.paninij.apt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import org.paninij.apt.util.DuckShape;
import org.paninij.apt.util.ModelInfo;
import org.paninij.apt.util.Source;

public abstract class MakeDuck
{

    final String packageName = "org.paninij.runtime.ducks";
    PaniniPress context;

    public static MakeDuck make(PaniniPress context)
    {
        throw new UnsupportedOperationException("Cannot instantiate an abstract class.");
    }

    String buildDuck(DuckShape currentDuck)
    {
        switch (currentDuck.category)
        {
        case NORMAL:
            return buildNormalDuck(currentDuck);
        case VOID:
            return buildVoidDuck(currentDuck);
        default:
            throw new UnsupportedOperationException("Duck category not supported.");
        }
    }

    abstract String buildClassName(DuckShape currentDuck);

    abstract String buildQualifiedClassName(DuckShape currentDuck);

    String buildParameterImports(DuckShape currentDuck)
    {
        String importsStr = "";
        Set<String> uniqueParams = new HashSet<String>(currentDuck.parameters);
        for (String param : uniqueParams)
        {
            importsStr += "import " + param + ";\n";
        }
        return importsStr;
    }

    String buildParameterFields(DuckShape currentDuck)
    {
        String fieldsStr = "";
        for (int i = 0; i < currentDuck.parameters.size(); i++)
        {
            fieldsStr += "    public "
                    + Source.dropPackageName(currentDuck.parameters.get(i)) + " panini$arg" + i
                    + ";\n";
        }
        return fieldsStr;
    }

    String buildFacades(DuckShape currentDuck)
    {
        String facades = "";

        for (Element el : currentDuck.returnType.getEnclosedElements())
        {
            if (el.getKind() == ElementKind.METHOD)
            {
                ExecutableElement method = (ExecutableElement) el;
                if (this.canMakeFacade(method))
                {
                    facades += buildFacade(method);
                }

            }
        }

        return facades;
    }

    String buildFacade(ExecutableElement method)
    {
        String fmt = Source.lines(1, 
                "", 
                "@Override", 
                "#0 #1 #2(#3) {", 
                "    #4", 
                "}");
        return Source.format(fmt, Source.buildModifierString(method),
                Source.dropPackageName(method.getReturnType().toString()), 
                method.getSimpleName(),
                Source.buildParameterList(method), 
                buildFacadeBody(method));

    }

    String buildFacadeBody(ExecutableElement method)
    {
        String fmt;
        if (ModelInfo.hasVoidReturnType(method))
        {
            fmt = "panini$get().#0(#1);";
        }
        else
        {
            fmt = "return panini$get().#0(#1);";
        }
        return Source.format(fmt, method.getSimpleName(), Source.buildArgsList(method));
    }

    boolean canMakeFacade(ExecutableElement method)
    {
        // Some methods do not need to have a facade made for them
        // e.g. native methods, final methods

        /*
         * if (Source.buildModifierString(method).matches("(native|final)")) {
         * return false; }
         */

        if (Source.buildModifierString(method).contains("native"))
        {
            return false;
        }
        if (Source.buildModifierString(method).contains("final"))
        {
            return false;
        }
        if (Source.buildModifierString(method).contains("protected"))
        {
            return false;
        }
        if (Source.buildModifierString(method).contains("private"))
        {
            return false;
        }
        return true;
    }
    
    String buildReleaseArgs(DuckShape currentDuck) {
        
        String args = "";
        for(int i = 0; i < currentDuck.parameters.size(); i++)
        {
            args += "        panini$arg" + i + " = null;";
        }
            
        return args;
    }

    abstract String buildConstructor(DuckShape currentDuck);

    abstract String buildConstructorDecl(DuckShape currentDuck);

    abstract String buildNormalDuck(DuckShape currentDuck);

    abstract String buildVoidDuck(DuckShape currentDuck);

    public abstract void makeSourceFile(DuckShape currentDuck);
}
