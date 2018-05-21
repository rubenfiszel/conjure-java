/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.conjure.java;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.conjure.java.services.JerseyServiceGenerator;
import com.palantir.conjure.spec.ConjureDefinition;
import com.palantir.remoting3.ext.jackson.ObjectMappers;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class JerseyServiceGeneratorTests extends TestBase {

    private static final ObjectMapper mapper = ObjectMappers.newServerObjectMapper();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static String compiledFileContent(File srcDir, String clazz) throws IOException {
        return new String(Files.readAllBytes(Paths.get(srcDir.getPath(), clazz)), StandardCharsets.UTF_8);
    }

    @Test
    public void testServiceGeneration_exampleService() throws IOException {
        testServiceGeneration("example-service");
    }

    @Test
    public void testServiceGeneration_cookieService() throws IOException {
        testServiceGeneration("cookie-service");
    }

    @Test
    public void testConjureImports() throws IOException {
        //        ConjureDefinition conjure = Conjure.parse(
        //                ImmutableList.of(
        //                        new File("src/test/resources/example-conjure-imports.yml"),
        //                        new File("src/test/resources/example-types.yml"),
        //                        new File("src/test/resources/example-service.yml")));
        //        mapper.writeValue(new File("src/test/resources/testConjureImports.json"), conjure);

        ConjureDefinition conjure = mapper.readValue(new File("src/test/resources/testConjureImports.json"),
                ConjureDefinition.class);

        File src = folder.newFolder("src");
        JerseyServiceGenerator generator = new JerseyServiceGenerator();
        generator.emit(conjure, src);

        // Generated files contain imports
        assertThat(compiledFileContent(src, "test/api/with/imports/ImportService.java"))
                .contains("import com.palantir.product.StringExample;");
    }

    @Test
    public void testBinaryReturnInputStream() throws IOException {
        //        ConjureDefinition def = Conjure.parse(
        //                ImmutableList.of(new File("src/test/resources/example-binary.yml")));
        //        mapper.writeValue(new File("src/test/resources/testBinaryReturnInputStream.json"), def);

        ConjureDefinition def = mapper.readValue(new File("src/test/resources/testBinaryReturnInputStream.json"),
                ConjureDefinition.class);

        List<Path> files = new JerseyServiceGenerator()
                .emit(def, folder.getRoot());

        for (Path file : files) {
            if (Boolean.valueOf(System.getProperty("recreate", "false"))) {
                Path output = Paths.get("src/test/resources/test/api/" + file.getFileName() + ".jersey.binary");
                Files.delete(output);
                Files.copy(file, output);
            }

            assertThat(readFromFile(file)).isEqualTo(
                    readFromFile(Paths.get("src/test/resources/test/api/" + file.getFileName() + ".jersey.binary")));
        }
    }

    private void testServiceGeneration(String conjureFile) throws IOException {
        //        ConjureDefinition def = Conjure.parse(
        //                ImmutableList.of(new File("src/test/resources/" + conjureFile + ".yml")));
        //        mapper.writeValue(new File("src/test/resources/"+conjureFile + ".json"), def);
        ConjureDefinition def = mapper.readValue(new File("src/test/resources/" + conjureFile + ".json"),
                ConjureDefinition.class);
        List<Path> files = new JerseyServiceGenerator().emit(def, folder.getRoot());

        for (Path file : files) {
            if (Boolean.valueOf(System.getProperty("recreate", "false"))) {
                Path output = Paths.get("src/test/resources/test/api/" + file.getFileName() + ".jersey");
                Files.delete(output);
                Files.copy(file, output);
            }

            assertThat(readFromFile(file)).isEqualTo(
                    readFromFile(Paths.get("src/test/resources/test/api/" + file.getFileName() + ".jersey")));
        }
    }

}