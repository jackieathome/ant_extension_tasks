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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.selectors.modifiedselector.Algorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.DigestAlgorithm;
import org.apache.tools.ant.util.FileUtils;

abstract class ResourceURLRewriteTask extends Task {

	protected static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
	private static final Algorithm MD5DIGESTER = new DigestAlgorithm();
	private File file = null;

	private Union resources = null;

	private boolean preserveLastModified = false;

	private String encoding = null;

	protected String webRootUrl = null;

	protected String targetRootUrl = null;

	public ResourceURLRewriteTask() {
		super();
		this.file = null;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public void setWebRootUrl(String webRootUrl) {
		this.webRootUrl = webRootUrl;
	}

	public void setTargetRootUrl(String targetRootUrl) {
		this.targetRootUrl = targetRootUrl;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void addFileset(FileSet set) {
		addConfigured(set);
	}

	public void addConfigured(ResourceCollection rc) {
		if (!rc.isFilesystemOnly()) {
			throw new BuildException("only filesystem resources are supported");
		}
		if (resources == null) {
			resources = new Union();
		}
		resources.add(rc);
	}

	public void setPreserveLastModified(boolean b) {
		preserveLastModified = b;
	}

	protected abstract String doReplace(String input);

	protected String getMD5Value(File file) {
		return MD5DIGESTER.getValue(file);
	}
	
	protected void doReplace() throws IOException {
		File temp = FILE_UTILS.createTempFile("replace", ".txt", null, true,
				true);
		try {
			boolean changes = false;

			InputStream is = new FileInputStream(file);
			try {
				Reader r = encoding != null ? new InputStreamReader(is,
						encoding) : new InputStreamReader(is);
				OutputStream os = new FileOutputStream(temp);
				try {
					Writer w = encoding != null ? new OutputStreamWriter(os,
							encoding) : new OutputStreamWriter(os);

					changes = multilineReplace(r, w);

					r.close();
					w.close();

				} finally {
					FileUtils.close(os);
				}
			} finally {
				FileUtils.close(is);
			}
			if (changes) {
				log("File has changed; saving the updated file",
						Project.MSG_VERBOSE);
				try {
					long origLastModified = file.lastModified();
					FILE_UTILS.rename(temp, file);
					if (preserveLastModified) {
						FILE_UTILS.setFileLastModified(file, origLastModified);
					}
					temp = null;
				} catch (IOException e) {
					throw new BuildException("Couldn't rename temporary file "
							+ temp, e, getLocation());
				}
			} else {
				log("No change made", Project.MSG_DEBUG);
			}
		} finally {
			FileUtils.delete(temp);
		}
	}

	public void execute() throws BuildException {

		if (file != null && resources != null) {
			throw new BuildException("You cannot supply the 'file' attribute "
					+ "and resource collections at the same " + "time.");
		}

		if (file != null && file.exists()) {
			try {
				doReplace();
			} catch (IOException e) {
				log("An error occurred processing file: '"
						+ file.getAbsolutePath() + "': " + e.toString(),
						Project.MSG_ERR);
			}
		} else if (file != null) {
			log("The following file is missing: '" + file.getAbsolutePath()
					+ "'", Project.MSG_ERR);
		}

		if (resources != null) {
			for (Resource r : resources) {
				FileProvider fp = r.as(FileProvider.class);
				file = fp.getFile();

				if (file.exists()) {
					try {
						doReplace();
					} catch (Exception e) {
						log("An error occurred processing file: '"
								+ file.getAbsolutePath() + "': " + e.toString(),
								Project.MSG_ERR);
					}
				} else {
					log("The following file is missing: '"
							+ file.getAbsolutePath() + "'", Project.MSG_ERR);
				}
			}
		}
	}

	private boolean multilineReplace(Reader r, Writer w) throws IOException {
		return replaceAndWrite(FileUtils.safeReadFully(r), w);
	}

	private boolean replaceAndWrite(String s, Writer w) throws IOException {
		String res = doReplace(s);
		w.write(res);
		return !res.equals(s);
	}
}
