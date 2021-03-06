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
package org.apache.juneau.rest.annotation;

import org.apache.juneau.rest.*;

/**
 * Extended annotation for {@link RestResource#swagger() @RestResource.swagger()}.
 */
public @interface ResourceSwagger {
	/**
	 * Optional servlet terms-of-service for this API.
	 * <p>
	 * It is used to populate the Swagger terms-of-service field.
	 * <p>
	 * The default value pulls the description from the <code>termsOfService</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"termsOfService = foo"</js> or <js>"MyServlet.termsOfService = foo"</js>).
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/termsOfService</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getTermsOfService(RestRequest)} method.
	 */
	String termsOfService() default "";

	/**
	 * Optional contact information for the exposed API.
	 * <p>
	 * It is used to populate the Swagger contact field and to display on HTML pages.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		name: string,
	 * 		url: string,
	 * 		email: string
	 * 	}
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>contact</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"contact = {name:'John Smith',email:'john.smith@foo.bar'}"</js> or <js>"MyServlet.contact = {name:'John Smith',email:'john.smith@foo.bar'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			contact=<js>"{name:'John Smith',email:'john.smith@foo.bar'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/contact</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getContact(RestRequest)} method.
	 */
	String contact() default "";

	/**
	 * Optional license information for the exposed API.
	 * <p>
	 * It is used to populate the Swagger license field and to display on HTML pages.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		name: string,
	 * 		url: string
	 * 	}
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>license</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"license = {name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js> or <js>"MyServlet.license = {name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			license=<js>"{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/license</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getLicense(RestRequest)} method.
	 */
	String license() default "";

	/**
	 * Provides the version of the application API (not to be confused with the specification version).
	 * <p>
	 * It is used to populate the Swagger version field and to display on HTML pages.
	 * <p>
	 * The default value pulls the description from the <code>version</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"version = 2.0"</js> or <js>"MyServlet.version = 2.0"</js>).
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/version</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getVersion(RestRequest)} method.
	 */
	String version() default "";

	/**
	 * Optional tagging information for the exposed API.
	 * <p>
	 * It is used to populate the Swagger tags field and to display on HTML pages.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	[
	 * 		{
	 * 			name: string,
	 * 			description: string,
	 * 			externalDocs: {
	 * 				description: string,
	 * 				url: string
	 * 			}
	 * 		}
	 * 	]
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>tags</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"tags = [{name:'Foo',description:'Foobar'}]"</js> or <js>"MyServlet.tags = [{name:'Foo',description:'Foobar'}]"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			tags=<js>"[{name:'Foo',description:'Foobar'}]"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/tags</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getTags(RestRequest)} method.
	 */
	String tags() default "";

	/**
	 * Optional external documentation information for the exposed API.
	 * <p>
	 * It is used to populate the Swagger external documentation field and to display on HTML pages.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		description: string,
	 * 		url: string
	 * 	}
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>externalDocs</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"externalDocs = {url:'http://juneau.apache.org'}"</js> or <js>"MyServlet.externalDocs = {url:'http://juneau.apache.org'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			externalDocs=<js>"{url:'http://juneau.apache.org'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/tags</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getExternalDocs(RestRequest)} method.
	 */
	String externalDocs() default "";


}
