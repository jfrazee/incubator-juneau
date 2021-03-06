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

import java.net.*;
import java.util.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;

/**
 * Utility class for starting up the examples microservice.
 * <p>
 * This class is NOT thread safe.
 */
public class SamplesMicroservice {
	static RestMicroservice microservice;
	static URI microserviceURI;

	// Reusable HTTP clients that get created and shut down with the microservice.
	public static RestClient DEFAULT_CLIENT;
	public static RestClient DEFAULT_CLIENT_PLAINTEXT;

	/**
	 * Starts the microservice.
	 * @return <jk>true</jk> if the service started, <jk>false</jk> if it's already started.
	 * If this returns <jk>false</jk> then don't call stopMicroservice()!.
	 */
	public static boolean startMicroservice() {
		if (microservice != null)
			return false;
		try {
			Locale.setDefault(Locale.US);
			microservice = new RestMicroservice().setConfig("examples.cfg", false);
			microserviceURI = microservice.start().getURI();
			DEFAULT_CLIENT = client().build();
			DEFAULT_CLIENT_PLAINTEXT = client(PlainTextSerializer.class, PlainTextParser.class).build();
			return true;
		} catch (Throwable e) {
			// Probably already started.
			e.printStackTrace();
			System.err.println(e); // NOT DEBUG
			return false;
		}
	}

	/**
	 * Returns the URI of the microservice.
	 * @return The URI of the microservice.
	 */
	public static URI getURI() {
		if (microservice == null)
			startMicroservice();
		return microserviceURI;
	}
	
	/**
	 * Stops the microservice.
	 */
	public static void stopMicroservice() {
		try {
			microservice.stop();
			microservice = null;
			DEFAULT_CLIENT.closeQuietly();
			DEFAULT_CLIENT_PLAINTEXT.closeQuietly();
		} catch (Exception e) {
			System.err.println(e); // NOT DEBUG
		}
	}
	
	/**
	 * Create a new HTTP client.
	 */
	public static RestClientBuilder client() {
		try {
			return new RestClientBuilder()
				.rootUrl(microserviceURI)
			;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a new HTTP client using the specified serializer and parser.
	 */
	public static RestClientBuilder client(Serializer s, Parser p) {
		return client().serializer(s).parser(p);
	}

	/**
	 * Create a new HTTP client using the specified serializer and parser.
	 */
	public static RestClientBuilder client(Class<? extends Serializer> s, Class<? extends Parser> p) {
		return client().serializer(s).parser(p);
	}
}
