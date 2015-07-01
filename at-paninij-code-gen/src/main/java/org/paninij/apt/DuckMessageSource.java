/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * For more details and the latest version of this code please see
 * http://paninij.org
 *
 * Contributor(s): Dalton Mills
 */
package org.paninij.apt;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.paninij.apt.util.JavaModelInfo;
import org.paninij.apt.util.MessageShape;
import org.paninij.apt.util.Source;
import org.paninij.apt.util.SourceFile;
import org.paninij.model.Procedure;
import org.paninij.model.Variable;
import org.paninij.model.Type.Category;

public class DuckMessageSource extends MessageSource
{
    public DuckMessageSource() {
        this.context = null;
    }

    @Override
    protected SourceFile generate(Procedure procedure) {
        this.context = procedure;
        this.shape = new MessageShape(procedure);
        String name = this.buildQualifiedClassName();
        String content;
        if (procedure.getReturnType().isInterface()) {
            content = this.generateImplContent();
        } else {
            content = this.generateContent();
        }
        return new SourceFile(name, content);
    }

    protected String generateImplContent() {
        String src = Source.cat(
                "package #0;",
                "",
                "##",
                "",
                "@SuppressWarnings(\"all\")",  // Suppress unused imports.
                "public class #1 implements #2, Panini$Message, Panini$Future<#2>",
                "{",
                "    public final int panini$procID;",
                "    private #2 panini$result = null;",
                "    boolean panini$isResolved = false;",
                "",
                "    ##",
                "",
                "    ##",
                "",
                "    @Override",
                "    public int panini$msgID() {",
                "        return panini$procID;",
                "    }",
                "",
                "    @Override",
                "    public void panini$resolve(#2 result) {",
                "        synchronized (this) {",
                "            panini$result = result;",
                "            panini$isResolved = true;",
                "            this.notifyAll();",
                "        }",
                "        ##",
                "    }",
                "",
                "    @Override",
                "    public #2 panini$get() {",
                "        while (panini$isResolved == false) {",
                "            try {",
                "                synchronized (this) {",
                "                    while (panini$isResolved == false) this.wait();",
                "                }",
                "            } catch (InterruptedException e) { /* try waiting again */ }",
                "         }",
                "         return panini$result;",
                "    }",
                "",
                "    /* The following implement the methods of `#2` */",
                "    ##",
                "}");

        src = Source.format(src, this.shape.getPackage(),
                                 this.shape.encoded,
                                 this.shape.returnType.wrapped());

        src = Source.formatAligned(src, this.buildImports());
        src = Source.formatAligned(src, this.buildParameterFields());
        src = Source.formatAligned(src, this.buildConstructor());
        src = Source.formatAligned(src, this.buildReleaseArgs());
        src = Source.formatAligned(src, this.buildFacades());

        return src;
    }

    @Override
    protected String generateContent() {
        String src = Source.cat(
                "package #0;",
                "",
                "##",
                "",
                "@SuppressWarnings(\"all\")",  // Suppress unused imports.
                "public class #1 extends #2 implements Panini$Message, Panini$Future<#2>",
                "{",
                "    public final int panini$procID;",
                "    private #2 panini$result = null;",
                "    boolean panini$isResolved = false;",
                "",
                "    ##",
                "",
                "    ##",
                "",
                "    @Override",
                "    public int panini$msgID() {",
                "        return panini$procID;",
                "    }",
                "",
                "    @Override",
                "    public void panini$resolve(#2 result) {",
                "        synchronized (this) {",
                "            panini$result = result;",
                "            panini$isResolved = true;",
                "            this.notifyAll();",
                "        }",
                "        ##",
                "    }",
                "",
                "    @Override",
                "    public #2 panini$get() {",
                "        while (panini$isResolved == false) {",
                "            try {",
                "                synchronized (this) {",
                "                    while (panini$isResolved == false) this.wait();",
                "                }",
                "            } catch (InterruptedException e) { /* try waiting again */ }",
                "         }",
                "         return panini$result;",
                "    }",
                "",
                "    /* The following override the methods of `#2` */",
                "    ##",
                "}");

        src = Source.format(src, this.shape.getPackage(),
                                 this.shape.encoded,
                                 this.shape.returnType.wrapped());

        src = Source.formatAligned(src, this.buildImports());
        src = Source.formatAligned(src, this.buildParameterFields());
        src = Source.formatAligned(src, this.buildConstructor());
        src = Source.formatAligned(src, this.buildReleaseArgs());
        src = Source.formatAligned(src, this.buildFacades());

        return src;
    }

    protected List<String> buildReleaseArgs() {
        List<String> statements=  new ArrayList<String>();
        int i = 0;
        for (Variable v : context.getParameters()) {
            if (v.getCategory() == Category.NORMAL) statements.add("panini$arg" + (i++) + " = null;");
        }
        return statements;
    }

    protected List<String> buildFacades() {
        List<String> facades =  new ArrayList<String>();

        DeclaredType returnType = (DeclaredType) this.shape.returnType.getMirror();
        for (Element el : returnType.asElement().getEnclosedElements()) {
            if (el.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) el;
                if (this.canMakeFacade(method)) {
                    facades.addAll(this.buildFacade(method));
                    facades.add("");
                }
            }
        }

        return facades;
    }

    private List<String> buildFacade(ExecutableElement method) {
        List<String> fmt = Source.lines("@Override",
                this.buildFacadeDecl(method),
                "{",
                "    #0",
                "}");
        return Source.formatAll(fmt, this.buildFacadeBody(method));
    }

    private String buildFacadeDecl(ExecutableElement method) {

        List<String> modifiers = new ArrayList<String>();
        for (Modifier m : method.getModifiers()) {
            if (m != Modifier.ABSTRACT) {
                modifiers.add(m.toString());
            }
        }
        String mod = String.join(" ", modifiers);

        String decl = Source.format("#0 #1 #2(#3)",
                mod,
                method.getReturnType(),
                method.getSimpleName(),
                Source.buildParametersList(method));

        List<String> thrown = new ArrayList<String>();
        for (TypeMirror type : method.getThrownTypes()) {
            System.out.println("throws");
            thrown.add(type.toString());
        }
        return (thrown.isEmpty()) ? decl : decl + " throws " + String.join(", ", thrown);
    }

    private String buildFacadeBody(ExecutableElement method) {
        String fmt;
        if (JavaModelInfo.hasVoidReturnType(method)) {
            fmt = "panini$get().#0(#1);";
        } else {
            fmt = "return panini$get().#0(#1);";
        }
        return Source.format(fmt, method.getSimpleName(), Source.buildParameterNamesList(method));
    }

    private boolean canMakeFacade(ExecutableElement method)
    {
        // Some methods do not need to have a facade made for them
        // e.g. native methods, final methods
        String modifiers = Source.buildModifiersList(method);
        if (modifiers.contains("native"))
        {
            return false;
        }
        if (modifiers.contains("final"))
        {
            return false;
        }
        if (modifiers.contains("protected"))
        {
            return false;
        }
        if (modifiers.contains("private"))
        {
            return false;
        }
        if (modifiers.contains("private"))
        {
            return false;
        }
        if (modifiers.contains("static"))
        {
            return false;
        }
        return true;
    }
}
