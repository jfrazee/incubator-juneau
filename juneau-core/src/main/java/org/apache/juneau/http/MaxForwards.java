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
package org.apache.juneau.http;

/**
 * Represents a parsed <l>Max-Forwards</l> HTTP request header.
 * <p>
 * Limit the number of times the message can be forwarded through proxies or gateways.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Max-Forwards: 10
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The Max-Forwards request-header field provides a mechanism with the TRACE (section 9.8) and OPTIONS (section 9.2)
 * methods to limit the number of proxies or gateways that can forward the request to the next inbound server.
 * This can be useful when the client is attempting to trace a request chain which appears to be failing or looping in
 * mid-chain.
 * <p class='bcode'>
 * 	Max-Forwards   = "Max-Forwards" ":" 1*DIGIT
 * </p>
 * <p>
 * The Max-Forwards value is a decimal integer indicating the remaining number of times this request message may be
 * forwarded.
 * <p>
 * Each proxy or gateway recipient of a TRACE or OPTIONS request containing a Max-Forwards header field MUST check and
 * update its value prior to forwarding the request.
 * If the received value is zero (0), the recipient MUST NOT forward the request; instead, it MUST respond as the final
 * recipient.
 * If the received Max-Forwards value is greater than zero, then the forwarded message MUST contain an updated
 * Max-Forwards field with a value decremented by one (1).
 * <p>
 * The Max-Forwards header field MAY be ignored for all other methods defined by this specification and for any
 * extension methods for which it is not explicitly referred to as part of that method definition.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class MaxForwards extends HeaderInteger {

	/**
	 * Returns a parsed <code>Max-Forwards</code> header.
	 *
	 * @param value The <code>Max-Forwards</code> header string.
	 * @return The parsed <code>Max-Forwards</code> header, or <jk>null</jk> if the string was null.
	 */
	public static MaxForwards forString(String value) {
		if (value == null)
			return null;
		return new MaxForwards(value);
	}

	private MaxForwards(String value) {
		super(value);
	}
}
