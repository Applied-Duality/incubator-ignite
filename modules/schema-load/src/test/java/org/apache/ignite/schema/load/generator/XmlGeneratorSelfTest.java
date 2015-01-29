/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.schema.load.generator;

import org.apache.ignite.cache.query.*;
import org.apache.ignite.schema.generator.*;
import org.apache.ignite.schema.load.*;
import org.apache.ignite.schema.model.*;

import java.io.*;
import java.util.*;

/**
 * Tests for XML generator.
 */
public class XmlGeneratorSelfTest extends BaseSchemaLoaderSelfTest {
    /**
     * Test that XML generated correctly.
     */
    public void testXmlGeneration() throws Exception {
        Collection<CacheQueryTypeMetadata> all = new ArrayList<>();

        // Generate XML.
        for (PojoDescriptor pojo : pojoLst)
            if (!pojo.valueClassName().isEmpty())
                all.add(pojo.metadata(true));

        String fileName = "Ignite.xml";

        XmlGenerator.generate("org.apache.ignite.schema.load.model", all, new File(OUT_DIR_PATH, fileName),
            askOverwrite);

        assertTrue("Generated XML file content is differ from expected one",
            compareFilesInt(getClass().getResourceAsStream("/org/apache/ignite/schema/load/model/" + fileName),
                new File(OUT_DIR_PATH + "/" + fileName), "XML generated by Apache Ignite Schema Load utility"));
    }
}