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
 * Contributor(s): Dalton Mills, David Johnston
 */
package org.paninij.apt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.paninij.apt.util.MessageShape;
import org.paninij.apt.util.Source;
import org.paninij.apt.util.SourceFile;
import org.paninij.model.Capsule;
import org.paninij.model.Procedure;

public class CapsuleTestFactory
{
    public static final String CAPSULE_TEST_SUFFIX = "Test";

    private Capsule context;

    public SourceFile make(Capsule capsule) {
        this.context = capsule;

        String name = this.generateFileName();
        String content = this.generateContent();

        return new SourceFile(name, content);
    }

    private String generateFileName() {
        return this.context.getQualifiedName() + CAPSULE_TEST_SUFFIX;
    }

    private String generateContent() {
        String src = Source.cat(
                "package #0;",
                "",
                "##",
                "",
                "public class #1",
                "{",
                "    ##",
                "}");

        src = Source.format(src,
                this.context.getPackage(),
                this.generateClassName());
        src = Source.formatAligned(src, generateImports());
        src = Source.formatAligned(src, generateTests());

        return src;
    }

    private String generateClassName() {
        return this.context.getSimpleName() + CAPSULE_TEST_SUFFIX;
    }

    private List<String> generateImports() {
        Set<String> imports = new HashSet<String>();

        for (Procedure p : this.context.getProcedures()) {
            MessageShape shape = new MessageShape(p);
            imports.add(shape.getPackage() + "." +shape.encoded);
        }

        imports.addAll(this.context.getImports());

        imports.add("java.util.concurrent.TimeUnit");
        imports.add("org.junit.Test");
        imports.add("org.paninij.runtime.Capsule$Thread");
        imports.add("org.paninij.runtime.SimpleMessage");
        imports.add("org.paninij.runtime.Panini$Message");
        imports.add(this.context.getQualifiedName());

        List<String> prefixedImports = new ArrayList<String>();

        for (String i : imports) {
            prefixedImports.add("import " + i + ";");
        }

        return prefixedImports;
    }

    private List<String> generateTests() {
        List<String> src = Source.lines();
        int testId = 0;
        for (Procedure procedure : this.context.getProcedures()) {
            src.addAll(this.generateTest(procedure, testId++));
            src.add("");
        }
        return src;
    }

    private List<String> generateTest(Procedure procedure, int testId) {
        List<String> src = Source.lines(
                "@Test",
                "public void #0() throws Throwable {",
                "    Panini$Message test_msg = new SimpleMessage(#1);",
                "    Panini$Message exit_msg = new SimpleMessage(Capsule$Thread.PANINI$TERMINATE);",
                "",
                "    #2$Thread capsule = new #2$Thread();",
                "    capsule.panini$push(test_msg);",
                "    capsule.panini$push(exit_msg);",
                "    capsule.run();",
                "",
                "    // Re-throw any errors that were caught by `capsule`.",
                "    Throwable thrown = capsule.panini$pollErrors(1, TimeUnit.SECONDS);",
                "    if (thrown != null) {",
                "        throw thrown;",
                "    }",
                "}");
        return Source.formatAll(src, procedure.getName(), testId, this.context.getSimpleName());
    }

}
