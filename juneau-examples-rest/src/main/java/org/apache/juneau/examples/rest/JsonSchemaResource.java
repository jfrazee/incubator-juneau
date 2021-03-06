// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.examples.rest;

import org.apache.juneau.dto.jsonschema.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.*;

/**
 * Sample resource that shows how to serialize JSON-Schema documents.
 */
@RestResource(
	path="/jsonSchema",
	messages="nls/JsonSchemaResource",
	title="Sample JSON-Schema document",
	description="Sample resource that shows how to generate JSON-Schema documents",
	htmldoc=@HtmlDoc(
		links="{up:'request:/..',options:'servlet:/?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/JsonSchemaResource.java'}",
		aside=""
			+ "<div style='min-width:200px' class='text'>"
			+ "	<p>Shows how to produce JSON-Schema documents in a variety of languages using the JSON-Schema DTOs.</p>"
			+ "	<p>$W{contentTypeLinksColumn}</p>"
			+ "</div>"
	),
	widgets={
		ContentTypeLinksColumnWidget.class
	}
)
public class JsonSchemaResource extends ResourceJena {
	private static final long serialVersionUID = 1L;

	private Schema schema;     // The schema document

	@Override /* Servlet */
	public void init() {

		try {
			schema = new Schema()
				.setId("http://example.com/sample-schema#")
				.setSchemaVersionUri("http://json-schema.org/draft-04/schema#")
				.setTitle("Example Schema")
				.setType(JsonType.OBJECT)
				.addProperties(
					new SchemaProperty("firstName", JsonType.STRING),
					new SchemaProperty("lastName", JsonType.STRING),
					new SchemaProperty("age", JsonType.INTEGER)
						.setDescription("Age in years")
						.setMinimum(0)
				)
				.addRequired("firstName", "lastName");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** GET request handler */
	@RestMethod(name="GET", path="/", summary="Get the JSON-Schema document")
	public Schema getSchema() throws Exception {
		return schema;
	}

	/**
	 * PUT request handler.
	 * Replaces the schema document with the specified content, and then mirrors it as the response.
	 */
	@RestMethod(name="PUT", path="/", summary="Overwrite the JSON-Schema document")
	public Schema setSchema(@Body Schema schema) throws Exception {
		this.schema = schema;
		return schema;
	}
}
