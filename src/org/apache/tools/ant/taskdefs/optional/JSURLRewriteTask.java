/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.Project;

/**
 * @author jackie
 *
 */
public class JSURLRewriteTask extends ResourceURLRewriteTask {

	private int start = 0;
	
	/**
	 * {@inheritDoc}
	 */		
	protected String doReplace(String input) {
		String res = input;

		String pattern = "<script.+src=\"(?:<%=.+%>)?(.+\\.js)\".+>";
		log("Replacing pattern '" + pattern + "'.",
				Project.MSG_VERBOSE);

		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(input);

		start = 0;
		while (m.find(start)) {

			String matched = m.group(1);
			res = doRewriteResource(m, matched);
			m.reset(res);
		}

		return res;
	}
	
	protected String doRewriteResource(Matcher m, String matched) {
		String resourceNameExtension = ".js";
		start = m.end(1);
		File resourceFile = new File(webRootUrl, matched);
		StringBuffer sb = new StringBuffer();

		if (!resourceFile.exists()) {
			log("Resource file '" + resourceFile.getAbsolutePath() + "' is missing.",
					Project.MSG_ERR);
			m.appendTail(sb);
			return sb.toString();
		}

		String url = matched;
		int index = -1;

		if ((index = matched.indexOf(resourceNameExtension)) != -1) {
			url = matched.substring(0, index);
		}
		
		if (url.startsWith("/")) {
			url = url.substring(1);
		}
			
		String md5Value = getMD5Value(resourceFile);
		String filename = String.format("%s_%s.js", url, md5Value);
		
		String targetUrl = String.format("<script type=\"text/javascript\" src=\"<%%=request.getContextPath()%%>/%s\"></script>", filename);
		m.appendReplacement(sb, targetUrl);
		m.appendTail(sb);

		File targetFile = new File(targetRootUrl, filename);
		if (!targetFile.exists()) {
			try {
				FILE_UTILS.copyFile(resourceFile, targetFile);
			} catch (IOException e) {
				log("Fail to copy file '" + resourceFile.getAbsolutePath()
						+ "' to '" + targetFile.getAbsolutePath() + "'", e,
						Project.MSG_ERR);
			}
		}
			
		return sb.toString();
	}
}
