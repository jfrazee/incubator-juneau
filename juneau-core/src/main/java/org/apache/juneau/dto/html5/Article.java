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
package org.apache.juneau.dto.html5;

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-article-element">&lt;article&gt;</a> element.
 * <p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects (org.apache.juneau.dto)</a>
 * 	<ul>
 * 		<li class='sublink'><a class='doclink' href='../../../../../overview-summary.html#DTOs.HTML5'>HTML5</a>
 * 	</ul>
 * </ul>
 */
@Bean(typeName="article")
public class Article extends HtmlElementMixed {

	/**
	 * Adds a header node to this element.
	 *
	 * @param children The children inside the header node.
	 * @return This object (for method chaining).
	 */
	public Article header(Object...children) {
		super.child(HtmlBuilder.header(children));
		return this;
	}

	/**
	 * Adds a footer node to this element.
	 *
	 * @param children The children inside the footer node.
	 * @return This object (for method chaining).
	 */
	public Article footer(Object...children) {
		super.child(HtmlBuilder.footer(children));
		return this;
	}

	/**
	 * Adds a link node to this element.
	 *
	 * @param link The link node to add to this article.
	 * @return This object (for method chaining).
	 */
	public Article link(Link link) {
		super.child(link);
		return this;
	}

	/**
	 * Adds a section node to this element.
	 *
	 * @param section The section node to add to this article.
	 * @return This object (for method chaining).
	 */
	public Article section(Section section) {
		super.child(section);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Article _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Article id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Article style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Article children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Article child(Object child) {
		super.child(child);
		return this;
	}
}
