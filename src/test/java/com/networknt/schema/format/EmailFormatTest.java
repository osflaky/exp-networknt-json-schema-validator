/*
 * Copyright (c) 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.networknt.schema.format;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.serialization.JsonMapperFactory;

class EmailFormatTest {

    /** U+00A0 NON-BREAKING SPACE, built from its code point so no invisible character sits in the source. */
    private static final String NBSP = new String(Character.toChars(0x00A0));

    /**
     * Validates {@code email} against a {@code {"format": "email"}} schema with
     * format assertions turned on, and returns the validation errors. The email is
     * serialized via the mapper so a value containing quotes is escaped correctly.
     */
    private List<Error> validateEmail(String email) {
        String schemaData = "{\r\n"
                + "  \"format\": \"email\"\r\n"
                + "}";
        String inputData = JsonMapperFactory.getInstance().writeValueAsString(email);
        Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schemaData);
        return schema.validate(inputData, InputFormat.JSON,
                executionContext -> executionContext.executionConfig(executionConfig -> executionConfig.formatAssertionsEnabled(true)));
    }

    /** Sanity check that the test harness accepts a plainly valid email. */
    @Test
    void validEmailShouldPass() {
        List<Error> messages = validateEmail("name@email.com");
        assertTrue(messages.isEmpty(), "a plainly valid email should pass");
    }

    /**
     * A quoted local part with an ASCII space ({@code "joe bloggs"@example.com}) is a
     * valid email, so the non-ASCII whitespace guard must not reject it.
     */
    @Test
    void quotedLocalPartWithAsciiSpaceShouldPass() {
        List<Error> messages = validateEmail("\"joe bloggs\"@example.com");
        assertTrue(messages.isEmpty(), "a quoted local part with an ASCII space should stay valid");
    }

    /**
     * Reproduces issue #1164: a leading non-breaking space (U+00A0) should make
     * the email invalid, just like a regular leading space does, but it is
     * currently accepted because the validator only excludes ASCII whitespace.
     */
    @Test
    void emailWithLeadingNbspShouldFail() {
        List<Error> messages = validateEmail(NBSP + "name@email.com");
        assertFalse(messages.isEmpty(), "email with a leading non-breaking space (U+00A0) should be invalid");
    }

    /**
     * The fix generalizes to other non-ASCII whitespace such as U+2003 EM SPACE
     * and U+3000 IDEOGRAPHIC SPACE.
     */
    @ParameterizedTest
    @ValueSource(ints = { 0x2003, 0x3000 })
    void emailWithLeadingUnicodeWhitespaceShouldFail(int codePoint) {
        String whitespace = new String(Character.toChars(codePoint));
        List<Error> messages = validateEmail(whitespace + "name@email.com");
        assertFalse(messages.isEmpty(), "email with a leading non-ASCII whitespace character should be invalid");
    }
}
