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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.paninij.apt.util.MessageShape;
import org.paninij.apt.util.Source;
import org.paninij.apt.util.SourceFile;
import org.paninij.model.Procedure;
import org.paninij.model.Signature;

public class SignatureFactory
{
    private Signature context;

    public SourceFile make(Signature signature) {
        // TODO actually generate the source file

        this.context = signature;

        System.out.println("GENERATE SIGNATURE: " + signature.getQualifiedName());
        for (Procedure procedure : signature.getProcedures()) {
            System.out.println("GENERATE PROC: " + procedure.toString());
        }

        String name = this.context.getQualifiedName();
        String content = this.generateContent();
        return new SourceFile(name, content);
    }

    public String generateContent() {
        String src = Source.cat(
                "package #0;",
                "",
                "##",
                "",
                "public interface #1",
                "{",
                "",
                "}");
        src = Source.format(src,
                this.context.getPackage(),
                this.context.getSimpleName());
        src = Source.formatAligned(src, this.generateImports());

        return src;
    }

    public List<String> generateImports() {
        Set<String> imports = new HashSet<String>();

        for (Procedure p : this.context.getProcedures()) {
            MessageShape shape = new MessageShape(p);
            imports.add(shape.getPackage() + "." +shape.encoded);
        }

        imports.addAll(this.context.getImports());

        List<String> prefixedImports = new ArrayList<String>();

        for (String i : imports) {
            prefixedImports.add("import " + i + ";");
        }

        return prefixedImports;
    }
}
