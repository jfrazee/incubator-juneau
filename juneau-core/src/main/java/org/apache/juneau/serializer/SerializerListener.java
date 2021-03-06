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
package org.apache.juneau.serializer;

import java.text.*;

import org.apache.juneau.*;

/**
 * Class for listening for serialize events during a serialization.
 */
public class SerializerListener {

	/**
	 * Called when an exception is thrown when trying to call a bean getter method.
	 *
	 * @param session The serializer session.
	 * 	Note that if
	 * @param t The throwable that was thrown by the getter method.
	 * @param p The bean property we had an issue on.
	 */
	public void onBeanGetterException(SerializerSession session, Throwable t, BeanPropertyMeta p) {
		onError(session, t, MessageFormat.format("Could not call getValue() on property ''{1}'' of class ''{2}'', exception = {3}", p.getName(), p.getBeanMeta().getClassMeta(), t.getLocalizedMessage()));
	}

	/**
	 * Called when an error occurs during serialization but is ignored.
	 *
	 * @param session The serializer session.
	 * @param t The throwable that was thrown by the getter method.
	 * @param msg The error message.
	 */
	public void onError(SerializerSession session, Throwable t, String msg) {
		// Do something with this information.
	}
}
