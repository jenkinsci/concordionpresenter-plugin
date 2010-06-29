package com.rjohnst.hudson.plugins.concordionpresenter;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractBuild;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Action;

public class ConcordionBuildAction implements Action {

	private final AbstractBuild<?,?> build;

	public ConcordionBuildAction(final AbstractBuild<?,?> build) {
		this.build = build;
	}

	public String getDisplayName() {
		return "Concordion Report";
	}

	public String getIconFileName() {
		return "clipboard.gif";
	}

	public String getUrlName() {
		return "concordion";
	}

	public Object getTarget() {
		if (build != null) {
			return ConcordionPresenter.getConcordionReportDirectory(build);
		}
		return null;
	}

	public DirectoryBrowserSupport doDynamic(final StaplerRequest req, final StaplerResponse rsp) {
		if(this.build != null) {
			return new DirectoryBrowserSupport(this, 
					ConcordionPresenter.getConcordionReportDirectory(this.build),
					"concordion", "clipboard.gif", false);
		}
		return null;
	}
}
