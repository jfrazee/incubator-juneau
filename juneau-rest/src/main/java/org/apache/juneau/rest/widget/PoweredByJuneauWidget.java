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
package org.apache.juneau.rest.widget;

import org.apache.juneau.rest.*;

/**
 * Widget that places a powered-by-Juneau message on the page.
 * <p>
 * The variable it resolves is <js>"$W{poweredByJuneau}"</js>.
 */
public class PoweredByJuneauWidget extends Widget {

	@Override /* Widget */
	public String getName() {
		return "poweredByJuneau";
	}

	@Override /* Widget */
	public String resolve(RestRequest req) throws Exception {
		return "<a href='http://apache.org'><img style='float:right;padding-right:20px;height:32px' src='"+req.getUriResolver().resolve("servlet:/htdocs/juneau.png")+"'>";
	}
}


