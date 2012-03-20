package com.alibaba.crm.hudsonplugin;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class DubboServiceVersionChangerBuildWrapper extends BuildWrapper implements Serializable{

	private final String version;

	@DataBoundConstructor
	public DubboServiceVersionChangerBuildWrapper(String version) {
		this.version = version.trim();
	}

	public String getVersion() {
		return version;
	}

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		setDubboServiceVersion(build, listener);
		return new Environment() {};
	}

	private void setDubboServiceVersion(AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {
		FilePath targetDir = build.getWorkspace().child("src//java.test//spring//esb//esb-client-provider.xml");
		listener.getLogger().println(targetDir);
		
		// replace the expect version into the esb-client-provider.xml file
		targetDir.act(new FileCallable<Void>() {
//			private static final long serialVersionUID = -4425754058302003580L;

			public Void invoke(File file, VirtualChannel channel) {
				listener.getLogger().println("[LOG]Begin Replace Dubbo Service Version:" + file.toString());
				replaceTxtByStr(file, version);
				listener.getLogger().println("[LOG]End Replace Dubbo Service Version:" + file.toString());
				
				return null;
			}

			private void replaceTxtByStr(File file, String replaceStr) {
				String temp = "";
				String keyword = null;
				try {
					FileInputStream fis = new FileInputStream(file);// TODO 精简点
					InputStreamReader isr = new InputStreamReader(fis);
					BufferedReader br = new BufferedReader(isr);
					StringBuffer buf = new StringBuffer();

					for (int j = 1; (temp = br.readLine()) != null; j++) {
						if (keyword == null) {
							keyword = getKeywordByRegex(temp);
						}

						if (keyword != null && temp.contains(keyword)) {
							listener.getLogger().println("[LOG]Before Replace: " + temp);
							temp = temp.replace(keyword, replaceStr);
							listener.getLogger().println("[LOG]After Replace: " + temp);
						}
						buf = buf.append(temp);
						buf = buf.append(System.getProperty("line.separator"));// TODO linux换行可能不兼容
					}

					br.close();
					FileOutputStream fos = new FileOutputStream(file);
					PrintWriter pw = new PrintWriter(fos);
					pw.write(buf.toString().toCharArray());
					pw.flush();
					pw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			private String getKeywordByRegex(String line) {
//				String regex = "version=\"(.+?)\">";
				String regex = "kernal(.+?)\">";
				return getKeywordByRegex(line, regex);
			}

			private String getKeywordByRegex(String line, String regex) {// TODO 工具类
				String val = null;

				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(line);
				if (m.find()) {
					val = m.group();
				}
				if (val == null) {
					return null;
				} else {
//					int beginIndex = val.indexOf("\"");
//					int endIndex = val.lastIndexOf("\"");
//					String keyword = val.substring(beginIndex + 1, endIndex);
					int endIndex = val.lastIndexOf("\"");
					String keyword = val.substring(0, endIndex);
					return keyword;
				}
			}
		});
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Set dubbo service version";
		}

		public static FormValidation doCheckVersion(@QueryParameter String value) {
			if (value.trim().length() == 0)
				return FormValidation.error("Please set the dubbo service version");
			return FormValidation.ok();
		}

	}
}
